# visual

- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- LambDynamicLights - Dynamic Lights (`lambdynamiclights`) 4.13.0+26.3 — lambdynamiclights-4.13.0+26.3.jar
- **sodium-dynamic-lights**: NO build for 26.3 NeoForge (latest NeoForge builds for 1.21.1, 1.21.4, 1.21.5)
- Entity Culling (`entityculling`) 1.11.3 — entityculling-neoforge-1.11.3-mc26.3.jar
- 3D Skin Layers (`3dskinlayers`) 1.11.4 — skinlayers3d-neoforge-1.11.4-mc26.3.jar
- [EMF] Entity Model Features (`entity-model-features`) 3.3.12-neoforge-26.3 — entity_model_features-3.3.12-26.3-neoforge.jar
- [ETF] Entity Texture Features (`entitytexturefeatures`) 7.2.6-neoforge-26.3 — entity_texture_features-7.2.6-26.3-neoforge.jar
- Subtle Effects (`subtle-effects`) 1.15.1-hotfix.1 — SubtleEffects-neoforge-26.3-1.15.1-hotfix.1.jar
- **visuality**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- **particle-rain**: NO build for 26.3 NeoForge (latest NeoForge builds for 1.21.6, 1.21.7, 1.21.8)
- **falling-leaves**: NO build for 26.3 NeoForge (latest NeoForge builds for 1.21, 1.21.1, 1.21.11)
- Not Enough Animations (`not-enough-animations`) 1.12.6 — notenoughanimations-neoforge-1.12.6-mc26.3.jar
- Better Clouds (`better-clouds`) 1.15.0-beta.6 — better-clouds-1.15.0-beta.6+26.3-neoforge.jar
- **eating-animation**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- **P7dR8mSH**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- [ETF] Entity Texture Features (`entitytexturefeatures`) 7.2.6-neoforge-26.3 — entity_texture_features-7.2.6-26.3-neoforge.jar (needed by entity-model-features)
- Fzzy Config (`fzzy-config`) 0.7.7+fix3+26.3+neoforge — fzzy_config-0.7.7+fix3+26.3+neoforge.jar (needed by subtle-effects)
- YetAnotherConfigLib (YACL) (`yacl`) 3.9.7+26.3-neoforge — yet_another_config_lib_v3-3.9.7+26.3-neoforge.jar (needed by better-clouds)
- KotlinLangForge (`kotlin-lang-forge`) 2.16.0-k2.4.21-3.1+neoforge — KotlinLangForge-2.16.0-k2.4.21-3.1+neoforge.jar (needed by fzzy-config)

