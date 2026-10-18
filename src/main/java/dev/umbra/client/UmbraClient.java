package dev.umbra.client;

import dev.umbra.Umbra;
import dev.umbra.client.model.Animations;
import dev.umbra.client.model.GeneratedModels;
import dev.umbra.client.model.ModLayers;
import dev.umbra.client.model.UmbraRenderer;
import dev.umbra.registry.ModEntities;
import dev.umbra.boss.Doppelganger;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Umbra.MODID, value = Dist.CLIENT)
public final class UmbraClient {
    private UmbraClient() {}

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHADOW.get(), ctx -> new MirrorRenderer<>(ctx, "shadow"));
        event.registerEntityRenderer(ModEntities.HALLUCINATION.get(), ctx -> new SilhouetteRenderer<>(ctx, "hallucination"));
        event.registerEntityRenderer(ModEntities.BLIND.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.BLIND, "blind", 0.5F, Animations::blind, null));
        event.registerEntityRenderer(ModEntities.MIMICKER.get(), ctx -> new UmbraRenderer<dev.umbra.mob.Mimicker>(ctx, ModLayers.MIMICKER, "mimicker", 0.5F, Animations::mimicker,
                (e, st) -> st.flag = e.isHidden()));
        event.registerEntityRenderer(ModEntities.WEEPER.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.WEEPER, "weeper", 0.4F, Animations::weeper, null));
        event.registerEntityRenderer(ModEntities.BELLRINGER.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.BELLRINGER, "bellringer", 0.5F, Animations::bellringer, null));
        event.registerEntityRenderer(ModEntities.SHADE.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.SHADE, "shade", 0.3F, Animations::shade, null));
        event.registerEntityRenderer(ModEntities.PHANTOM_ZOMBIE.get(), ctx -> new SilhouetteRenderer<>(ctx, net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png")));
        event.registerEntityRenderer(ModEntities.PHANTOM_SKELETON.get(), ctx -> new SilhouetteRenderer<>(ctx, net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png")));
        event.registerEntityRenderer(ModEntities.PHANTOM_HUSK.get(), ctx -> new SilhouetteRenderer<>(ctx, net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/zombie/husk.png")));
        event.registerEntityRenderer(ModEntities.PHANTOM_STEVE.get(), ctx -> new SilhouetteRenderer<>(ctx, net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png")));
        event.registerEntityRenderer(ModEntities.PHANTOM_SHADOW.get(), ctx -> new SilhouetteRenderer<>(ctx, "phantom_shadow"));
        event.registerEntityRenderer(ModEntities.ASH_FIGURE.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.ASH_FIGURE, "ash_figure", 0.4F, Animations::ashFigure, null));
        event.registerEntityRenderer(ModEntities.DEATH_DOUBLE.get(), ctx -> new SilhouetteRenderer<>(ctx, "death_double"));
        event.registerEntityRenderer(ModEntities.CITADEL_STATUE.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.STATUE, "citadel_statue", 0.5F, Animations::statue,
                (e, st) -> st.variant = Math.floorMod((int) Math.floor(e.getX() * 3) * 31 + (int) Math.floor(e.getZ() * 3) * 17, 3)));
        event.registerEntityRenderer(ModEntities.CITADEL_KEEPER.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.KEEPER, "citadel_keeper", 0.6F, Animations::keeper, null));
        event.registerEntityRenderer(ModEntities.SHADOW_VILLAGER.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.SHADOW_VILLAGER, "shadow_villager", 0.5F, Animations::shadowVillager, null));
        event.registerEntityRenderer(ModEntities.LAMP_KEEPER.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.LAMP_KEEPER, "lamp_keeper", 0.5F, Animations::lampKeeper, null));
        event.registerEntityRenderer(ModEntities.LURKER.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.LURKER, "lurker", 0.0F, Animations::lurker, null));
        event.registerEntityRenderer(ModEntities.CRAWLER.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.CRAWLER, "crawler", 0.8F, Animations::crawler, null));
        event.registerEntityRenderer(ModEntities.WATCHER.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.WATCHER, "watcher", 0.6F, Animations::watcher, null));
        event.registerEntityRenderer(ModEntities.MIMIC_QUEEN.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.MIMIC_QUEEN, "mimic_queen", 0.8F, Animations::mimicQueen, null));
        event.registerEntityRenderer(ModEntities.SHEPHERD.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.SHEPHERD, "shepherd", 0.6F, Animations::shepherd, null));
        event.registerEntityRenderer(ModEntities.DOPPELGANGER.get(), ctx -> new MirrorRenderer<Doppelganger>(ctx, "doppelganger").withArmor(ctx));
        event.registerEntityRenderer(ModEntities.ARCHITECT.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.ARCHITECT, "architect", 0.7F, Animations::architect, null));
        event.registerEntityRenderer(ModEntities.PRIMORDIAL_SHADOW.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.PRIMORDIAL_SHADOW, "primordial_shadow", 1.0F, Animations::primordialShadow, null));
        event.registerEntityRenderer(ModEntities.LIGHT_EATER.get(), ctx -> new UmbraRenderer<>(ctx, ModLayers.LIGHT_EATER, "light_eater", 0.3F,
                Animations::lightEater, (e, st) -> st.flag = e.isCharging()));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModLayers.CRAWLER, GeneratedModels::crawler);
        event.registerLayerDefinition(ModLayers.BLIND, GeneratedModels::blind);
        event.registerLayerDefinition(ModLayers.WEEPER, GeneratedModels::weeper);
        event.registerLayerDefinition(ModLayers.BELLRINGER, GeneratedModels::bellringer);
        event.registerLayerDefinition(ModLayers.LIGHT_EATER, GeneratedModels::lightEater);
        event.registerLayerDefinition(ModLayers.SHADE, GeneratedModels::shade);
        event.registerLayerDefinition(ModLayers.STATUE, GeneratedModels::citadelStatue);
        event.registerLayerDefinition(ModLayers.KEEPER, GeneratedModels::citadelKeeper);
        event.registerLayerDefinition(ModLayers.WATCHER, GeneratedModels::watcher);
        event.registerLayerDefinition(ModLayers.MIMIC_QUEEN, GeneratedModels::mimicQueen);
        event.registerLayerDefinition(ModLayers.SHEPHERD, GeneratedModels::shepherd);
        event.registerLayerDefinition(ModLayers.ARCHITECT, GeneratedModels::architect);
        event.registerLayerDefinition(ModLayers.PRIMORDIAL_SHADOW, GeneratedModels::primordialShadow);
        event.registerLayerDefinition(ModLayers.SHADOW_VILLAGER, GeneratedModels::shadowVillager);
        event.registerLayerDefinition(ModLayers.LURKER, GeneratedModels::lurker);
        event.registerLayerDefinition(ModLayers.MIMICKER, GeneratedModels::mimicker);
        event.registerLayerDefinition(ModLayers.LAMP_KEEPER, GeneratedModels::lampKeeper);
        event.registerLayerDefinition(ModLayers.ASH_FIGURE, GeneratedModels::ashFigure);
    }

    /** Game-bus client events. */
    @EventBusSubscriber(modid = Umbra.MODID, value = Dist.CLIENT)
    public static final class Game {
        private Game() {}

        /** 2.4: music of its own in the Underside and in a boss fight. */
        @SubscribeEvent
        static void music(net.neoforged.neoforge.client.event.SelectMusicEvent event) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            boolean boss = !mc.level.getEntitiesOfClass(dev.umbra.boss.UmbraBoss.class, mc.player.getBoundingBox().inflate(40)).isEmpty();
            if (boss) {
                event.setMusic(new net.minecraft.sounds.Music(dev.umbra.registry.ModSounds.MUSIC_BOSS, 0, 0, true));
            } else if (dev.umbra.underside.Underside.is(mc.level)) {
                event.setMusic(new net.minecraft.sounds.Music(dev.umbra.registry.ModSounds.MUSIC_UNDERSIDE, 1200, 3600, false));
            }
        }

        /** 2.4: on a silent night every creature falls quiet. */
        @SubscribeEvent
        static void hush(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null || event.getSound() == null) return;
            if (!dev.umbra.night.NightEvents.SILENCE.equals(player.getData(dev.umbra.registry.ModAttachments.NIGHT_EVENT))) return;
            var src = event.getSound().getSource();
            if (src == net.minecraft.sounds.SoundSource.HOSTILE || src == net.minecraft.sounds.SoundSource.NEUTRAL || src == net.minecraft.sounds.SoundSource.AMBIENT && !event.getSound().getIdentifier().getNamespace().equals(dev.umbra.Umbra.MODID)) {
                event.setSound(null);
            }
        }

        /** Ash storm: the world closes in to a few blocks of grey. Fog nights: a white wall. */
        @SubscribeEvent
        static void stormFog(net.neoforged.neoforge.client.event.ViewportEvent.RenderFog event) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null || event.getType() != net.minecraft.world.level.material.FogType.NONE) return;
            if (dev.umbra.night.NightEvents.FOG.equals(player.getData(dev.umbra.registry.ModAttachments.NIGHT_EVENT))) {
                event.setNearPlaneDistance(0.0F);
                event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), 14.0F));
                return;
            }
            float s = dev.umbra.underside.AshStorm.at(player);
            if (s <= 0) return;
            event.setNearPlaneDistance(net.minecraft.util.Mth.lerp(s, event.getNearPlaneDistance(), 0.0F));
            event.setFarPlaneDistance(net.minecraft.util.Mth.lerp(s, event.getFarPlaneDistance(), 11.0F));
        }

        @SubscribeEvent
        static void stormColor(net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor event) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return;
            String night = player.getData(dev.umbra.registry.ModAttachments.NIGHT_EVENT);
            if (dev.umbra.night.NightEvents.BLOOD.equals(night)) {
                event.setRed(Math.min(1.0F, event.getRed() * 0.6F + 0.35F));
                event.setGreen(event.getGreen() * 0.35F);
                event.setBlue(event.getBlue() * 0.35F);
                return;
            }
            if (dev.umbra.night.NightEvents.FOG.equals(night)) {
                event.setRed(0.55F);
                event.setGreen(0.56F);
                event.setBlue(0.6F);
                return;
            }
            float s = dev.umbra.underside.AshStorm.at(player);
            if (s <= 0) return;
            event.setRed(net.minecraft.util.Mth.lerp(s, event.getRed(), 0.33F));
            event.setGreen(net.minecraft.util.Mth.lerp(s, event.getGreen(), 0.31F));
            event.setBlue(net.minecraft.util.Mth.lerp(s, event.getBlue(), 0.30F));
        }

        /** Ash blowing past in a storm. */
        @SubscribeEvent
        static void stormParticles(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var player = mc.player;
            if (player == null || mc.level == null || mc.isPaused()) return;
            float s = dev.umbra.underside.AshStorm.at(player);
            if (s <= 0) return;
            var r = player.getRandom();
            double wind = (mc.level.getGameTime() / 6000L) % 2 == 0 ? 0.35 : -0.35;
            for (int i = 0; i < (int) (40 * s); i++) {
                double x = player.getX() + r.nextGaussian() * 8, y = player.getY() + r.nextDouble() * 5 - 1, z = player.getZ() + r.nextGaussian() * 8;
                mc.level.addParticle(i % 3 == 0 ? net.minecraft.core.particles.ParticleTypes.ASH : net.minecraft.core.particles.ParticleTypes.WHITE_ASH,
                        x, y, z, wind, -0.02, wind * 0.4);
            }
        }

        @SubscribeEvent
        static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
            // a quiet line in the item's own voice, where it has one
            Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
            if (id.getNamespace().equals(Umbra.MODID)) {
                String key = "tip.umbra." + id.getPath();
                if (net.minecraft.locale.Language.getInstance().has(key))
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable(key)
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
                int[] piece = dev.umbra.gear.Gear.piece(event.getItemStack());
                if (piece != null) {
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable("set.umbra." + dev.umbra.gear.Gear.SETS[piece[0]])
                            .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
                    if (piece[1] == 3)
                        event.getToolTip().add(net.minecraft.network.chat.Component.translatable("set.umbra." + dev.umbra.gear.Gear.SETS[piece[0]] + ".master")
                                .withStyle(net.minecraft.ChatFormatting.AQUA));
                }
            }
            if (!(event.getItemStack().getItem() instanceof dev.umbra.madness.DespairBladeItem) || event.getEntity() == null) return;
            float bonus = dev.umbra.madness.DespairBladeItem.bonus(event.getEntity());
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable("item.umbra.despair_blade.bonus", String.format("%.1f", bonus))
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
    }

    @SubscribeEvent
    static void menuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(dev.umbra.registry.ModMenus.GLOOM_ANVIL.get(), GloomAnvilScreen::new);
    }

    @SubscribeEvent
    static void clientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        ArmorClient.register(event);
    }

    @SubscribeEvent
    static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        dev.umbra.journal.Journal.openScreen = () -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(new JournalScreen());
    }

    @SubscribeEvent
    static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, Identifier.fromNamespaceAndPath(Umbra.MODID, "sanity"), SanityHud::render);
    }
}
