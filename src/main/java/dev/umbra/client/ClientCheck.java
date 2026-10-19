package dev.umbra.client;

import dev.umbra.Umbra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CI only (-Dumbra.clientcheck=true): the real client starts, loads every model and texture, reaches the title screen;
 * the client-side pieces that a server test cannot touch are baked once (armour models); then the game quits and
 * leaves run-client/umbra-clientcheck.txt behind. Model and texture problems show up in the log next to it.
 */
@EventBusSubscriber(modid = Umbra.MODID, value = Dist.CLIENT)
public final class ClientCheck {
    private static final boolean ON = Boolean.getBoolean("umbra.clientcheck");
    private static int countdown = -1;
    private static final List<String> lines = new ArrayList<>();

    private ClientCheck() {}

    @SubscribeEvent
    static void onTitle(ScreenEvent.Opening event) {
        if (!ON || countdown >= 0 || !(event.getNewScreen() instanceof TitleScreen)) return;
        countdown = 200;
        check(() -> {
            for (String set : ArmorModels.SETS)
                for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                        net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                    new net.minecraft.client.model.HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState>(ArmorModels.create(set, slot).bakeRoot());
                }
        }, "the 3D armour models bake");
        check(() -> {
            var feet = ArmorModels.create("gloom", net.minecraft.world.entity.EquipmentSlot.FEET).bakeRoot();
            if (feet.getChild("head").hasChild("horn_r") || feet.getChild("right_arm").hasChild("pauldron_r") || feet.getChild("body").hasChild("gorget"))
                throw new IllegalStateException("gloom boots still carry the helmet's horns or the chestplate's pauldrons");
            if (!feet.getChild("right_leg").hasChild("toe_r")) throw new IllegalStateException("gloom boots lost their toe caps");
            var head = ArmorModels.create("shadow", net.minecraft.world.entity.EquipmentSlot.HEAD).bakeRoot();
            if (head.getChild("body").hasChild("cloak")) throw new IllegalStateException("the shadow hood carries the cloak");
        }, "each armour slot draws only its own piece");
        check(() -> {
            for (var f : new java.util.function.Supplier<?>[]{
                    dev.umbra.client.model.GeneratedModels::mimicker, dev.umbra.client.model.GeneratedModels::lampKeeper,
                    dev.umbra.client.model.GeneratedModels::ashFigure, dev.umbra.client.model.GeneratedModels::primordialShadow,
                    dev.umbra.client.model.GeneratedModels::fallenKeeper, dev.umbra.client.model.GeneratedModels::drownedKeeper,
                    dev.umbra.client.model.GeneratedModels::acolyte}) {
                ((net.minecraft.client.model.geom.builders.LayerDefinition) f.get()).bakeRoot();
            }
        }, "the new creature models bake");
        lines.add("ok   the title screen was reached");
    }

    private static void check(Runnable r, String what) {
        try {
            r.run();
            lines.add("ok   " + what);
        } catch (Throwable t) {
            lines.add("FAIL " + what + ": " + t);
            Umbra.LOGGER.error("UMBRA CLIENTCHECK: {}", what, t);
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!ON || countdown < 0) return;
        if (--countdown != 0) return;
        try {
            Files.write(Path.of("umbra-clientcheck.txt"), lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Umbra.LOGGER.error("UMBRA CLIENTCHECK: cannot write the report", e);
        }
        Umbra.LOGGER.info("UMBRA CLIENTCHECK: done");
        Minecraft.getInstance().stop();
    }
}
