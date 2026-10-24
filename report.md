# extra

- Not Enough Animations (`not-enough-animations`) 1.12.6 — notenoughanimations-neoforge-1.12.6-mc26.3.jar
- 3D Skin Layers (`3dskinlayers`) 1.11.4 — skinlayers3d-neoforge-1.11.4-mc26.3.jar
- Subtle Effects (`subtle-effects`) 1.15.1-hotfix.1 — SubtleEffects-neoforge-26.3-1.15.1-hotfix.1.jar
- Entity Culling (`entityculling`) 1.11.3 — entityculling-neoforge-1.11.3-mc26.3.jar
- FerriteCore (`ferrite-core`) 9.0.0-neoforge — ferritecore-9.0.0-neoforge.jar
- ImmediatelyFast (`immediatelyfast`) 1.17.1+26.3-neoforge — ImmediatelyFast-NeoForge-1.17.1+26.3.jar
- Better Clouds (`better-clouds`) 1.15.0-beta.6 — better-clouds-1.15.0-beta.6+26.3-neoforge.jar
- **cave-dust**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- Particular ✨ Reforged (`particular-reforged`) 1.5.7 — particular-26.3-NeoForge-1.5.7.jar
- **true-darkness-refabricated**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- **fog-looks-good-now**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- Zoomify (Zoom) (`zoomify`) 2.17.0+mc26.3 — zoomify-2.17.0+mc26.3-universal.jar
- Sound Physics Remastered (`sound-physics-remastered`) neoforge-1.5.2+26.3 — sound-physics-remastered-neoforge-1.5.2+26.3.jar
- LambDynamicLights - Dynamic Lights (`lambdynamiclights`) 4.13.0+26.3 — lambdynamiclights-4.13.0+26.3.jar
- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- Fzzy Config (`fzzy-config`) 0.7.7+fix3+26.3+neoforge — fzzy_config-0.7.7+fix3+26.3+neoforge.jar (needed by subtle-effects)
- YetAnotherConfigLib (YACL) (`yacl`) 3.9.7+26.3-neoforge — yet_another_config_lib_v3-3.9.7+26.3-neoforge.jar (needed by better-clouds)
- BaguetteLib (`baguettelib`) 2.0.8 — baguettelib-26.3-NeoForge-2.0.8.jar (needed by particular-reforged)
- **Ha28R6CL**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- **P7dR8mSH**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- KotlinLangForge (`kotlin-lang-forge`) 2.16.0-k2.4.21-3.1+neoforge — KotlinLangForge-2.16.0-k2.4.21-3.1+neoforge.jar (needed by zoomify)

## Result
- title screen: NOT reached
- world tour: NOT finished (0 pictures)

## Graphics
[24Oct2026 19:25:05.249] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[24Oct2026 19:25:05.251] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
[24Oct2026 19:44:22.696] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[24Oct2026 19:44:22.698] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[24Oct2026 19:25:05.184] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[24Oct2026 19:25:05.254] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[24Oct2026 19:25:05.445] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
45:		Umbra 2.12.2 (umbra)
90: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
105: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
118:[24Oct2026 19:25:07.969] [main/WARN] [mixin/]: Error loading class: traben/entity_model_features/models/parts/EMFModelPart (java.lang.ClassNotFoundException: traben.entity_model_features.models.parts.EMFModelPart)
122:[24Oct2026 19:25:15.996] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
123:[24Oct2026 19:25:15.996] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
124:[24Oct2026 19:25:15.996] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
134:[24Oct2026 19:25:17.048] [modloading-worker-0/ERROR] [net.neoforged.fml.javafmlmod.FMLModContainer/LOADING]: Failed to create mod instance. ModID: particular, class com.leclowndu93150.particular.Main
152:[24Oct2026 19:25:17.135] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Config file '/home/runner/work/umbra-mod/umbra-mod/run-client/config/yacl.json5' does not exist. Creating it with default values.
153:[24Oct2026 19:25:17.141] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Serializing class dev.isxander.yacl3.platform.YACLConfig to '/home/runner/work/umbra-mod/umbra-mod/run-client/config/yacl.json5'
154:[24Oct2026 19:25:17.158] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Config file '/home/runner/work/umbra-mod/umbra-mod/run-client/config/betterclouds-v1.json' does not exist. Creating it with default values.
155:[24Oct2026 19:25:17.158] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Serializing class com.qendolin.betterclouds.config.Config to '/home/runner/work/umbra-mod/umbra-mod/run-client/config/betterclouds-v1.json'
163:[24Oct2026 19:25:18.863] [main/FATAL] [net.neoforged.fml.ModLoader/LOADING]: Failed to wait for future Mod Construction, 1 errors found
164:[24Oct2026 19:25:19.421] [main/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 81f034de-af42-4b9c-8c31-9e0160db775d
165:[24Oct2026 19:25:19.446] [main/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 17f0c086-153c-4944-afaa-3ac581ec0654
171:[24Oct2026 19:44:22.625] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
180:[24Oct2026 19:44:22.701] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
186:[24Oct2026 19:44:22.882] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
213:		Umbra 2.12.2 (umbra)
258: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
273: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
286:[24Oct2026 19:44:25.216] [main/WARN] [mixin/]: Error loading class: traben/entity_model_features/models/parts/EMFModelPart (java.lang.ClassNotFoundException: traben.entity_model_features.models.parts.EMFModelPart)
290:[24Oct2026 19:44:33.233] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
291:[24Oct2026 19:44:33.233] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
292:[24Oct2026 19:44:33.233] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
302:[24Oct2026 19:44:34.143] [modloading-worker-0/ERROR] [net.neoforged.fml.javafmlmod.FMLModContainer/LOADING]: Failed to create mod instance. ModID: particular, class com.leclowndu93150.particular.Main
318:[24Oct2026 19:44:34.188] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Deserializing YACLConfig from '/home/runner/work/umbra-mod/umbra-mod/run-client/config/yacl.json5'
321:[24Oct2026 19:44:34.219] [modloading-worker-0/INFO] [YetAnotherConfigLib/]: Deserializing Config from '/home/runner/work/umbra-mod/umbra-mod/run-client/config/betterclouds-v1.json'
329:[24Oct2026 19:44:35.699] [main/FATAL] [net.neoforged.fml.ModLoader/LOADING]: Failed to wait for future Mod Construction, 1 errors found
330:[24Oct2026 19:44:36.266] [main/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 53ec8d52-6e74-4190-b0cf-3d534a9ca693
331:[24Oct2026 19:44:36.289] [main/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID e4b6faa3-502d-4896-9e80-a128ef3bf417

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

BUILD FAILED in 15s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
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

BUILD FAILED in 41s
7 actionable tasks: 7 executed
Configuration cache entry stored.
