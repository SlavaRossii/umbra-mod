package dev.umbra.client;

import dev.umbra.Umbra;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CI only (-Dumbra.clientshots=true, the client opened straight into a flat world): puts on armour and takes up
 * shields in a fixed set of poses, and for each asks the workflow for a screenshot of the screen (a .req file in
 * shots/; the workflow answers with the .png and a .done file). So what only eyes can judge (where a shield sits in
 * the hand, which pieces of armour a slot draws) is looked at on every build.
 */
@EventBusSubscriber(modid = Umbra.MODID, value = Dist.CLIENT)
public final class ClientShots {
    private static final boolean ON = Boolean.getBoolean("umbra.clientshots");
    private static final Path DIR = Path.of("shots");

    private record Pose(String name, CameraType camera, boolean use, boolean hideGui, String... commands) {}

    private static final String CLEAR = "clear @a";
    private static final List<Pose> POSES = new ArrayList<>();

    static {
        String fp = "tp @a ~ ~ ~ 0 0";
        POSES.add(new Pose("01_aegis_main", CameraType.FIRST_PERSON, false, false, CLEAR, fp, "item replace entity @a weapon.mainhand with umbra:luminite_shield"));
        POSES.add(new Pose("02_aegis_main_block", CameraType.FIRST_PERSON, true, false));
        POSES.add(new Pose("03_aegis_off", CameraType.FIRST_PERSON, false, false, CLEAR, "item replace entity @a weapon.offhand with umbra:luminite_shield"));
        POSES.add(new Pose("04_aegis_off_block", CameraType.FIRST_PERSON, true, false));
        POSES.add(new Pose("05_vanilla_main", CameraType.FIRST_PERSON, false, false, CLEAR, "item replace entity @a weapon.mainhand with minecraft:shield"));
        POSES.add(new Pose("06_vanilla_main_block", CameraType.FIRST_PERSON, true, false));
        POSES.add(new Pose("07_vanilla_off", CameraType.FIRST_PERSON, false, false, CLEAR, "item replace entity @a weapon.offhand with minecraft:shield"));
        POSES.add(new Pose("08_vanilla_off_block", CameraType.FIRST_PERSON, true, false));
        POSES.add(new Pose("09_aegis_third", CameraType.THIRD_PERSON_FRONT, false, true, CLEAR, "item replace entity @a weapon.offhand with umbra:luminite_shield"));
        POSES.add(new Pose("10_aegis_third_block", CameraType.THIRD_PERSON_FRONT, true, true));
        POSES.add(new Pose("11_marta_lantern", CameraType.FIRST_PERSON, false, false, CLEAR, "item replace entity @a weapon.mainhand with umbra:marta_lantern"));
        int n = 12;
        for (String set : new String[]{"gloom", "shadow", "luminite"}) {
            for (String piece : new String[]{"boots", "leggings", "chestplate", "helmet"}) {
                String slot = switch (piece) { case "boots" -> "feet"; case "leggings" -> "legs"; case "chestplate" -> "chest"; default -> "head"; };
                POSES.add(new Pose(String.format("%02d_%s_%s_only", n++, set, piece), CameraType.THIRD_PERSON_FRONT, false, true, CLEAR,
                        "item replace entity @a armor." + slot + " with umbra:" + set + "_" + piece));
            }
            POSES.add(new Pose(String.format("%02d_%s_full", n++, set), CameraType.THIRD_PERSON_FRONT, false, true, CLEAR,
                    "item replace entity @a armor.feet with umbra:" + set + "_boots_3", "item replace entity @a armor.legs with umbra:" + set + "_leggings_3",
                    "item replace entity @a armor.chest with umbra:" + set + "_chestplate_3", "item replace entity @a armor.head with umbra:" + set + "_helmet_3"));
        }
        // 2.7.1: the new creatures, three quarters on, and the weapons in the hand
        // (moved far off, not killed: a killed Fallen Keeper would roll the credits over the next shots)
        String away = "tp @e[tag=shot] ~ ~ ~2000";
        String noSlimes = "kill @e[type=minecraft:slime]";
        for (String[] mob : new String[][]{{"fallen_keeper", "4.5"}, {"drowned_keeper", "6"}, {"acolyte", "3"}}) {
            for (String yaw : new String[]{"150", "210"}) {
                POSES.add(new Pose(String.format("%02d_mob_%s_%s", n++, mob[0], yaw), CameraType.FIRST_PERSON, false, true, CLEAR, away, fp,
                        "execute as @p at @p run summon umbra:" + mob[0] + " ^ ^ ^" + mob[1] + " {NoAI:1b,PersistenceRequired:1b,Tags:[\"shot\"],Rotation:[" + yaw + "f,0f]}"));
            }
        }
        // 2.7.3: the Primordial Shadow, by day and by night (her glow)
        for (String[] v : new String[][]{{"day", "160"}, {"day", "200"}, {"night", "180"}}) {
            boolean night = v[0].equals("night");
            POSES.add(new Pose(String.format("%02d_mob_primordial_%s_%s", n++, v[0], v[1]), CameraType.FIRST_PERSON, false, true, CLEAR, away, fp,
                    night ? "time set 18000" : "time set 6000", night ? "effect clear @a minecraft:night_vision" : "say day",
                    "execute as @p at @p run summon umbra:primordial_shadow ^ ^1 ^9 {NoAI:1b,PersistenceRequired:1b,Tags:[\"shot\"],Rotation:[" + v[1] + "f,0f]}"));
        }
        // 2.7.4: the Architect, the same in all his phases; 2.7.5: the Shepherd
        for (String[] v : new String[][]{{"day", "150"}, {"day", "215"}, {"night", "180"}, {"day", "160", "shepherd"}, {"day", "220", "shepherd"},
                {"night", "180", "shepherd"}, {"day", "150", "mimic_queen"}, {"day", "215", "mimic_queen"}, {"night", "180", "mimic_queen"}, {"day", "150", "watcher"}, {"day", "220", "watcher"}, {"night", "180", "watcher"}}) {
            String who = v.length > 2 ? v[2] : "architect";
            boolean night = v[0].equals("night");
            POSES.add(new Pose(String.format("%02d_mob_%s_%s_%s", n++, who, v[0], v[1]), CameraType.FIRST_PERSON, false, true, CLEAR, away, fp,
                    night ? "time set 18000" : "time set 6000", night ? "effect clear @a minecraft:night_vision" : "say day",
                    "execute as @p at @p run summon umbra:" + who + " ^ ^ ^7 {NoAI:1b,PersistenceRequired:1b,Tags:[\"shot\"],Rotation:[" + v[1] + "f,0f]}"));
        }
        POSES.add(new Pose(String.format("%02d_clear", n++), CameraType.FIRST_PERSON, false, false, CLEAR, away, "time set 6000",
                "effect give @a minecraft:night_vision infinite 0 true"));
        for (String w : new String[]{"gloom_sword", "despair_blade", "shadow_dagger", "shadow_knife", "gloom_axe", "gloom_pickaxe", "gloom_shovel",
                "luminite_spear", "shepherd_scythe", "echo_staff", "keeper_staff", "luminite_bow",
                "seal_of_watching", "seal_of_mimicry", "seal_of_herding", "seal_of_reflection",
                "luminite_lantern[umbra:lit=true]", "luminite_lantern", "marta_lantern", "dawn_lantern"}) {
            POSES.add(new Pose(String.format("%02d_hand_%s", n++, w.replaceAll("\\[.*", "_lit")), CameraType.FIRST_PERSON, false, false, CLEAR, noSlimes, "item replace entity @a weapon.mainhand with umbra:" + w));
        }
        POSES.add(new Pose(String.format("%02d_hand_luminite_bow_drawn", n++), CameraType.FIRST_PERSON, true, false));
        // 2.8.1: the lantern in your own hand, in the holds there are to choose from, and looking down (it hangs upright)
        for (int k = 0; k < LanternFirstPerson.HOLDS.length; k++)
            POSES.add(new Pose(String.format("%02d_hold%d_luminite_lantern", n++, k), CameraType.FIRST_PERSON, false, false, CLEAR, noSlimes,
                    "item replace entity @a weapon.mainhand with umbra:luminite_lantern[umbra:lit=true]"));
        POSES.add(new Pose(String.format("%02d_hold0_lantern_down", n++), CameraType.FIRST_PERSON, false, false, CLEAR, noSlimes, "tp @a ~ ~ ~ 0 40",
                "item replace entity @a weapon.mainhand with umbra:marta_lantern"));
        POSES.add(new Pose(String.format("%02d_hold0_lantern_offhand", n++), CameraType.FIRST_PERSON, false, false, CLEAR, noSlimes, "tp @a ~ ~ ~ 0 0",
                "item replace entity @a weapon.offhand with umbra:dawn_lantern", "item replace entity @a weapon.mainhand with umbra:gloom_sword"));
        for (String w : new String[]{"luminite_spear", "shepherd_scythe", "keeper_staff", "gloom_axe", "luminite_bow", "seal_of_watching", "seal_of_herding",
                "luminite_lantern[umbra:lit=true]", "marta_lantern"}) {
            POSES.add(new Pose(String.format("%02d_third_%s", n++, w.replaceAll("\\[.*", "_lit")), CameraType.THIRD_PERSON_FRONT, false, true, CLEAR, "item replace entity @a weapon.mainhand with umbra:" + w));
        }
        POSES.add(new Pose(String.format("%02d_third_luminite_bow_drawn", n++), CameraType.THIRD_PERSON_FRONT, true, true));
        // 2.8.0: the lantern held out on the arm, seen from the side: a mannequin renders like a player, arm poses and all
        for (String[] v : new String[][]{{"luminite_lantern", "{\"umbra:lit\":true}", "90"}, {"luminite_lantern", "{\"umbra:lit\":true}", "140"},
                {"marta_lantern", "{}", "130"}, {"dawn_lantern", "{}", "60"}}) {
            POSES.add(new Pose(String.format("%02d_side_%s_%s", n++, v[0], v[2]), CameraType.FIRST_PERSON, false, true, CLEAR, away, noSlimes,
                    "kill @e[type=minecraft:mannequin]", "tp @a ~ ~ ~ 0 18",
                    "execute as @p at @p run summon minecraft:mannequin ~ ~ ~2.7 {Tags:[\"shot\"],immovable:1b,hide_description:1b,Rotation:[" + v[2]
                            + "f,0f],equipment:{mainhand:{id:\"umbra:" + v[0] + "\",count:1,components:" + v[1] + "}}}"));
        }
        POSES.add(new Pose(String.format("%02d_side_clear", n++), CameraType.FIRST_PERSON, false, false, "kill @e[type=minecraft:mannequin]", "tp @a ~ ~ ~ 0 0"));
        // 2.7.2: every building and every creature and block of the mod, built far off and looked at from a good place
        for (String what : dev.umbra.command.Showcase.ALL)
            POSES.add(new Pose(String.format("%02d_tour_%s", n++, what), CameraType.FIRST_PERSON, false, true, CLEAR, "execute as @p run umbra showcase " + what));
        POSES.add(new Pose(String.format("%02d_tour_end", n++), CameraType.FIRST_PERSON, false, true, "tp @e[tag=shot] ~ ~-300 ~"));
    }

