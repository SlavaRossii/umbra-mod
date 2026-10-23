# pack

- Sound Physics Remastered (`sound-physics-remastered`) neoforge-1.5.2+26.3 — sound-physics-remastered-neoforge-1.5.2+26.3.jar
- AmbientSounds (`ambientsounds`) 6.3.11 — AmbientSounds_NEOFORGE_v6.3.11_mc26.3.jar
- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- Iris Shaders (`iris`) 1.11.7+26.3-neoforge — iris-neoforge-1.11.7+mc26.3.jar
- LambDynamicLights - Dynamic Lights (`lambdynamiclights`) 4.13.0+26.3 — lambdynamiclights-4.13.0+26.3.jar
- CreativeCore (`creativecore`) 2.14.22 — CreativeCore_NEOFORGE_v2.14.22_mc26.3.jar (needed by ambientsounds)
- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar (needed by iris)
- **P7dR8mSH**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- shader pack spooklementary v2.0.4 — Spooklementary_v2.0.4.zip

## Result
- title screen: reached
- world tour: NOT finished (0 pictures)

## Graphics
[23Oct2026 13:40:32.986] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 13:40:32.988] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
		Iris 1.11.7+mc26.3 (iris)
[23Oct2026 13:40:44.387] [modloading-worker-0/WARN] [Iris/]: Found fine program match for neoforge:pipeline/entity_unlit_translucent: ENTITIES_TRANSLUCENT
[23Oct2026 13:40:45.818] [ForkJoinPool.commonPool-worker-2/WARN] [Iris/]: [Iris Update Check] This version doesn't have an update index, skipping.
[23Oct2026 13:40:46.312] [Render thread/INFO] [Iris/]: Debug functionality is disabled.
[23Oct2026 13:40:46.313] [Render thread/INFO] [Iris/]: OpenGL 4.5 detected, enabling DSA.
[23Oct2026 13:40:46.580] [Render thread/INFO] [Iris/]: Profile: HIGH (+0 options changed by user)
[23Oct2026 13:40:47.598] [Render thread/INFO] [Iris/]: Using shaderpack: Spooklementary_v2.0.4.zip
[23Oct2026 13:40:47.599] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4
[23Oct2026 13:40:48.227] [Render thread/INFO] [Iris/]: Hardware information:
[23Oct2026 13:40:48.399] [Render thread/INFO] [Iris/]: CPU: 4x AMD EPYC 7763 64-Core Processor
[23Oct2026 13:40:48.399] [Render thread/INFO] [Iris/]: GPU: llvmpipe (LLVM 20.1.2, 256 bits) (Supports OpenGL 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4)
[23Oct2026 13:40:48.399] [Render thread/INFO] [Iris/]: OS: Linux (6.17.0-1022-azure)
[23Oct2026 13:40:54.654] [Render thread/INFO] [Iris/]: Creating pipeline for dimension minecraft:overworld
[23Oct2026 13:40:56.633] [Render thread/WARN] [Iris/]: Shader compilation log for textured: 0:1021(14): warning: `atmColorMult' used uninitialized
[23Oct2026 13:40:57.310] [Render thread/WARN] [Iris/]: Shader compilation log for terrain_solid: 0:556(15): warning: `lmCoord' used uninitialized
[23Oct2026 13:40:58.062] [Render thread/WARN] [Iris/]: Shader compilation log for terrain_translucent: 0:553(15): warning: `lmCoord' used uninitialized
[23Oct2026 13:40:58.086] [Render thread/WARN] [Iris/]: Shader compilation log for terrain_translucent: 0:967(18): warning: `lightColorMult' used uninitialized
[23Oct2026 13:41:02.789] [Render thread/WARN] [Iris/]: Shader compilation log for particles: 0:1023(14): warning: `atmColorMult' used uninitialized

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 13:40:32.924] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 13:40:32.991] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[23Oct2026 13:40:33.166] [ForkJoinPool.commonPool-worker-1/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
35:		Umbra 2.12.0 (umbra)
52: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
60: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
70:[23Oct2026 13:40:35.120] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
71:[23Oct2026 13:40:35.121] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
72:[23Oct2026 13:40:35.122] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
73:[23Oct2026 13:40:35.124] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
77:[23Oct2026 13:40:43.010] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
78:[23Oct2026 13:40:43.011] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
79:[23Oct2026 13:40:43.011] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
151:[23Oct2026 13:40:49.473] [modloading-worker-0/ERROR] [creativecore/]: java.util.NoSuchElementException: No value present
152:[23Oct2026 13:40:49.473] [modloading-worker-0/ERROR] [creativecore/]: Could not load default style
153:[23Oct2026 13:40:49.600] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/lambdynlights, mod/iris, mod/yumi_mc_core, mod/ambientsounds, mod/pride, mod/sound_physics_remastered, mod/lambdynlights_api, mod/umbra, mod/creativecore, mod/neoforge, mod/spruceui
191:[23Oct2026 13:40:54.633] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
386:[23Oct2026 13:41:51.088] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
395:[23Oct2026 13:41:51.162] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
401:[23Oct2026 13:41:51.336] [ForkJoinPool.commonPool-worker-3/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
418:		Umbra 2.12.0 (umbra)
435: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
443: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
453:[23Oct2026 13:41:53.225] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
454:[23Oct2026 13:41:53.226] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
455:[23Oct2026 13:41:53.227] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
456:[23Oct2026 13:41:53.229] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
460:[23Oct2026 13:42:00.285] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
461:[23Oct2026 13:42:00.285] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
462:[23Oct2026 13:42:00.285] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
532:[23Oct2026 13:42:06.319] [modloading-worker-0/ERROR] [creativecore/]: java.util.NoSuchElementException: No value present
533:[23Oct2026 13:42:06.319] [modloading-worker-0/ERROR] [creativecore/]: Could not load default style
534:[23Oct2026 13:42:06.456] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/lambdynlights, mod/iris, mod/yumi_mc_core, mod/ambientsounds, mod/pride, mod/sound_physics_remastered, mod/lambdynlights_api, mod/umbra, mod/creativecore, mod/neoforge, mod/spruceui
572:[23Oct2026 13:42:11.050] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
582:[23Oct2026 13:42:13.542] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
735:[23Oct2026 13:42:25.951] [Render thread/ERROR] [net.minecraft.client.Minecraft/FATAL]: Reported exception thrown!
736:net.minecraft.ReportedException: Render Frame
744:Caused by: java.lang.IllegalStateException: Missing uniform InSampler (should be COMBINED_IMAGE_SAMPLER)
760:[23Oct2026 13:42:26.918] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 69244a85-8605-4784-89a7-eb67fcccccc2
761:[23Oct2026 13:42:27.009] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 09bc08fa-e1cc-4be3-b11c-6654bd3be6d7
768:[23Oct2026 13:42:27.782] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

## Shots report

## Gradle tail
	at org.gradle.internal.execution.steps.LoadPreviousExecutionStateStep.execute(LoadPreviousExecutionStateStep.java:23)
	at org.gradle.internal.execution.steps.HandleStaleOutputsStep.execute(HandleStaleOutputsStep.java:75)
	at org.gradle.internal.execution.steps.HandleStaleOutputsStep.execute(HandleStaleOutputsStep.java:41)
	at org.gradle.internal.execution.steps.AssignMutableWorkspaceStep.lambda$execute$0(AssignMutableWorkspaceStep.java:35)
	at org.gradle.api.internal.tasks.execution.TaskExecution$4.withWorkspace(TaskExecution.java:297)
	at org.gradle.internal.execution.steps.AssignMutableWorkspaceStep.execute(AssignMutableWorkspaceStep.java:31)
	at org.gradle.internal.execution.steps.AssignMutableWorkspaceStep.execute(AssignMutableWorkspaceStep.java:22)
	at org.gradle.internal.execution.steps.ChoosePipelineStep.execute(ChoosePipelineStep.java:40)
	at org.gradle.internal.execution.steps.ChoosePipelineStep.execute(ChoosePipelineStep.java:23)
	at org.gradle.internal.execution.steps.ExecuteWorkBuildOperationFiringStep.lambda$execute$2(ExecuteWorkBuildOperationFiringStep.java:67)
	at org.gradle.internal.execution.steps.ExecuteWorkBuildOperationFiringStep.execute(ExecuteWorkBuildOperationFiringStep.java:67)
	at org.gradle.internal.execution.steps.ExecuteWorkBuildOperationFiringStep.execute(ExecuteWorkBuildOperationFiringStep.java:39)
	at org.gradle.internal.execution.steps.IdentityCacheStep.execute(IdentityCacheStep.java:46)
	at org.gradle.internal.execution.steps.IdentityCacheStep.execute(IdentityCacheStep.java:34)
	at org.gradle.internal.execution.steps.IdentifyStep.execute(IdentifyStep.java:44)
	at org.gradle.internal.execution.steps.IdentifyStep.execute(IdentifyStep.java:31)
	at org.gradle.internal.execution.impl.DefaultExecutionEngine$1.execute(DefaultExecutionEngine.java:64)
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.executeIfValid(ExecuteActionsTaskExecuter.java:132)
	... 61 more


Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 39s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
[13:41:08] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::setSelfPosition(Lnet/minecraft/world/phys/Vec3;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[13:41:08] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::setSelfPosition(Lnet/minecraft/world/phys/Vec3;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[13:41:08] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::play(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V doesn't use it's CallbackInfo
[13:41:08] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::play(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[13:41:08] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::play(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[13:41:08] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::linearAttenuation2(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V doesn't use it's CallbackInfo
[13:41:08] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::linearAttenuation2(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[13:41:08] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::linearAttenuation2(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[13:41:08] [Download-1/DEBUG] [mixin/]: Mixing OggAudioStreamMixin from ambientsounds.mixins.json into net.minecraft.client.sounds.JOrbisAudioStream
[13:41:08] [Download-1/DEBUG] [mixin/]: ambientsounds.mixins.json:OggAudioStreamMixin from mod ambientsounds: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_21 supports class version 65)
[13:41:08] [Download-1/DEBUG] [mixin/]: Renaming synthetic method lambda$setPositionRandomly$0(F)V to md18df4c$ambientsounds$lambda$setPositionRandomly$0$0 in ambientsounds.mixins.json:OggAudioStreamMixin from mod ambientsounds
[13:41:15] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[13:41:15] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 681/10519 (6.47%) and 700 parsed for mixin
[13:41:15] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[13:41:15] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 2ee0105
[13:41:15] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792762813467.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 8s
7 actionable tasks: 7 executed
Configuration cache entry stored.
client exited with 0
