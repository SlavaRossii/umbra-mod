# sh_makeup

- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- Iris Shaders (`iris`) 1.11.7+26.3-neoforge — iris-neoforge-1.11.7+mc26.3.jar
- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar (needed by iris)
- shader pack makeup-ultra-fast-shaders 9.6 — MakeUp-UltraFast-9.6.zip

## Result
- title screen: reached
- world tour: NOT finished (0 pictures)

## Graphics
[23Oct2026 15:03:08.328] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 15:03:08.329] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
		Iris 1.11.7+mc26.3 (iris)
[23Oct2026 15:03:19.462] [modloading-worker-0/WARN] [Iris/]: Found fine program match for neoforge:pipeline/entity_unlit_translucent: ENTITIES_TRANSLUCENT
[23Oct2026 15:03:20.553] [ForkJoinPool.commonPool-worker-2/WARN] [Iris/]: [Iris Update Check] This version doesn't have an update index, skipping.
[23Oct2026 15:03:21.524] [Render thread/INFO] [Iris/]: Debug functionality is disabled.
[23Oct2026 15:03:21.525] [Render thread/INFO] [Iris/]: OpenGL 4.5 detected, enabling DSA.
[23Oct2026 15:03:21.752] [Render thread/INFO] [Iris/]: Profile: medium (+0 options changed by user)
[23Oct2026 15:03:22.308] [Render thread/WARN] [Iris/]: Warning: the block ID map entry "betterendforge::lumecorn:shape=light_top_middle" could not be fully parsed:
[23Oct2026 15:03:22.309] [Render thread/WARN] [Iris/]: - Block state property filters must be of the form "key=value", but lumecorn is not of that form!
[23Oct2026 15:03:22.309] [Render thread/WARN] [Iris/]: Warning: the block ID map entry "betterendforge::lumecorn:shape=light_middle" could not be fully parsed:
[23Oct2026 15:03:22.309] [Render thread/WARN] [Iris/]: - Block state property filters must be of the form "key=value", but lumecorn is not of that form!
[23Oct2026 15:03:22.309] [Render thread/WARN] [Iris/]: Warning: the block ID map entry "betterendforge::lumecorn:shape=light_bottom" could not be fully parsed:
[23Oct2026 15:03:22.309] [Render thread/WARN] [Iris/]: - Block state property filters must be of the form "key=value", but lumecorn is not of that form!
[23Oct2026 15:03:22.310] [Render thread/WARN] [Iris/]: Warning: the block ID map entry "betterendforge::purple_polypore" could not be fully parsed:
[23Oct2026 15:03:22.310] [Render thread/WARN] [Iris/]: - Block state property filters must be of the form "key=value", but purple_polypore is not of that form!
[23Oct2026 15:03:22.310] [Render thread/WARN] [Iris/]: Warning: the block ID map entry "betterendforge::bulb_vine:shape=bottom" could not be fully parsed:
[23Oct2026 15:03:22.310] [Render thread/WARN] [Iris/]: - Block state property filters must be of the form "key=value", but bulb_vine is not of that form!
[23Oct2026 15:03:22.318] [Render thread/INFO] [Iris/]: Using shaderpack: MakeUp-UltraFast-9.6.zip
[23Oct2026 15:03:22.318] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 15:03:08.261] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 15:03:08.333] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[23Oct2026 15:03:08.520] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
28:		Umbra 2.12.0 (umbra)
37: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
42: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
50:[23Oct2026 15:03:10.242] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
51:[23Oct2026 15:03:10.244] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
52:[23Oct2026 15:03:10.245] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
53:[23Oct2026 15:03:10.247] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
132:[23Oct2026 15:03:23.737] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
157:[23Oct2026 15:03:28.890] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
182:java.lang.RuntimeException: Couldn't resolve: 
218:	at TRANSFORMER/umbra@2.12.0/dev.umbra.client.CiWarnings.onTick(CiWarnings.java:32) ~[main/:?]
237:[23Oct2026 15:04:17.747] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
246:[23Oct2026 15:04:17.817] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
252:[23Oct2026 15:04:17.995] [ForkJoinPool.commonPool-worker-3/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
262:		Umbra 2.12.0 (umbra)
271: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
276: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
284:[23Oct2026 15:04:19.605] [main/WARN] [mixin/]: Error loading class: com/portingdeadmods/cable_facades/events/GameClientEvents$2 (java.lang.ClassNotFoundException: com.portingdeadmods.cable_facades.events.GameClientEvents$2)
285:[23Oct2026 15:04:19.607] [main/WARN] [mixin/]: Error loading class: com/direwolf20/justdirethings/client/renderers/OurRenderTypes (java.lang.ClassNotFoundException: com.direwolf20.justdirethings.client.renderers.OurRenderTypes)
286:[23Oct2026 15:04:19.608] [main/WARN] [mixin/]: Error loading class: mekanism/client/render/lib/effect/BillboardingEffectRenderer (java.lang.ClassNotFoundException: mekanism.client.render.lib.effect.BillboardingEffectRenderer)
287:[23Oct2026 15:04:19.610] [main/WARN] [mixin/]: Error loading class: blusunrize/immersiveengineering/client/utils/IEGLShaders (java.lang.ClassNotFoundException: blusunrize.immersiveengineering.client.utils.IEGLShaders)
365:[23Oct2026 15:04:31.601] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/iris, mod/umbra, mod/neoforge
390:[23Oct2026 15:04:36.375] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
400:[23Oct2026 15:04:38.867] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
437:java.lang.RuntimeException: Couldn't resolve: 
489:[23Oct2026 15:04:47.780] [Render thread/ERROR] [net.minecraft.client.Minecraft/FATAL]: Reported exception thrown!
490:net.minecraft.ReportedException: Render Frame
498:Caused by: java.lang.IllegalStateException: Missing uniform InSampler (should be COMBINED_IMAGE_SAMPLER)
515:[23Oct2026 15:04:48.513] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID fc8d8f39-b94e-41fa-9134-1356b51b867b
516:[23Oct2026 15:04:48.519] [Render thread/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 4e62ffbb-49c3-4768-bd60-004c1248fdff
523:[23Oct2026 15:04:49.425] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

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

BUILD FAILED in 34s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
	at TRANSFORMER/minecraft@26.3/net.minecraft.client.Minecraft.run(Minecraft.java:938) [minecraft-patched-26.3.0.76.jar:?] {neoforge:access_transformer,neoforge:access_transformer,neoforge:mixin[APP:mixins.iris.json:MixinOptions_Entrypoint from mod iris,APP:sodium-common.mixins.json:core.MinecraftMixin from mod sodium,APP:sodium-common.mixins.json:features.gui.hooks.debug.MinecraftRenderFrameMixin from mod sodium,APP:sodium-neoforge.mixins.json:platform.neoforge.EntrypointMixin from mod sodium,APP:mixins.iris.json:MixinMinecraft_Images from mod iris,APP:mixins.iris.json:MixinMinecraft_Keybinds from mod iris,APP:mixins.iris.json:MixinMinecraft_PipelineManagement from mod iris,APP:mixins.iris.json:entity_render_context.MixinEntityRenderer from mod iris]}
	at TRANSFORMER/minecraft@26.3/net.minecraft.client.main.Main.main(Main.java:294) [minecraft-patched-26.3.0.76.jar:?] {}
	at net.neoforged.fml.startup.Client.main(Client.java:19) [loader-12.0.12.jar:12.0] {}
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[?:?] {}
	at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[?:?] {}
	at net.neoforged.devlaunch.Main.main(Main.java:57) [DevLaunch-1.0.2.jar:?] {}
[15:03:32] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[15:03:32] [Render thread/DEBUG] [mixin/]: Generating mapped inner class com/mojang/renderpearl/backend/opengl/GlProgram$Anonymous$55a7f69bf8ee34c1a9edb88414170abe (originally net/irisshaders/iris/mixin/MixinGlProgram$1)
[15:03:32] [Render thread/DEBUG] [mixin/]: Mixing MixinShaderChunkRenderer from mixins.iris.compat.sodium.json into net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer
[15:03:32] [Render thread/DEBUG] [mixin/]: mixins.iris.compat.sodium.json:MixinShaderChunkRenderer from mod iris: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_8 supports class version 52)
[15:03:38] [sodium-workaround-cleanup/WARN] [Sodium-Workarounds/]: Timed out waiting for NeoForge's early renderer; restoring graphics driver workaround environment changes now
[15:03:41] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[15:03:41] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 618/9627 (6.42%) and 540 parsed for mixin
[15:03:41] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:03:42] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 7fe3f2cb
[15:03:42] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792767768492.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 3s
7 actionable tasks: 7 executed
Configuration cache entry stored.
client exited with 0
