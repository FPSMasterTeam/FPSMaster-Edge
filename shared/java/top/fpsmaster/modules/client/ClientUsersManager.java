package top.fpsmaster.modules.client;

import net.minecraft.entity.Entity;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.websocket.data.message.server.SFetchPlayerPacket;

import java.util.ArrayList;

public class ClientUsersManager {
    public ArrayList<ClientUser> users = new ArrayList<>();

    public void addFromFetch(SFetchPlayerPacket packet) {
        ClientUser clientUser = new ClientUser(packet.uid, packet.name, packet.uuid, packet.gameId, packet.cosmetics, packet.skin, packet.rank, packet.customRank);
        ClientUser rm = null;
        for (ClientUser user : users) {
            if (user.uid.equals(clientUser.uid))
                rm = user;
        }
        if (rm != null)
            users.remove(rm);
        users.add(clientUser);
        Utility.sendClientDebug("Add user: " + clientUser.name + " " + clientUser.uid + " " + clientUser.uuid + " " + clientUser.gameId + " " + clientUser.rank + " " + clientUser.customRank + " " + clientUser.cosmetics + " " + clientUser.skin);
    }

    public ClientUser getClientUser(Entity entityIn) {
        for (ClientUser user : users)
            if (user.gameId.equals(entityIn.getName()) && user.uuid.equals(entityIn.getUniqueID().toString()))
                return user;
        return null;
    }

    public ClientUser getClientUser(String name, String uuid) {
        for (ClientUser user : users)
            if (user.gameId.equals(name) && user.uuid.equals(uuid))
                return user;
        return null;
    }
}
