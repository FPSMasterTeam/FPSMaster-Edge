package top.fpsmaster.utils.world; // 替换为你的mod包名

import net.minecraft.potion.Potion; // 用于 Potion.heal.id 等
import java.util.HashMap;
import java.util.Map;


public class PotionMetadata {

    public static final int SPLASH_POTION_COMMON_OFFSET = 16384; // 0x4000
    public static final int WATER_BOTTLE = 0;

    public static final int HEALING_I = 8197;
    public static final int HEALING_II = 8261;

    public static final int HARMING_I = 8260;
    public static final int HARMING_II = 8204;

    public static final int SPEED_I = 8258;
    public static final int SPEED_II = 8226;

    public static final int SLOWNESS_I = 8202;

    public static final int STRENGTH_I = 8265;
    public static final int STRENGTH_II = 8201;

    public static final int NIGHT_VISION_I = 8262;

    public static final int INVISIBILITY_I = 8230;

    public static final int POISON_I = 8264;
    public static final int POISON_II = 8200;

    public static final int REGENERATION_I = 8257;
    public static final int REGENERATION_II = 8225;

    public static final int FIRE_RESISTANCE_I = 8259;

    public static final int WATER_BREATHING_I = 8231;

    public static final int WEAKNESS_I = 8232;

    public static final int LEAPING_I = 8235;
    public static final int LEAPING_II = 8203;

    public static final int SPLASH_WATER_BOTTLE = 16384;

    public static final int SPLASH_HEALING_I = 16388;
    public static final int SPLASH_HEALING_II = 16421;

    public static final int SPLASH_HARMING_I = 16420;
    public static final int SPLASH_HARMING_II = 16364;

    public static final int SPLASH_SPEED_I = 16418;
    public static final int SPLASH_SPEED_II = 16386;

    public static final int SPLASH_SLOWNESS_I = 16362;

    public static final int SPLASH_STRENGTH_I = 16425;
    public static final int SPLASH_STRENGTH_II = 16361;

    public static final int SPLASH_NIGHT_VISION_I = 16422;

    public static final int SPLASH_INVISIBILITY_I = 16390;

    public static final int SPLASH_POISON_I = 16424;
    public static final int SPLASH_POISON_II = 16360;

    public static final int SPLASH_REGENERATION_I = 16417;
    public static final int SPLASH_REGENERATION_II = 16385;

    public static final int SPLASH_FIRE_RESISTANCE_I = 16419;

    public static final int SPLASH_WATER_BREATHING_I = 16391;

    public static final int SPLASH_WEAKNESS_I = 16392;

    public static final int SPLASH_LEAPING_I = 16395;
    public static final int SPLASH_LEAPING_II = 16363;

    private static final Map<Integer, Map<Boolean, Integer>> basePotionMetadataMap = new HashMap<>();
    private static final Map<Integer, Map<Boolean, Integer>> splashPotionMetadataMap = new HashMap<>();

