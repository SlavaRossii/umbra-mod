# sh_cursed

- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- Iris Shaders (`iris`) 1.11.7+26.3-neoforge — iris-neoforge-1.11.7+mc26.3.jar
- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar (needed by iris)
- shader pack cursed-fog 1.0.10 — Cursed Fog - V1.0.10.zip

## Result
- title screen: reached
- world tour: NOT finished (0 pictures)

## Graphics
[23Oct2026 15:02:53.654] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 15:02:53.656] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
		Iris 1.11.7+mc26.3 (iris)
[23Oct2026 15:03:04.313] [modloading-worker-0/WARN] [Iris/]: Found fine program match for neoforge:pipeline/entity_unlit_translucent: ENTITIES_TRANSLUCENT
[23Oct2026 15:03:05.768] [ForkJoinPool.commonPool-worker-1/WARN] [Iris/]: [Iris Update Check] This version doesn't have an update index, skipping.
[23Oct2026 15:03:06.136] [Render thread/INFO] [Iris/]: Debug functionality is disabled.
[23Oct2026 15:03:06.137] [Render thread/INFO] [Iris/]: OpenGL 4.5 detected, enabling DSA.
[23Oct2026 15:03:06.301] [Render thread/INFO] [Iris/]: Profile: STANDARD (+0 options changed by user)
[23Oct2026 15:03:06.430] [Render thread/INFO] [Iris/]: Using shaderpack: Cursed Fog - V1.0.10.zip
[23Oct2026 15:03:06.430] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4
[23Oct2026 15:03:07.062] [Render thread/INFO] [Iris/]: Hardware information:
[23Oct2026 15:03:07.233] [Render thread/INFO] [Iris/]: CPU: 4x AMD EPYC 7763 64-Core Processor
[23Oct2026 15:03:07.233] [Render thread/INFO] [Iris/]: GPU: llvmpipe (LLVM 20.1.2, 256 bits) (Supports OpenGL 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4)
[23Oct2026 15:03:07.233] [Render thread/INFO] [Iris/]: OS: Linux (6.17.0-1022-azure)
[23Oct2026 15:03:12.861] [Render thread/INFO] [Iris/]: Creating pipeline for dimension minecraft:overworld
[23Oct2026 15:03:59.293] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 15:03:59.295] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
		Iris 1.11.7+mc26.3 (iris)
[23Oct2026 15:04:09.694] [modloading-worker-0/WARN] [Iris/]: Found fine program match for neoforge:pipeline/entity_unlit_translucent: ENTITIES_TRANSLUCENT
[23Oct2026 15:04:10.934] [ForkJoinPool.commonPool-worker-1/WARN] [Iris/]: [Iris Update Check] This version doesn't have an update index, skipping.

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 15:02:53.590] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 15:02:53.659] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[23Oct2026 15:02:53.831] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
28:		Umbra 2.12.0 (umbra)
37: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
42: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
50:[23Oct2026 15:02:55.539] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
51:[23Oct2026 15:02:55.541] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
52:[23Oct2026 15:02:55.541] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
53:[23Oct2026 15:02:55.544] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
122:[23Oct2026 15:03:07.863] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
147:[23Oct2026 15:03:12.842] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
162:[23Oct2026 15:03:59.223] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
171:[23Oct2026 15:03:59.298] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
177:[23Oct2026 15:03:59.479] [ForkJoinPool.commonPool-worker-1/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
187:		Umbra 2.12.0 (umbra)
196: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
201: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
209:[23Oct2026 15:04:01.116] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
210:[23Oct2026 15:04:01.117] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
211:[23Oct2026 15:04:01.118] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
212:[23Oct2026 15:04:01.121] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
280:[23Oct2026 15:04:12.877] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
305:[23Oct2026 15:04:17.428] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
313:[23Oct2026 15:04:20.214] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
338:[23Oct2026 15:04:27.140] [Render thread/ERROR] [net.minecraft.client.Minecraft/FATAL]: Reported exception thrown!
339:net.minecraft.ReportedException: Render Frame
347:Caused by: java.lang.IllegalStateException: Missing uniform InSampler (should be COMBINED_IMAGE_SAMPLER)
363:[23Oct2026 15:04:27.863] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 071683e5-34b8-4ce0-bba5-8ffaf7a5c3b7
364:[23Oct2026 15:04:27.873] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 17a91207-26e7-4f44-937d-565467152813
369:[23Oct2026 15:04:28.771] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

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

BUILD FAILED in 32s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
[15:03:13] [Render thread/INFO] [mojang/GlDebug]: OpenGL debug message: id=1, source=SHADER COMPILER, type=OTHER, severity=HIGH, message='0:145(44): warning: `state' used uninitialized'
[15:03:13] [Render thread/INFO] [mojang/GlDebug]: OpenGL debug message: id=2, source=SHADER COMPILER, type=OTHER, severity=HIGH, message='0:146(34): warning: `state' used uninitialized'
[15:03:13] [Render thread/WARN] [ne.ir.ir.gl.sh.GlShader/]: Shader compilation log for composite.fsh: 0:145(44): warning: `state' used uninitialized
0:146(34): warning: `state' used uninitialized

[15:03:13] [Render thread/WARN] [ne.ir.ir.pi.tr.tr.CompatibilityTransformer/]: The in declaration 'color' in the final FRAGMENT shader is missing a corresponding out declaration in the previous stage VERTEX and has been compatibility-patched. See debugging.md for more information.
[15:03:14] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[15:03:14] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[15:03:14] [Render thread/DEBUG] [mixin/]: Mixing MixinShaderChunkRenderer from mixins.iris.compat.sodium.json into net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer
[15:03:14] [Render thread/DEBUG] [mixin/]: mixins.iris.compat.sodium.json:MixinShaderChunkRenderer from mod iris: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_8 supports class version 52)
[15:03:23] [sodium-workaround-cleanup/WARN] [Sodium-Workarounds/]: Timed out waiting for NeoForge's early renderer; restoring graphics driver workaround environment changes now
[15:03:23] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[15:03:23] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 618/9591 (6.44%) and 540 parsed for mixin
[15:03:23] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:03:23] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 78bdb72a
[15:03:23] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792767751889.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 58s
7 actionable tasks: 7 executed
Configuration cache entry stored.
client exited with 0
