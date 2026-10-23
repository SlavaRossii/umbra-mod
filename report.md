# sh_vanilla

- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- Iris Shaders (`iris`) 1.11.7+26.3-neoforge — iris-neoforge-1.11.7+mc26.3.jar
- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar (needed by iris)
- shader pack i-like-vanilla v1.5.0b — I Like Vanilla v1.5.0b.zip

## Result
- title screen: reached
- world tour: NOT finished (0 pictures)

## Graphics
[23Oct2026 15:03:07.978] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 15:03:07.980] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
		Iris 1.11.7+mc26.3 (iris)
[23Oct2026 15:03:18.182] [modloading-worker-0/WARN] [Iris/]: Found fine program match for neoforge:pipeline/entity_unlit_translucent: ENTITIES_TRANSLUCENT
[23Oct2026 15:03:19.534] [ForkJoinPool.commonPool-worker-3/WARN] [Iris/]: [Iris Update Check] This version doesn't have an update index, skipping.
[23Oct2026 15:03:20.208] [Render thread/INFO] [Iris/]: Debug functionality is disabled.
[23Oct2026 15:03:20.209] [Render thread/INFO] [Iris/]: OpenGL 4.5 detected, enabling DSA.
[23Oct2026 15:03:20.537] [Render thread/INFO] [Iris/]: Profile: MEDIUM (+0 options changed by user)
[23Oct2026 15:03:21.710] [Render thread/INFO] [Iris/]: Using shaderpack: I Like Vanilla v1.5.0b.zip
[23Oct2026 15:03:21.710] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4
[23Oct2026 15:03:22.342] [Render thread/INFO] [Iris/]: Hardware information:
[23Oct2026 15:03:22.509] [Render thread/INFO] [Iris/]: CPU: 4x AMD EPYC 7763 64-Core Processor
[23Oct2026 15:03:22.510] [Render thread/INFO] [Iris/]: GPU: llvmpipe (LLVM 20.1.2, 256 bits) (Supports OpenGL 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4)
[23Oct2026 15:03:22.510] [Render thread/INFO] [Iris/]: OS: Linux (6.17.0-1022-azure)
[23Oct2026 15:03:28.166] [Render thread/INFO] [Iris/]: Creating pipeline for dimension minecraft:overworld
[23Oct2026 15:03:30.791] [Render thread/WARN] [Iris/]: Shader compilation log for terrain_solid: 0:462(42): warning: `lmcoord' used uninitialized
[23Oct2026 15:03:32.712] [Render thread/WARN] [Iris/]: Shader compilation log for sodium_terrain_solid: 0:537(42): warning: `lmcoord' used uninitialized
[23Oct2026 15:04:20.644] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 15:04:20.646] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
		Iris 1.11.7+mc26.3 (iris)

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 15:03:07.915] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 15:03:07.983] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[23Oct2026 15:03:08.155] [ForkJoinPool.commonPool-worker-3/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
28:		Umbra 2.12.0 (umbra)
37: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
42: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
50:[23Oct2026 15:03:09.828] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
51:[23Oct2026 15:03:09.829] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
52:[23Oct2026 15:03:09.830] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
53:[23Oct2026 15:03:09.833] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
122:[23Oct2026 15:03:23.071] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
147:[23Oct2026 15:03:28.146] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
164:[23Oct2026 15:04:20.580] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
173:[23Oct2026 15:04:20.649] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
179:[23Oct2026 15:04:20.834] [ForkJoinPool.commonPool-worker-3/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
189:		Umbra 2.12.0 (umbra)
198: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
203: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
211:[23Oct2026 15:04:22.448] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
212:[23Oct2026 15:04:22.450] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
213:[23Oct2026 15:04:22.450] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
214:[23Oct2026 15:04:22.453] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
282:[23Oct2026 15:04:35.386] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
307:[23Oct2026 15:04:40.101] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
316:[23Oct2026 15:04:42.662] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
339:[23Oct2026 15:04:52.999] [Render thread/ERROR] [net.minecraft.client.Minecraft/FATAL]: Reported exception thrown!
340:net.minecraft.ReportedException: Render Frame
348:Caused by: java.lang.IllegalStateException: Missing uniform InSampler (should be COMBINED_IMAGE_SAMPLER)
364:[23Oct2026 15:04:53.841] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID e3589045-753e-4dc9-a4af-9c14dfb089a7
365:[23Oct2026 15:04:53.853] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID f9b62ce7-bac4-47a0-88e7-1300fbd693b1
371:[23Oct2026 15:04:54.757] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

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

[15:03:30] [Render thread/INFO] [mojang/GlDebug]: OpenGL debug message: id=2, source=SHADER COMPILER, type=OTHER, severity=HIGH, message='0:462(42): warning: `lmcoord' used uninitialized'
[15:03:32] [Render thread/INFO] [mojang/GlDebug]: OpenGL debug message: id=3, source=SHADER COMPILER, type=OTHER, severity=HIGH, message='0:537(42): warning: `lmcoord' used uninitialized'
[15:03:32] [Render thread/WARN] [Iris/]: Shader compilation log for sodium_terrain_solid: 0:537(42): warning: `lmcoord' used uninitialized

[15:03:32] [Render thread/INFO] [mojang/GlDebug]: OpenGL debug message: id=4, source=SHADER COMPILER, type=OTHER, severity=HIGH, message='0:537(42): warning: `lmcoord' used uninitialized'
[15:03:33] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[15:03:33] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[15:03:33] [Render thread/DEBUG] [mixin/]: Mixing MixinShaderChunkRenderer from mixins.iris.compat.sodium.json into net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer
[15:03:33] [Render thread/DEBUG] [mixin/]: mixins.iris.compat.sodium.json:MixinShaderChunkRenderer from mod iris: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_8 supports class version 52)
[15:03:37] [sodium-workaround-cleanup/WARN] [Sodium-Workarounds/]: Timed out waiting for NeoForge's early renderer; restoring graphics driver workaround environment changes now
[15:03:43] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[15:03:43] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 618/9637 (6.41%) and 540 parsed for mixin
[15:03:43] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:03:43] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 608f2d7c
[15:03:43] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792767771053.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 57s
7 actionable tasks: 7 executed
Configuration cache entry stored.
client exited with 0
