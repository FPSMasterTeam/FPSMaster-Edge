package top.fpsmaster.replay;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.world.chunk.Chunk;
import top.fpsmaster.forge.mixin.accessor.ChunkProviderClientAccessor;
import top.fpsmaster.modules.logger.ClientLogger;

import java.util.Collection;
import java.util.List;

/**
 * Reconstructs the world as it already stands, so a recording started mid-session is not empty.
 *
 * <p>A server sends chunks, entity spawns and tab-list entries once. Anything already loaded when
 * recording begins is never repeated, so without a snapshot the replay opens on empty space with the
 * odd entity popping into existence as it happens to move.
 *
 * <p>Tab-list entries are written as raw protocol bytes rather than built as packet objects.
 * {@code S38PacketPlayerListItem} can only be constructed from a server-side {@code EntityPlayerMP},
 * which does not exist on a client — but the recording format stores a packet id and its payload, so
 * the payload can simply be written directly. This matters more than it looks: without the tab-list
 * entry a replayed player has no skin and no name, and skin textures are a large part of what an
 * entity costs to draw, which is the thing these recordings exist to measure.
 */
final class ReplaySnapshot {

    /** Packet id of S38PacketPlayerListItem in the 1.8 play/clientbound table. */
    private static final int PLAYER_LIST_ITEM_ID = 0x38;

    /** ADD_PLAYER, the first entry of the packet's action enum. */
    private static final int ACTION_ADD_PLAYER = 0;

    /** Sidebar, list and below-name; the three slots a scoreboard can be shown in. */
    private static final int DISPLAY_SLOTS = 3;

    /** Object type id for a dropped item in the spawn-object table. */
    private static final int OBJECT_TYPE_ITEM = 2;

    private ReplaySnapshot() {
    }

    interface Sink {
        void accept(net.minecraft.network.Packet<?> packet);

        void acceptRaw(int packetId, byte[] payload);
    }

    static void capture(Minecraft mc, Sink sink) {
        capturePlayerList(mc, sink);
        captureScoreboard(mc, sink);
        captureChunks(mc, sink);
        captureEntities(mc, sink);
    }

    /**
     * Rebuilds the scoreboard, because every later change to it is an update to something that
     * already exists.
     *
     * <p>handleTeams and handleScoreboardObjective look their subject up by name and use it without
     * checking, so an update naming a team or objective created before recording began throws and is
     * lost. On a server that keeps its scoreboard busy that is most of them. It is not cosmetic
     * either: nameplate colours and prefixes live in teams, so the players that do appear render
     * wrongly without this.
     */
    private static void captureScoreboard(Minecraft mc, Sink sink) {
        if (mc.theWorld == null) {
            return;
        }
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        int objectives = 0;
        for (ScoreObjective objective : scoreboard.getScoreObjectives()) {
            sink.accept(new S3BPacketScoreboardObjective(objective, 0));
            objectives++;
        }
        for (Score score : scoreboard.getScores()) {
            sink.accept(new S3CPacketUpdateScore(score));
        }
        for (int slot = 0; slot < DISPLAY_SLOTS; slot++) {
            ScoreObjective displayed = scoreboard.getObjectiveInDisplaySlot(slot);
            if (displayed != null) {
                sink.accept(new S3DPacketDisplayScoreboard(slot, displayed));
            }
        }
        int teams = 0;
        for (ScorePlayerTeam team : scoreboard.getTeams()) {
            sink.accept(new S3EPacketTeams(team, 0));
            teams++;
        }
        ClientLogger.info("replay", "seeded " + objectives + " objective(s) and " + teams + " team(s)");
    }