    static {
        Map<Boolean, Integer> healingMap = new HashMap<>();
        healingMap.put(false, HEALING_I);
        healingMap.put(true, HEALING_II);
        basePotionMetadataMap.put(Potion.heal.id, healingMap);

        Map<Boolean, Integer> harmingMap = new HashMap<>();
        harmingMap.put(false, HARMING_I);
        harmingMap.put(true, HARMING_II);
        basePotionMetadataMap.put(Potion.harm.id, harmingMap);

        Map<Boolean, Integer> speedMap = new HashMap<>();
        speedMap.put(false, SPEED_I);
        speedMap.put(true, SPEED_II);
        basePotionMetadataMap.put(Potion.moveSpeed.id, speedMap);

        Map<Boolean, Integer> slownessMap = new HashMap<>();
        slownessMap.put(false, SLOWNESS_I); // Slowness usually no II
        basePotionMetadataMap.put(Potion.moveSlowdown.id, slownessMap);

        Map<Boolean, Integer> strengthMap = new HashMap<>();
        strengthMap.put(false, STRENGTH_I);
        strengthMap.put(true, STRENGTH_II);
        basePotionMetadataMap.put(Potion.damageBoost.id, strengthMap);

        Map<Boolean, Integer> nightVisionMap = new HashMap<>();
        nightVisionMap.put(false, NIGHT_VISION_I);
        basePotionMetadataMap.put(Potion.nightVision.id, nightVisionMap);

        Map<Boolean, Integer> invisibilityMap = new HashMap<>();
        invisibilityMap.put(false, INVISIBILITY_I);
        basePotionMetadataMap.put(Potion.invisibility.id, invisibilityMap);

        Map<Boolean, Integer> poisonMap = new HashMap<>();
        poisonMap.put(false, POISON_I);
        poisonMap.put(true, POISON_II);
        basePotionMetadataMap.put(Potion.poison.id, poisonMap);

        Map<Boolean, Integer> regenerationMap = new HashMap<>();
        regenerationMap.put(false, REGENERATION_I);
        regenerationMap.put(true, REGENERATION_II);
        basePotionMetadataMap.put(Potion.regeneration.id, regenerationMap);

        Map<Boolean, Integer> fireResistanceMap = new HashMap<>();
        fireResistanceMap.put(false, FIRE_RESISTANCE_I);
        basePotionMetadataMap.put(Potion.fireResistance.id, fireResistanceMap);

        Map<Boolean, Integer> waterBreathingMap = new HashMap<>();
        waterBreathingMap.put(false, WATER_BREATHING_I);
        basePotionMetadataMap.put(Potion.waterBreathing.id, waterBreathingMap);

        Map<Boolean, Integer> weaknessMap = new HashMap<>();
        weaknessMap.put(false, WEAKNESS_I);
        basePotionMetadataMap.put(Potion.weakness.id, weaknessMap);

        Map<Boolean, Integer> leapingMap = new HashMap<>();
        leapingMap.put(false, LEAPING_I);
        leapingMap.put(true, LEAPING_II);
        basePotionMetadataMap.put(Potion.jump.id, leapingMap);

        // 初始化可喷溅药水映射
        Map<Boolean, Integer> splashHealingMap = new HashMap<>();
        splashHealingMap.put(false, SPLASH_HEALING_I);
        splashHealingMap.put(true, SPLASH_HEALING_II);
        splashPotionMetadataMap.put(Potion.heal.id, splashHealingMap);

        Map<Boolean, Integer> splashHarmingMap = new HashMap<>();
        splashHarmingMap.put(false, SPLASH_HARMING_I);
        splashHarmingMap.put(true, SPLASH_HARMING_II);
        splashPotionMetadataMap.put(Potion.harm.id, splashHarmingMap);

        Map<Boolean, Integer> splashSpeedMap = new HashMap<>();
        splashSpeedMap.put(false, SPLASH_SPEED_I);
        splashSpeedMap.put(true, SPLASH_SPEED_II);
        splashPotionMetadataMap.put(Potion.moveSpeed.id, splashSpeedMap);

        Map<Boolean, Integer> splashSlownessMap = new HashMap<>();
        splashSlownessMap.put(false, SPLASH_SLOWNESS_I);
        splashPotionMetadataMap.put(Potion.moveSlowdown.id, splashSlownessMap);

        Map<Boolean, Integer> splashStrengthMap = new HashMap<>();
        splashStrengthMap.put(false, SPLASH_STRENGTH_I);
        splashStrengthMap.put(true, SPLASH_STRENGTH_II);
        splashPotionMetadataMap.put(Potion.damageBoost.id, splashStrengthMap);

        Map<Boolean, Integer> splashNightVisionMap = new HashMap<>();
        splashNightVisionMap.put(false, SPLASH_NIGHT_VISION_I);
        splashPotionMetadataMap.put(Potion.nightVision.id, splashNightVisionMap);

        Map<Boolean, Integer> splashInvisibilityMap = new HashMap<>();
        splashInvisibilityMap.put(false, SPLASH_INVISIBILITY_I);
        splashPotionMetadataMap.put(Potion.invisibility.id, splashInvisibilityMap);

        Map<Boolean, Integer> splashPoisonMap = new HashMap<>();
        splashPoisonMap.put(false, SPLASH_POISON_I);
        splashPoisonMap.put(true, SPLASH_POISON_II);
        splashPotionMetadataMap.put(Potion.poison.id, splashPoisonMap);

        Map<Boolean, Integer> splashRegenerationMap = new HashMap<>();
        splashRegenerationMap.put(false, SPLASH_REGENERATION_I);
        splashRegenerationMap.put(true, SPLASH_REGENERATION_II);
        splashPotionMetadataMap.put(Potion.regeneration.id, splashRegenerationMap);

        Map<Boolean, Integer> splashFireResistanceMap = new HashMap<>();
        splashFireResistanceMap.put(false, SPLASH_FIRE_RESISTANCE_I);
        splashPotionMetadataMap.put(Potion.fireResistance.id, splashFireResistanceMap);

        Map<Boolean, Integer> splashWaterBreathingMap = new HashMap<>();
        splashWaterBreathingMap.put(false, SPLASH_WATER_BREATHING_I);
        splashPotionMetadataMap.put(Potion.waterBreathing.id, splashWaterBreathingMap);

        Map<Boolean, Integer> splashWeaknessMap = new HashMap<>();
        splashWeaknessMap.put(false, SPLASH_WEAKNESS_I);
        splashPotionMetadataMap.put(Potion.weakness.id, splashWeaknessMap);

        Map<Boolean, Integer> splashLeapingMap = new HashMap<>();
        splashLeapingMap.put(false, SPLASH_LEAPING_I);
        splashLeapingMap.put(true, SPLASH_LEAPING_II);
        splashPotionMetadataMap.put(Potion.jump.id, splashLeapingMap);
    }


}


