package com.drakou111.neohouse.music;

import com.drakou111.neohouse.compat.SoundCompat;
import com.drakou111.neohouse.constants.Sound;
import net.minecraft.sounds.SoundEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class SoundEventResolver {

    private SoundEventResolver() {}

    private static final Set<Sound> MELODIC = EnumSet.of(
            Sound.BLOCK_NOTE_BLOCK_HARP,
            Sound.BLOCK_NOTE_BLOCK_BASS,
            Sound.BLOCK_NOTE_BLOCK_BASEDRUM,
            Sound.BLOCK_NOTE_BLOCK_SNARE,
            Sound.BLOCK_NOTE_BLOCK_HAT,
            Sound.BLOCK_NOTE_BLOCK_GUITAR,
            Sound.BLOCK_NOTE_BLOCK_FLUTE,
            Sound.BLOCK_NOTE_BLOCK_BELL,
            Sound.BLOCK_NOTE_BLOCK_CHIME,
            Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,
            Sound.BLOCK_NOTE_BLOCK_PLING,
            Sound.NOTE_PIANO,
            Sound.NOTE_BASS,
            Sound.NOTE_BASS_GUITAR,
            Sound.NOTE_BASS_DRUM,
            Sound.NOTE_SNARE_DRUM,
            Sound.NOTE_STICKS,
            Sound.NOTE_PLING
    );

    public static boolean isMelodic(Sound sound) {
        return MELODIC.contains(sound);
    }

    /**
     * 1.8-era legacy names -> modern sound-event id (path, minecraft namespace).
     * Only names that do NOT normalize-match a modern id need to be here.
     */
    private static final Map<Sound, String> LEGACY_OVERRIDES = buildOverrides();

    private static Map<Sound, String> buildOverrides() {
        Map<Sound, String> m = new HashMap<>();

        // --- note block instruments (legacy note.* names) ---
        m.put(Sound.NOTE_PIANO,      "block.note_block.harp");
        m.put(Sound.NOTE_BASS,       "block.note_block.bass");
        m.put(Sound.NOTE_BASS_GUITAR,"block.note_block.bass");
        m.put(Sound.NOTE_BASS_DRUM,  "block.note_block.basedrum");
        m.put(Sound.NOTE_SNARE_DRUM, "block.note_block.snare");
        m.put(Sound.NOTE_STICKS,     "block.note_block.hat");
        m.put(Sound.NOTE_PLING,      "block.note_block.pling");

        // --- wither / dragon / ghast / enderman (mob.* short names) ---
        m.put(Sound.WITHER_DEATH,    "entity.wither.death");
        m.put(Sound.WITHER_SPAWN,    "entity.wither.spawn");
        m.put(Sound.WITHER_SHOOT,    "entity.wither.shoot");
        m.put(Sound.WITHER_IDLE,     "entity.wither.ambient");
        m.put(Sound.WITHER_HURT,     "entity.wither.hurt");
        m.put(Sound.ENDERDRAGON_DEATH,"entity.ender_dragon.death");
        m.put(Sound.ENDERDRAGON_GROWL,"entity.ender_dragon.growl");
        m.put(Sound.ENDERDRAGON_HIT, "entity.ender_dragon.hurt");
        m.put(Sound.ENDERDRAGON_WINGS,"entity.ender_dragon.flap");
        m.put(Sound.GHAST_SCREAM,    "entity.ghast.hurt");
        m.put(Sound.GHAST_SCREAM2,   "entity.ghast.hurt");
        m.put(Sound.GHAST_DEATH,     "entity.ghast.death");
        m.put(Sound.GHAST_CHARGE,    "entity.ghast.warn");
        m.put(Sound.GHAST_FIREBALL,  "entity.ghast.shoot");
        m.put(Sound.GHAST_MOAN,      "entity.ghast.ambient");
        m.put(Sound.ENDERMAN_SCREAM, "entity.enderman.scream");
        m.put(Sound.ENDERMAN_HIT,    "entity.enderman.hurt");
        m.put(Sound.ENDERMAN_IDLE,   "entity.enderman.ambient");
        m.put(Sound.ENDERMAN_DEATH,  "entity.enderman.death");
        m.put(Sound.ENDERMAN_TELEPORT,"entity.enderman.teleport");
        m.put(Sound.ENDERMAN_STARE,  "entity.enderman.stare");

        // --- zombie / pigman / skeleton / spider / silverfish / blaze / creeper ---
        m.put(Sound.ZOMBIE_DEATH,    "entity.zombie.death");
        m.put(Sound.ZOMBIE_HURT,     "entity.zombie.hurt");
        m.put(Sound.ZOMBIE_IDLE,     "entity.zombie.ambient");
        m.put(Sound.ZOMBIE_WALK,     "entity.zombie.step");
        m.put(Sound.ZOMBIE_METAL,    "entity.zombie.attack_iron_door");
        m.put(Sound.ZOMBIE_WOOD,     "entity.zombie.attack_wooden_door");
        m.put(Sound.ZOMBIE_WOODBREAK,"entity.zombie.break_wooden_door");
        m.put(Sound.ZOMBIE_INFECT,   "entity.zombie.infect");
        m.put(Sound.ZOMBIE_UNFECT,   "entity.zombie_villager.converted");
        m.put(Sound.ZOMBIE_REMEDY,   "entity.zombie_villager.cure");
        m.put(Sound.ZOMBIE_PIG_IDLE, "entity.zombified_piglin.ambient");
        m.put(Sound.ZOMBIE_PIG_ANGRY,"entity.zombified_piglin.angry");
        m.put(Sound.ZOMBIE_PIG_HURT, "entity.zombified_piglin.hurt");
        m.put(Sound.ZOMBIE_PIG_DEATH,"entity.zombified_piglin.death");
        m.put(Sound.SKELETON_IDLE,   "entity.skeleton.ambient");
        m.put(Sound.SKELETON_HURT,   "entity.skeleton.hurt");
        m.put(Sound.SKELETON_DEATH,  "entity.skeleton.death");
        m.put(Sound.SKELETON_WALK,   "entity.skeleton.step");
        m.put(Sound.SPIDER_IDLE,     "entity.spider.ambient");
        m.put(Sound.SPIDER_DEATH,    "entity.spider.death");
        m.put(Sound.SPIDER_WALK,     "entity.spider.step");
        m.put(Sound.SILVERFISH_HIT,  "entity.silverfish.hurt");
        m.put(Sound.SILVERFISH_IDLE, "entity.silverfish.ambient");
        m.put(Sound.SILVERFISH_KILL, "entity.silverfish.death");
        m.put(Sound.SILVERFISH_WALK, "entity.silverfish.step");
        m.put(Sound.BLAZE_BREATH,    "entity.blaze.ambient");
        m.put(Sound.BLAZE_DEATH,     "entity.blaze.death");
        m.put(Sound.BLAZE_HIT,       "entity.blaze.hurt");
        m.put(Sound.CREEPER_DEATH,   "entity.creeper.death");
        m.put(Sound.CREEPER_HISS,    "entity.creeper.primed");

        // --- passive / tameable short names ---
        m.put(Sound.WOLF_HOWL,  "entity.wolf.howl");
        m.put(Sound.WOLF_GROWL, "entity.wolf.growl");
        m.put(Sound.WOLF_BARK,  "entity.wolf.growl");
        m.put(Sound.WOLF_HURT,  "entity.wolf.hurt");
        m.put(Sound.WOLF_DEATH, "entity.wolf.death");
        m.put(Sound.WOLF_PANT,  "entity.wolf.pant");
        m.put(Sound.WOLF_WHINE, "entity.wolf.whine");
        m.put(Sound.WOLF_SHAKE, "entity.wolf.shake");
        m.put(Sound.WOLF_WALK,  "entity.wolf.step");
        m.put(Sound.CAT_MEOW,   "entity.cat.ambient");
        m.put(Sound.CAT_HISS,   "entity.cat.hiss");
        m.put(Sound.CAT_HIT,    "entity.cat.hurt");
        m.put(Sound.CAT_PURR,   "entity.cat.purr");
        m.put(Sound.CAT_PURREOW,"entity.cat.purreow");
        m.put(Sound.CHICKEN_IDLE,"entity.chicken.ambient");
        m.put(Sound.CHICKEN_HURT,"entity.chicken.hurt");
        m.put(Sound.CHICKEN_WALK,"entity.chicken.step");
        m.put(Sound.CHICKEN_EGG_POP,"entity.chicken.egg");
        m.put(Sound.COW_IDLE,   "entity.cow.ambient");
        m.put(Sound.COW_HURT,   "entity.cow.hurt");
        m.put(Sound.COW_WALK,   "entity.cow.step");
        m.put(Sound.PIG_IDLE,   "entity.pig.ambient");
        m.put(Sound.PIG_DEATH,  "entity.pig.death");
        m.put(Sound.PIG_WALK,   "entity.pig.step");
        m.put(Sound.SHEEP_IDLE, "entity.sheep.ambient");
        m.put(Sound.SHEEP_WALK, "entity.sheep.step");
        m.put(Sound.SHEEP_SHEAR,"entity.sheep.shear");
        m.put(Sound.RABBIT_AMBIENT,"entity.rabbit.ambient");
        m.put(Sound.RABBIT_DEATH,"entity.rabbit.death");
        m.put(Sound.RABBIT_HURT,"entity.rabbit.hurt");
        m.put(Sound.RABBIT_JUMP,"entity.rabbit.jump");

        // --- horses (incl. zombie / skeleton variants) ---
        m.put(Sound.HORSE_DEATH,  "entity.horse.death");
        m.put(Sound.HORSE_IDLE,   "entity.horse.ambient");
        m.put(Sound.HORSE_HIT,    "entity.horse.hurt");
        m.put(Sound.HORSE_ANGRY,  "entity.horse.angry");
        m.put(Sound.HORSE_ARMOR,  "entity.horse.armor");
        m.put(Sound.HORSE_BREATHE,"entity.horse.breathe");
        m.put(Sound.HORSE_GALLOP, "entity.horse.gallop");
        m.put(Sound.HORSE_JUMP,   "entity.horse.jump");
        m.put(Sound.HORSE_LAND,   "entity.horse.land");
        m.put(Sound.HORSE_SADDLE, "entity.horse.saddle");
        m.put(Sound.HORSE_SOFT,   "entity.horse.step");
        m.put(Sound.HORSE_WOOD,   "entity.horse.step_wood");
        m.put(Sound.HORSE_ZOMBIE_DEATH,  "entity.zombie_horse.death");
        m.put(Sound.HORSE_ZOMBIE_HIT,    "entity.zombie_horse.hurt");
        m.put(Sound.HORSE_ZOMBIE_IDLE,   "entity.zombie_horse.ambient");
        m.put(Sound.HORSE_SKELETON_DEATH,"entity.skeleton_horse.death");
        m.put(Sound.HORSE_SKELETON_HIT,  "entity.skeleton_horse.hurt");
        m.put(Sound.HORSE_SKELETON_IDLE, "entity.skeleton_horse.ambient");
        m.put(Sound.DONKEY_HIT,   "entity.donkey.hurt");
        m.put(Sound.DONKEY_IDLE,  "entity.donkey.ambient");
        m.put(Sound.DONKEY_DEATH, "entity.donkey.death");
        m.put(Sound.DONKEY_ANGRY, "entity.donkey.angry");

        // --- golems / villagers / bats / guardians ---
        m.put(Sound.IRONGOLEM_WALK, "entity.iron_golem.step");
        m.put(Sound.IRONGOLEM_HIT,  "entity.iron_golem.hurt");
        m.put(Sound.IRONGOLEM_DEATH,"entity.iron_golem.death");
        m.put(Sound.IRONGOLEM_THROW,"entity.iron_golem.attack");
        m.put(Sound.VILLAGER_IDLE,  "entity.villager.ambient");
        m.put(Sound.VILLAGER_DEATH, "entity.villager.death");
        m.put(Sound.VILLAGER_HAGGLE,"entity.villager.trade");
        m.put(Sound.VILLAGER_HIT,   "entity.villager.hurt");
        m.put(Sound.VILLAGER_NO,    "entity.villager.no");
        m.put(Sound.VILLAGER_YES,   "entity.villager.yes");
        m.put(Sound.BAT_IDLE,    "entity.bat.ambient");
        m.put(Sound.BAT_HURT,    "entity.bat.hurt");
        m.put(Sound.BAT_DEATH,   "entity.bat.death");
        m.put(Sound.BAT_LOOP,    "entity.bat.loop");
        m.put(Sound.BAT_TAKEOFF, "entity.bat.takeoff");
        m.put(Sound.GUARDIAN_IDLE,  "entity.guardian.ambient");
        m.put(Sound.GUARDIAN_HIT,   "entity.guardian.hurt");
        m.put(Sound.GUARDIAN_DEATH, "entity.guardian.death");
        m.put(Sound.GUARDIAN_FLOP,  "entity.guardian.flop");
        m.put(Sound.GUARDIAN_ATTACK,"entity.guardian.attack");
        m.put(Sound.GUARDIAN_CURSE, "entity.elder_guardian.curse");
        m.put(Sound.GUARDIAN_LAND_IDLE, "entity.guardian.ambient_land");
        m.put(Sound.GUARDIAN_LAND_HIT,  "entity.guardian.hurt_land");
        m.put(Sound.GUARDIAN_LAND_DEATH,"entity.guardian.death_land");
        m.put(Sound.GUARDIAN_ELDER_IDLE, "entity.elder_guardian.ambient");
        m.put(Sound.GUARDIAN_ELDER_HIT,  "entity.elder_guardian.hurt");
        m.put(Sound.GUARDIAN_ELDER_DEATH,"entity.elder_guardian.death");

        // --- slimes / magma cubes ---
        m.put(Sound.MAGMACUBE_WALK,  "entity.magma_cube.squish");
        m.put(Sound.MAGMACUBE_WALK2, "entity.magma_cube.squish");
        m.put(Sound.MAGMACUBE_JUMP,  "entity.magma_cube.jump");
        m.put(Sound.SLIME_WALK,      "entity.slime.squish");
        m.put(Sound.SLIME_WALK2,     "entity.slime.squish");
        m.put(Sound.SLIME_ATTACK,    "entity.slime.attack");

        // --- projectiles / items / xp / misc effects ---
        m.put(Sound.ARROW_HIT,   "entity.arrow.hit");
        m.put(Sound.SHOOT_ARROW, "entity.arrow.shoot");
        m.put(Sound.ITEM_PICKUP, "entity.item.pickup");
        m.put(Sound.ITEM_BREAK,  "entity.item.break");
        m.put(Sound.ORB_PICKUP,  "entity.experience_orb.pickup");
        m.put(Sound.LEVEL_UP,    "entity.player.levelup");
        m.put(Sound.CLICK,       "ui.button.click");
        m.put(Sound.GLASS,       "block.glass.break");
        m.put(Sound.EXPLODE,     "entity.generic.explode");
        m.put(Sound.FUSE,        "entity.tnt.primed");
        m.put(Sound.FIZZ,        "block.fire.extinguish");
        m.put(Sound.BURP,        "entity.player.burp");
        m.put(Sound.EAT,         "entity.generic.eat");
        m.put(Sound.DRINK,       "entity.generic.drink");
        m.put(Sound.SWIM,        "entity.player.swim");
        m.put(Sound.SPLASH,      "entity.player.splash");
        m.put(Sound.SPLASH2,     "entity.player.splash");
        m.put(Sound.FALL_BIG,    "entity.player.big_fall");
        m.put(Sound.FALL_SMALL,  "entity.player.small_fall");
        m.put(Sound.HURT_FLESH,  "entity.player.hurt");
        m.put(Sound.CHEST_OPEN,  "block.chest.open");
        m.put(Sound.CHEST_CLOSE, "block.chest.close");
        m.put(Sound.DOOR_OPEN,   "block.wooden_door.open");
        m.put(Sound.DOOR_CLOSE,  "block.wooden_door.close");
        m.put(Sound.ANVIL_LAND,  "block.anvil.land");
        m.put(Sound.ANVIL_USE,   "block.anvil.use");
        m.put(Sound.ANVIL_BREAK, "block.anvil.break");

        // --- fireworks (legacy fireworks.* names) ---
        m.put(Sound.FIREWORK_LAUNCH,      "entity.firework_rocket.launch");
        m.put(Sound.FIREWORK_BLAST,       "entity.firework_rocket.blast");
        m.put(Sound.FIREWORK_BLAST2,      "entity.firework_rocket.blast_far");
        m.put(Sound.FIREWORK_LARGE_BLAST, "entity.firework_rocket.large_blast");
        m.put(Sound.FIREWORK_LARGE_BLAST2,"entity.firework_rocket.large_blast_far");
        m.put(Sound.FIREWORK_TWINKLE,     "entity.firework_rocket.twinkle");
        m.put(Sound.FIREWORK_TWINKLE2,    "entity.firework_rocket.twinkle_far");

        // --- ambience / weather (ambient.* / weather.*) ---
        m.put(Sound.AMBIENCE_CAVE,    "ambient.cave");
        m.put(Sound.AMBIENCE_RAIN,    "weather.rain");
        m.put(Sound.AMBIENCE_THUNDER, "entity.lightning_bolt.thunder");
        m.put(Sound.WEATHER_RAIN,     "weather.rain");
        m.put(Sound.WEATHER_RAIN_ABOVE,"weather.rain.above");

        return m;
    }

    private static Map<String, String> normalizedIndex; // normalized path -> path
    private static Set<String> validPaths;
    private static final EnumMap<Sound, String> RESOLVED = new EnumMap<>(Sound.class);
    private static final EnumMap<Sound, SoundEvent> EVENTS = new EnumMap<>(Sound.class);
    private static boolean indexBuilt = false;

    private static synchronized void ensureIndex() {
        if (indexBuilt) return;
        validPaths = SoundCompat.soundEventPaths();
        Map<String, String> index = new HashMap<>();
        for (String path : validPaths) {
            index.putIfAbsent(normalize(path), path);
        }
        normalizedIndex = index;
        indexBuilt = true;
    }

    public static synchronized String resolvePath(Sound sound) {
        if (RESOLVED.containsKey(sound)) return RESOLVED.get(sound);
        ensureIndex();

        String result = null;
        String override = LEGACY_OVERRIDES.get(sound);
        if (override != null && validPaths.contains(override)) {
            result = override;
        }
        if (result == null) {
            result = normalizedIndex.get(normalize(sound.name()));
        }

        RESOLVED.put(sound, result);
        return result;
    }

    public static synchronized SoundEvent soundEvent(Sound sound) {
        if (EVENTS.containsKey(sound)) return EVENTS.get(sound);
        String path = resolvePath(sound);
        SoundEvent ev = path == null ? null : SoundCompat.soundEvent(path);
        EVENTS.put(sound, ev);
        return ev;
    }

    private static String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }
}
