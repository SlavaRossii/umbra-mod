# shaders

- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- Iris Shaders (`iris`) 1.11.7+26.3-neoforge — iris-neoforge-1.11.7+mc26.3.jar
- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar (needed by iris)
- shader pack complementary-reimagined r5.9.3 — ComplementaryReimagined_r5.9.3.zip

## Result
- title screen: reached
- world tour: NOT finished (0 pictures)

## Graphics
[23Oct2026 13:40:46.144] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 13:40:46.146] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
		Iris 1.11.7+mc26.3 (iris)
[23Oct2026 13:40:56.684] [modloading-worker-0/WARN] [Iris/]: Found fine program match for neoforge:pipeline/entity_unlit_translucent: ENTITIES_TRANSLUCENT
[23Oct2026 13:40:57.982] [ForkJoinPool.commonPool-worker-3/WARN] [Iris/]: [Iris Update Check] This version doesn't have an update index, skipping.
[23Oct2026 13:40:58.449] [Render thread/INFO] [Iris/]: Debug functionality is disabled.
[23Oct2026 13:40:58.450] [Render thread/INFO] [Iris/]: OpenGL 4.5 detected, enabling DSA.
[23Oct2026 13:40:58.752] [Render thread/INFO] [Iris/]: Profile: HIGH (+0 options changed by user)
[23Oct2026 13:40:59.956] [Render thread/INFO] [Iris/]: Using shaderpack: ComplementaryReimagined_r5.9.3.zip
[23Oct2026 13:40:59.957] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4
[23Oct2026 13:41:00.551] [Render thread/INFO] [Iris/]: Hardware information:
[23Oct2026 13:41:00.730] [Render thread/INFO] [Iris/]: CPU: 4x AMD EPYC 7763 64-Core Processor
[23Oct2026 13:41:00.730] [Render thread/INFO] [Iris/]: GPU: llvmpipe (LLVM 20.1.2, 256 bits) (Supports OpenGL 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4)
[23Oct2026 13:41:00.730] [Render thread/INFO] [Iris/]: OS: Linux (6.17.0-1022-azure)
[23Oct2026 13:41:06.261] [Render thread/INFO] [Iris/]: Creating pipeline for dimension minecraft:overworld
[23Oct2026 13:41:08.456] [Render thread/WARN] [Iris/]: Shader compilation log for terrain_solid: 0:466(15): warning: `lmCoord' used uninitialized
[23Oct2026 13:41:09.064] [Render thread/WARN] [Iris/]: Shader compilation log for terrain_translucent: 0:467(15): warning: `lmCoord' used uninitialized
[23Oct2026 13:41:14.828] [Render thread/WARN] [Iris/]: Shader compilation log for sodium_terrain_solid: 0:541(15): warning: `lmCoord' used uninitialized
[23Oct2026 13:41:15.334] [Render thread/WARN] [Iris/]: Shader compilation log for sodium_terrain_translucent: 0:542(15): warning: `lmCoord' used uninitialized
[23Oct2026 13:41:15.453] [Render thread/WARN] [Iris/]: Shader compilation log for shadow_sodium_terrain_solid: 0:528(15): warning: `lmCoord' used uninitialized

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 13:40:46.083] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 13:40:46.150] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[23Oct2026 13:40:46.337] [ForkJoinPool.commonPool-worker-3/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
28:		Umbra 2.12.0 (umbra)
37: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
42: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
50:[23Oct2026 13:40:47.942] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
51:[23Oct2026 13:40:47.943] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
52:[23Oct2026 13:40:47.944] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
53:[23Oct2026 13:40:47.947] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
122:[23Oct2026 13:41:01.352] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
147:[23Oct2026 13:41:06.241] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
299:[23Oct2026 13:42:03.366] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
308:[23Oct2026 13:42:03.439] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
314:[23Oct2026 13:42:03.613] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
324:		Umbra 2.12.0 (umbra)
333: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
338: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
346:[23Oct2026 13:42:05.266] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
347:[23Oct2026 13:42:05.267] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
348:[23Oct2026 13:42:05.268] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
349:[23Oct2026 13:42:05.271] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
417:[23Oct2026 13:42:17.719] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
442:[23Oct2026 13:42:22.279] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
449:[23Oct2026 13:42:24.666] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
476:[23Oct2026 13:42:36.105] [Render thread/ERROR] [net.minecraft.client.Minecraft/FATAL]: Reported exception thrown!
477:net.minecraft.ReportedException: Render Frame
485:Caused by: java.lang.IllegalStateException: Missing uniform InSampler (should be COMBINED_IMAGE_SAMPLER)
501:[23Oct2026 13:42:36.921] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 493d0acd-337a-4e2e-991d-1edaf533d05f
502:[23Oct2026 13:42:36.930] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 76dfb8ed-c411-4535-a20d-dd77068c5654
507:[23Oct2026 13:42:37.830] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

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
	... 30 more


Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 36s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
[13:41:16] [Render thread/INFO] [mojang/GlDebug]: OpenGL debug message: id=71, source=SHADER COMPILER, type=OTHER, severity=HIGH, message='0:453(15): warning: `lmCoord' used uninitialized'
[13:41:16] [Render thread/INFO] [mojang/GlDebug]: OpenGL debug message: id=72, source=SHADER COMPILER, type=OTHER, severity=HIGH, message='0:461(15): warning: `lmCoord' used uninitialized'
[13:41:16] [Render thread/INFO] [mojang/GlDebug]: OpenGL debug message: id=73, source=SHADER COMPILER, type=OTHER, severity=HIGH, message='0:469(15): warning: `lmCoord' used uninitialized'
[13:41:16] [Render thread/WARN] [Iris/]: Shader compilation log for shadow_text_bg: 0:453(15): warning: `lmCoord' used uninitialized
0:461(15): warning: `lmCoord' used uninitialized
0:469(15): warning: `lmCoord' used uninitialized

[13:41:16] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[13:41:16] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[13:41:16] [Render thread/DEBUG] [mixin/]: Mixing MixinShaderChunkRenderer from mixins.iris.compat.sodium.json into net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer
[13:41:16] [Render thread/DEBUG] [mixin/]: mixins.iris.compat.sodium.json:MixinShaderChunkRenderer from mod iris: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_8 supports class version 52)
[13:41:25] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[13:41:25] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 618/9636 (6.41%) and 540 parsed for mixin
[13:41:25] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[13:41:26] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 60a598f0
[13:41:26] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792762826631.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 6s
7 actionable tasks: 7 executed
Configuration cache entry stored.
client exited with 0
