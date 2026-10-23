# sh_sildur

- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- Iris Shaders (`iris`) 1.11.7+26.3-neoforge — iris-neoforge-1.11.7+mc26.3.jar
- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar (needed by iris)
- shader pack sildurs-enhanced-default-shaders 1.20 — Sildur's Enhanced Default v1.20 Fancy.zip

## Result
- title screen: reached
- world tour: NOT finished (0 pictures)

## Graphics
[23Oct2026 15:02:55.442] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 15:02:55.444] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
		Iris 1.11.7+mc26.3 (iris)
[23Oct2026 15:03:05.841] [modloading-worker-0/WARN] [Iris/]: Found fine program match for neoforge:pipeline/entity_unlit_translucent: ENTITIES_TRANSLUCENT
[23Oct2026 15:03:07.115] [ForkJoinPool.commonPool-worker-2/WARN] [Iris/]: [Iris Update Check] This version doesn't have an update index, skipping.
[23Oct2026 15:03:07.552] [Render thread/INFO] [Iris/]: Debug functionality is disabled.
[23Oct2026 15:03:07.553] [Render thread/INFO] [Iris/]: OpenGL 4.5 detected, enabling DSA.
[23Oct2026 15:03:07.694] [Render thread/INFO] [Iris/]: Profile: Fancy (+0 options changed by user)
[23Oct2026 15:03:07.816] [Render thread/INFO] [Iris/]: Using shaderpack: Sildur's Enhanced Default v1.20 Fancy.zip
[23Oct2026 15:03:07.817] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4
[23Oct2026 15:03:08.438] [Render thread/INFO] [Iris/]: Hardware information:
[23Oct2026 15:03:08.618] [Render thread/INFO] [Iris/]: CPU: 4x AMD EPYC 7763 64-Core Processor
[23Oct2026 15:03:08.618] [Render thread/INFO] [Iris/]: GPU: llvmpipe (LLVM 20.1.2, 256 bits) (Supports OpenGL 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4)
[23Oct2026 15:03:08.618] [Render thread/INFO] [Iris/]: OS: Linux (6.17.0-1022-azure)
[23Oct2026 15:03:14.556] [Render thread/INFO] [Iris/]: Creating pipeline for dimension minecraft:overworld
[23Oct2026 15:04:03.354] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 15:04:03.356] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
		Iris 1.11.7+mc26.3 (iris)
[23Oct2026 15:04:13.498] [modloading-worker-0/WARN] [Iris/]: Found fine program match for neoforge:pipeline/entity_unlit_translucent: ENTITIES_TRANSLUCENT
[23Oct2026 15:04:14.768] [ForkJoinPool.commonPool-worker-2/WARN] [Iris/]: [Iris Update Check] This version doesn't have an update index, skipping.

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 15:02:55.375] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 15:02:55.447] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[23Oct2026 15:02:55.635] [ForkJoinPool.commonPool-worker-3/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
28:		Umbra 2.12.0 (umbra)
37: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
42: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
50:[23Oct2026 15:02:57.336] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
51:[23Oct2026 15:02:57.338] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
52:[23Oct2026 15:02:57.339] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
53:[23Oct2026 15:02:57.341] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
122:[23Oct2026 15:03:09.249] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
147:[23Oct2026 15:03:14.532] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
183:[23Oct2026 15:04:03.286] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
192:[23Oct2026 15:04:03.360] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
198:[23Oct2026 15:04:03.521] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
208:		Umbra 2.12.0 (umbra)
217: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
222: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
230:[23Oct2026 15:04:05.081] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
231:[23Oct2026 15:04:05.083] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
232:[23Oct2026 15:04:05.083] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
233:[23Oct2026 15:04:05.086] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
301:[23Oct2026 15:04:16.678] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
326:[23Oct2026 15:04:21.451] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
335:[23Oct2026 15:04:24.136] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
384:[23Oct2026 15:04:31.317] [Render thread/ERROR] [net.minecraft.client.Minecraft/FATAL]: Reported exception thrown!
385:net.minecraft.ReportedException: Render Frame
393:Caused by: java.lang.IllegalStateException: Missing uniform InSampler (should be COMBINED_IMAGE_SAMPLER)
409:[23Oct2026 15:04:32.073] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 33e00930-6608-4d50-b949-e2fdbd0c68bd
410:[23Oct2026 15:04:32.082] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 20008e66-8878-4cfc-b3c5-eb9f5c9c9e14
417:[23Oct2026 15:04:33.020] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

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

BUILD FAILED in 32s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
[15:03:16] [Render thread/WARN] [ne.ir.ir.pi.tr.tr.CompatibilityTransformer/]: The in declaration 'NdotLight' in the text FRAGMENT shader that is never assigned to in the previous stage VERTEX has been compatibility-patched by adding an initialization for it. See debugging.md for more information.
[15:03:16] [Render thread/WARN] [ne.ir.ir.pi.tr.tr.CompatibilityTransformer/]: The in declaration 'NdotLight' in the glint FRAGMENT shader that is never assigned to in the previous stage VERTEX has been compatibility-patched by adding an initialization for it. See debugging.md for more information.
[15:03:16] [Render thread/WARN] [ne.ir.ir.pi.tr.tr.CompatibilityTransformer/]: The in declaration 'NdotLight' in the mekanism_flame FRAGMENT shader that is never assigned to in the previous stage VERTEX has been compatibility-patched by adding an initialization for it. See debugging.md for more information.
[15:03:16] [Render thread/WARN] [ne.ir.ir.pi.tr.tr.CompatibilityTransformer/]: The in declaration 'NdotLight' in the sodium_terrain_solid FRAGMENT shader that is never assigned to in the previous stage VERTEX has been compatibility-patched by adding an initialization for it. See debugging.md for more information.
[15:03:16] [Render thread/WARN] [ne.ir.ir.pi.tr.tr.CompatibilityTransformer/]: The in declaration 'NdotLight' in the sodium_terrain_cutout FRAGMENT shader that is never assigned to in the previous stage VERTEX has been compatibility-patched by adding an initialization for it. See debugging.md for more information.
[15:03:16] [Render thread/WARN] [ne.ir.ir.pi.tr.tr.CompatibilityTransformer/]: The in declaration 'NdotLight' in the sodium_terrain_translucent FRAGMENT shader that is never assigned to in the previous stage VERTEX has been compatibility-patched by adding an initialization for it. See debugging.md for more information.
[15:03:16] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[15:03:16] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[15:03:16] [Render thread/DEBUG] [mixin/]: Mixing MixinShaderChunkRenderer from mixins.iris.compat.sodium.json into net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer
[15:03:16] [Render thread/DEBUG] [mixin/]: mixins.iris.compat.sodium.json:MixinShaderChunkRenderer from mod iris: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_8 supports class version 52)
[15:03:25] [sodium-workaround-cleanup/WARN] [Sodium-Workarounds/]: Timed out waiting for NeoForge's early renderer; restoring graphics driver workaround environment changes now
[15:03:26] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[15:03:26] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 618/9624 (6.42%) and 540 parsed for mixin
[15:03:26] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:03:26] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 6231e197
[15:03:26] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792767754212.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 57s
7 actionable tasks: 7 executed
Configuration cache entry stored.
client exited with 0
