package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Resolves a worn armour texture without building its path first.
 *
 * <p>Forge's version of {@code getArmorResource} formats the path with {@code String.format} —
 * four arguments, one of them a boxed int, plus a nested {@code String.format} for the overlay
 * suffix — and only then looks the result up in a map keyed by that string. The map spares the
 * {@code ResourceLocation} allocation and nothing else: the formatting, the boxing and the hash of
 * a forty-character string are paid on every call. There are about forty-two of those per frame on
 * a recorded Hypixel match, one per armour slot per visible player, every frame.
 *
 * <p>The path depends only on the armour material, whether the slot takes leggings, and whether
 * this is the overlay pass, so it is cached on exactly those three. Nothing observable changes:
 * the same {@code ResourceLocation} instance comes back that vanilla would have produced.
 *
 * <p>Only exact {@code ItemArmor} instances take the fast path. Forge lets a mod override
 * {@code Item.getArmorTexture} to return a different path per stack, and that hook cannot be
 * cached on the material alone; vanilla armour is exactly {@code ItemArmor} and inherits the
 * default implementation, which returns null and leaves the formatted path untouched. Anything
 * else falls through to the original method.
 */
@Mixin(LayerArmorBase.class)
public class LayerArmorBaseMixin_TextureCache {

    /**
     * Indexed by {@code material.ordinal() * 4 + leggings * 2 + overlay}. An array rather than a
     * map because the whole point is to not hash anything; armour materials are an enum, so the
     * ordinal is already a dense index.
     */
    @Unique
    private static ResourceLocation[] fpsmaster$armorTextures;

    // The full descriptor is required: vanilla has a private getArmorResource(ItemArmor, boolean)
    // as well, and the bare name resolves to that one and fails to apply.
    //
    // remap = false because this overload is Forge's, not vanilla's, so there is no obfuscation
    // mapping for the name and the annotation processor refuses to build without it. Forge's SRG
    // naming renames methods and fields but leaves class names alone, so the descriptor written
    // here matches at runtime in both the development and the shipped environment.
    @Inject(method = "getArmorResource(Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;"
            + "ILjava/lang/String;)Lnet/minecraft/util/ResourceLocation;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void fpsmasterCachedArmorResource(Entity entity, ItemStack stack, int slot, String type,
                                              CallbackInfoReturnable<ResourceLocation> cir) {
        if (!Performance.using || !Performance.cacheArmorTextures.getValue()) {
            return;
        }
        if (stack == null || stack.getItem() == null || stack.getItem().getClass() != ItemArmor.class) {
            return;
        }
        // The two call sites pass null and "overlay"; anything else is a mod path this cache has
        // no index for.
        boolean overlay;
        if (type == null) {
            overlay = false;
        } else if (type.equals("overlay")) {
            overlay = true;
        } else {
            return;
        }

        ItemArmor.ArmorMaterial material = ((ItemArmor) stack.getItem()).getArmorMaterial();
        if (material == null) {
            return;
        }
        ResourceLocation[] cache = fpsmaster$armorTextures;
        if (cache == null) {
            cache = new ResourceLocation[ItemArmor.ArmorMaterial.values().length * 4];
            fpsmaster$armorTextures = cache;
        }
        int index = material.ordinal() * 4 + (slot == 2 ? 2 : 0) + (overlay ? 1 : 0);
        if (index < 0 || index >= cache.length) {
            return;
        }
        ResourceLocation resource = cache[index];
        if (resource == null) {
            // First use of this combination still pays vanilla's cost, once. Building the path the
            // same way vanilla does keeps the result identical, including the domain split a
            // modded material name can carry.
            String name = material.getName();
            String domain = "minecraft";
            int colon = name.indexOf(':');
            if (colon != -1) {
                domain = name.substring(0, colon);
                name = name.substring(colon + 1);
            }
            resource = new ResourceLocation(domain + ":textures/models/armor/" + name
                    + "_layer_" + (slot == 2 ? 2 : 1) + (overlay ? "_overlay" : "") + ".png");
            cache[index] = resource;
        }
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.armorTextureCacheHits++;
        }
        cir.setReturnValue(resource);
    }
}