    private static void capturePlayerList(Minecraft mc, Sink sink) {
        if (mc.getNetHandler() == null) {
            return;
        }
        Collection<NetworkPlayerInfo> infos = mc.getNetHandler().getPlayerInfoMap();
        if (infos.isEmpty()) {
            return;
        }
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            buffer.writeVarIntToBuffer(ACTION_ADD_PLAYER);
            buffer.writeVarIntToBuffer(infos.size());
            for (NetworkPlayerInfo info : infos) {
                GameProfile profile = info.getGameProfile();
                buffer.writeUuid(profile.getId());
                buffer.writeString(profile.getName());

                Collection<Property> properties = profile.getProperties().values();
                buffer.writeVarIntToBuffer(properties.size());
                for (Property property : properties) {
                    buffer.writeString(property.getName());
                    buffer.writeString(property.getValue());
                    boolean signed = property.getSignature() != null;
                    buffer.writeBoolean(signed);
                    if (signed) {
                        buffer.writeString(property.getSignature());
                    }
                }
                buffer.writeVarIntToBuffer(info.getGameType() == null ? 0 : info.getGameType().getID());
                buffer.writeVarIntToBuffer(info.getResponseTime());
                // No display name: the entry carries the profile name, which is what the renderer
                // needs. A scoreboard-driven display name arrives in the stream if it changes.
                buffer.writeBoolean(false);
            }
            byte[] payload = new byte[buffer.readableBytes()];
            buffer.readBytes(payload);
            sink.acceptRaw(PLAYER_LIST_ITEM_ID, payload);
            ClientLogger.info("replay", "seeded " + infos.size() + " tab-list entr(ies)");
        } catch (Exception failure) {
            ClientLogger.error("replay", "could not seed the tab list: " + failure);
        } finally {
            buffer.release();
        }
    }

    private static void captureChunks(Minecraft mc, Sink sink) {
        if (mc.theWorld == null || !(mc.theWorld.getChunkProvider() instanceof ChunkProviderClient)) {
            return;
        }
        List<Chunk> chunks = ((ChunkProviderClientAccessor) mc.theWorld.getChunkProvider()).getChunkListing();
        int captured = 0;
        for (Chunk chunk : chunks) {
            if (chunk != null && chunk.isLoaded()) {
                sink.accept(new S21PacketChunkData(chunk, true, 0xFFFF));
                captured++;
            }
        }
        ClientLogger.info("replay", "seeded " + captured + " chunk(s)");
    }

    private static void captureEntities(Minecraft mc, Sink sink) {
        if (mc.theWorld == null) {
            return;
        }
        int players = 0;
        int mobs = 0;
        int items = 0;
        int skipped = 0;

        for (Object candidate : mc.theWorld.loadedEntityList) {
            Entity entity = (Entity) candidate;
            // The recording client's own player is not in the server's stream either; it is captured
            // separately as a movement track and replayed as an avatar.
            if (entity == mc.thePlayer) {
                continue;
            }
            if (entity instanceof EntityPlayer) {
                sink.accept(new S0CPacketSpawnPlayer((EntityPlayer) entity));
                captureEquipment(entity, sink);
                players++;
            } else if (entity instanceof EntityLivingBase) {
                sink.accept(new S0FPacketSpawnMob((EntityLivingBase) entity));
                captureEquipment(entity, sink);
                mobs++;
            } else if (entity instanceof EntityItem) {
                sink.accept(new S0EPacketSpawnObject(entity, OBJECT_TYPE_ITEM));
                items++;
            } else {
                // Arrows, projectiles, hanging entities and the rest each need their own object type
                // id, and there is no reverse lookup for it on the client. They are a small share of
                // entity render cost, so they are counted and left out rather than guessed at.
                skipped++;
            }
        }
        ClientLogger.info("replay", "seeded " + players + " player(s), " + mobs + " mob(s), "
                + items + " item(s)" + (skipped > 0 ? ", skipped " + skipped + " other entit(ies)" : ""));
    }

    private static void captureEquipment(Entity entity, Sink sink) {
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        for (int slot = 0; slot < 5; slot++) {
            ItemStack stack = living.getEquipmentInSlot(slot);
            if (stack != null) {
                sink.accept(new S04PacketEntityEquipment(entity.getEntityId(), slot, stack));
            }
        }
    }
}