    /** Ticks to let the world settle before the picture: a far trip needs its chunks drawn. */
    private static int settle(Pose p) {
        return p.name.contains("_tour_") ? 220 : 50;
    }

    private static int tick, pose = -1, wait, outside;
    private static final List<String> report = new ArrayList<>();

    private ClientShots() {}

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            if (++outside % 200 == 0) Umbra.LOGGER.info("UMBRA CLIENTSHOTS: waiting for the world, screen {}", mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName());
            if (outside > 20 * 240) {
                write(Path.of("umbra-clientshots.txt"), "never got into the world (screen " + (mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName()) + ")\n");
                mc.stop();
            }
            return;
        }
        outside = 0;
        if (mc.gui.screen() != null) {
            String sc = mc.gui.screen().getClass().getSimpleName();
            if (++tick % 100 == 0) Umbra.LOGGER.info("UMBRA CLIENTSHOTS: a screen is open: {}", sc);
            if (tick % 100 == 0 && !sc.contains("Loading") && !sc.contains("Receiving")) mc.gui.setScreen(null);
            return;
        }
        tick++;
        if (pose == -1) {
            if (tick < 200) return;
            run(mc, "gamemode creative @a", "difficulty easy", "time set 6000", "weather clear", "effect give @a minecraft:night_vision infinite 0 true",
                    "gamerule doDaylightCycle false", "gamerule advance_time false", "gamerule doMobSpawning false", "gamerule spawn_mobs false", "kill @e[type=!player]");
            try {
                Files.createDirectories(DIR);
            } catch (Exception e) {
                Umbra.LOGGER.error("UMBRA CLIENTSHOTS: no shots dir", e);
            }
            next(mc);
            return;
        }
        if (pose >= POSES.size()) return;
        Pose p = POSES.get(pose);
        wait++;
        if (wait == settle(p)) {
            report.add(String.format("%s  using=%s hand=%s main=%s off=%s", p.name, mc.player.isUsingItem(), mc.player.getUsedItemHand(),
                    mc.player.getMainHandItem(), mc.player.getOffhandItem()));
            write(DIR.resolve(p.name + ".req"), "");
        }
        if (wait > settle(p) && (Files.exists(DIR.resolve(p.name + ".done")) || wait > settle(p) + 550)) next(mc);
    }

    private static void next(Minecraft mc) {
        mc.options.keyUse.setDown(false);
        pose++;
        wait = 0;
        if (pose >= POSES.size()) {
            write(Path.of("umbra-clientshots.txt"), String.join("\n", report) + "\n");
            Umbra.LOGGER.info("UMBRA CLIENTSHOTS: done");
            mc.stop();
            return;
        }
        Pose p = POSES.get(pose);
        java.util.regex.Matcher hold = java.util.regex.Pattern.compile("_hold(\\d)_").matcher(p.name);
        LanternFirstPerson.hold = hold.find() ? Integer.parseInt(hold.group(1)) : 0;
        mc.options.setCameraType(p.camera);
        mc.options.fov().set(p.camera == CameraType.FIRST_PERSON ? 70 : 52);   // third person: zoomed in on the armour
        run(mc, p.commands);
        if (p.use) mc.options.keyUse.setDown(true);
        Umbra.LOGGER.info("UMBRA CLIENTSHOTS: pose {}", p.name);
    }

    private static void run(Minecraft mc, String... commands) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            for (String c : commands) {
                try {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                } catch (Throwable t) {
                    Umbra.LOGGER.warn("UMBRA CLIENTSHOTS: command {} failed: {}", c, t.toString());
                }
            }
        });
    }

    private static void write(Path path, String text) {
        try {
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Umbra.LOGGER.error("UMBRA CLIENTSHOTS: cannot write {}", path, e);
        }
    }
}