## Result
- title screen: NOT reached
- world tour: done (0 pictures)

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 11:25:03.895] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 11:25:03.964] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[23Oct2026 11:25:04.162] [ForkJoinPool.commonPool-worker-1/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
42:		Umbra 2.12.0 (umbra)
85: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
98: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
107:[23Oct2026 11:25:06.117] [main/WARN] [mixin/]: Error loading class: net/irisshaders/iris/layer/OuterWrappedRenderType (java.lang.ClassNotFoundException: net.irisshaders.iris.layer.OuterWrappedRenderType)
111:[23Oct2026 11:25:14.137] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
112:[23Oct2026 11:25:14.138] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
113:[23Oct2026 11:25:14.138] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
126:[23Oct2026 11:25:15.223] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Config file '/home/runner/work/umbra-mod/umbra-mod/run-client/config/yacl.json5' does not exist. Creating it with default values.
127:[23Oct2026 11:25:15.223] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Serializing class dev.isxander.yacl3.platform.YACLConfig to '/home/runner/work/umbra-mod/umbra-mod/run-client/config/yacl.json5'
129:[23Oct2026 11:25:15.271] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Config file '/home/runner/work/umbra-mod/umbra-mod/run-client/config/betterclouds-v1.json' does not exist. Creating it with default values.
130:[23Oct2026 11:25:15.271] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Serializing class com.qendolin.betterclouds.config.Config to '/home/runner/work/umbra-mod/umbra-mod/run-client/config/betterclouds-v1.json'
150:[23Oct2026 11:25:17.970] [Render thread/ERROR] [net.minecraft.client.Minecraft/]: Failed to create backend OpenGL
208:[23Oct2026 11:25:19.484] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/entity_model_features, mod/entityculling, mod/entity_texture_features, mod/yumi_mc_core, mod/lambdynlights, mod/lambdynlights_api, mod/yet_another_config_lib_v3, mod/transition, mod/subtle_effects, mod/trender, mod/skinlayers3d, mod/betterclouds, mod/fzzy_config, mod/pride, mod/notenoughanimations, mod/umbra, mod/neoforge, mod/spruceui
249:[23Oct2026 11:44:34.812] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
258:[23Oct2026 11:44:34.882] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
264:[23Oct2026 11:44:35.066] [ForkJoinPool.commonPool-worker-1/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
288:		Umbra 2.12.0 (umbra)
331: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
344: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
353:[23Oct2026 11:44:37.147] [main/WARN] [mixin/]: Error loading class: net/irisshaders/iris/layer/OuterWrappedRenderType (java.lang.ClassNotFoundException: net.irisshaders.iris.layer.OuterWrappedRenderType)
357:[23Oct2026 11:44:45.450] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
358:[23Oct2026 11:44:45.451] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
359:[23Oct2026 11:44:45.451] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
372:[23Oct2026 11:44:46.469] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Deserializing YACLConfig from '/home/runner/work/umbra-mod/umbra-mod/run-client/config/yacl.json5'
374:[23Oct2026 11:44:46.493] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Deserializing Config from '/home/runner/work/umbra-mod/umbra-mod/run-client/config/betterclouds-v1.json'
392:[23Oct2026 11:44:49.012] [Render thread/ERROR] [net.minecraft.client.Minecraft/]: Failed to create backend OpenGL
450:[23Oct2026 11:44:50.464] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/entity_model_features, mod/entityculling, mod/entity_texture_features, mod/yumi_mc_core, mod/lambdynlights, mod/lambdynlights_api, mod/yet_another_config_lib_v3, mod/transition, mod/subtle_effects, mod/trender, mod/skinlayers3d, mod/betterclouds, mod/fzzy_config, mod/pride, mod/notenoughanimations, mod/umbra, mod/neoforge, mod/spruceui

## Shots report
never got into the world (screen net.neoforged.neoforge.client.gui.LoadingErrorScreen)

## Gradle tail
[11:47:25] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:47:35] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:47:45] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:47:55] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:48:05] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:48:15] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:48:25] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:48:35] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:48:45] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:48:55] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTSHOTS: waiting for the world, screen net.neoforged.neoforge.client.gui.LoadingErrorScreen
[11:48:55] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 785/11222 (7.00%) and 1151 parsed for mixin
[11:48:55] [CullThread/INFO] [minecraft/LoggedPrintStream]: [STDOUT]: Shutting down culling task!
[11:48:55] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[11:48:56] [Render thread/INFO] [Entity Texture Features/]: [ETF]: emissive suffixes loaded: [_e]
[11:48:56] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 7c269f21
[11:48:56] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_5-1792755873529.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 4m 23s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
client exited with 0
[11:25:24] [Render thread/DEBUG] [mixin/]: entity_model_features.mixins.json:MixinParrotEntityModel from mod entity_model_features->@Inject::emf$parrot2(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[11:25:24] [Render thread/DEBUG] [mixin/]: entity_model_features.mixins.json:MixinParrotEntityModel from mod entity_model_features->@Inject::emf$parrot2(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[11:25:24] [Render thread/DEBUG] [mixin/]: entity_texture_features.mixins.json:entity.renderer.feature.MixinShoulderParrotFeatureRenderer from mod entity_texture_features->@Inject::etf$modifySubmit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/animal/parrot/Parrot$Variant;FFZLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V doesn't use it's CallbackInfo
[11:25:24] [Render thread/DEBUG] [mixin/]: entity_texture_features.mixins.json:entity.renderer.feature.MixinShoulderParrotFeatureRenderer from mod entity_texture_features->@Inject::etf$modifySubmit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/animal/parrot/Parrot$Variant;FFZLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[11:25:24] [Render thread/DEBUG] [mixin/]: entity_texture_features.mixins.json:entity.renderer.feature.MixinShoulderParrotFeatureRenderer from mod entity_texture_features->@Inject::etf$modifySubmit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/animal/parrot/Parrot$Variant;FFZLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[11:25:24] [Render thread/DEBUG] [mixin/]: entity_texture_features.mixins.json:entity.renderer.feature.MixinShoulderParrotFeatureRenderer from mod entity_texture_features->@Inject::etf$resetEntity(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V doesn't use it's CallbackInfo
[11:25:24] [Render thread/DEBUG] [mixin/]: entity_texture_features.mixins.json:entity.renderer.feature.MixinShoulderParrotFeatureRenderer from mod entity_texture_features->@Inject::etf$resetEntity(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[11:25:24] [Render thread/DEBUG] [mixin/]: entity_texture_features.mixins.json:entity.renderer.feature.MixinShoulderParrotFeatureRenderer from mod entity_texture_features->@Inject::etf$resetEntity(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[11:25:24] [Render thread/INFO] [Subtle Effects/]: Finished loading dynamic sprite sets
[11:25:24] [Render thread/INFO] [BetterClouds/]: betterclouds:noise_presets not loaded yet, waiting for next resource load
[11:25:24] [Render thread/INFO] [BetterClouds/]: All preset resources loaded, initializing preset config
[11:25:24] [Render thread/INFO] [Entity Texture Features/]: [ETF]: reloading ETF data.
[11:25:24] [Render thread/INFO] [Entity Texture Features/]: [ETF]: emissive suffixes loaded: [_e]
[11:25:24] [Render thread/INFO] [Entity Texture Features/]: [ETF]: emissive suffixes loaded: [_e]
[11:25:24] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 782/11133 (7.02%) and 1148 parsed for mixin
[11:25:24] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mods loaded with 8 warning(s)
[11:25:24] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod entity_model_features uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[11:25:24] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod entity_texture_features uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[11:25:24] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod lambdynlights uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[11:25:24] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod lambdynlights_api uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[11:25:24] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod yet_another_config_lib_v3 uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[11:25:24] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod fzzy_config uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[11:25:24] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod pride uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[11:25:24] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod spruceui uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[11:25:24] [Render thread/DEBUG] [mixin/]: Mixing AbstractSelectionListAccessor from yacl.mixins.json into net.minecraft.client.gui.components.AbstractSelectionList
[11:25:24] [Render thread/DEBUG] [mixin/]: yacl.mixins.json:AbstractSelectionListAccessor from mod yet_another_config_lib_v3: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_17 supports class version 61)
[11:25:24] [Render thread/DEBUG] [mixin/]: Mixing AbstractSelectionListMixin from yacl.mixins.json into net.minecraft.client.gui.components.AbstractSelectionList
[11:25:24] [Render thread/DEBUG] [mixin/]: yacl.mixins.json:AbstractSelectionListMixin from mod yet_another_config_lib_v3: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_17 supports class version 61)
[11:25:24] [Render thread/INFO] [BetterClouds/]: Initial pipeline parameters: PipelineParams[celestialBodyHalo=true, nearCloudFade=false, iris=false, distantHorizons=false, voxy=false, reverseZ=false, zNeg1To1=false, cloudsOpaque=false]
[11:25:33] [sodium-workaround-cleanup/WARN] [Sodium-Workarounds/]: Timed out waiting for NeoForge's early renderer; restoring graphics driver workaround environment changes now
