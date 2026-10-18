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
    }

    private static int tick, pose = -1, wait;
    private static final List<String> report = new ArrayList<>();

    private ClientShots() {}

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            if (++tick % 200 == 0) Umbra.LOGGER.info("UMBRA CLIENTSHOTS: waiting for the world, screen {}", mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName());
            return;
        }
        if (mc.gui.screen() != null) {
            if (++tick % 100 == 0) Umbra.LOGGER.info("UMBRA CLIENTSHOTS: a screen is open: {}", mc.gui.screen().getClass().getName());
            if (tick % 100 == 0) mc.gui.setScreen(null);
            return;
        }
        tick++;
        if (pose == -1) {
            if (tick < 200) return;
            run(mc, "gamemode creative @a", "difficulty peaceful", "time set 6000", "weather clear", "effect give @a minecraft:night_vision infinite 0 true",
                    "gamerule doDaylightCycle false", "gamerule advance_time false", "kill @e[type=!player]");
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
        if (wait == 50) {
            report.add(String.format("%s  using=%s hand=%s main=%s off=%s", p.name, mc.player.isUsingItem(), mc.player.getUsedItemHand(),
                    mc.player.getMainHandItem(), mc.player.getOffhandItem()));
            write(DIR.resolve(p.name + ".req"), "");
        }
        if (wait > 50 && (Files.exists(DIR.resolve(p.name + ".done")) || wait > 600)) next(mc);
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
        mc.options.setCameraType(p.camera);
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
