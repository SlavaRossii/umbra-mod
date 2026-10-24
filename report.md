# opt

- FerriteCore (`ferrite-core`) 9.0.0-neoforge — ferritecore-9.0.0-neoforge.jar
- ImmediatelyFast (`immediatelyfast`) 1.17.1+26.3-neoforge — ImmediatelyFast-NeoForge-1.17.1+26.3.jar
- Entity Culling (`entityculling`) 1.11.3 — entityculling-neoforge-1.11.3-mc26.3.jar
- Sound Physics Remastered (`sound-physics-remastered`) neoforge-1.5.2+26.3 — sound-physics-remastered-neoforge-1.5.2+26.3.jar
- AmbientSounds (`ambientsounds`) 6.3.11 — AmbientSounds_NEOFORGE_v6.3.11_mc26.3.jar
- LambDynamicLights - Dynamic Lights (`lambdynamiclights`) 4.13.0+26.3 — lambdynamiclights-4.13.0+26.3.jar
- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- CreativeCore (`creativecore`) 2.14.24 — CreativeCore_NEOFORGE_v2.14.24_mc26.3.jar (needed by ambientsounds)
- **P7dR8mSH**: NO build for 26.3 NeoForge (no NeoForge builds at all)

## Result
- title screen: reached
- world tour: done (15 pictures)

## Graphics
[24Oct2026 20:28:55.446] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[24Oct2026 20:28:55.447] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
[24Oct2026 20:29:04.448] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4
[24Oct2026 20:29:50.984] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[24Oct2026 20:29:50.986] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
[24Oct2026 20:29:59.109] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[24Oct2026 20:28:55.403] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[24Oct2026 20:28:55.449] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[24Oct2026 20:28:55.568] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
39:		Umbra 2.12.2 (umbra)
55: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
65: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
75:[24Oct2026 20:29:01.970] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
76:[24Oct2026 20:29:01.970] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
77:[24Oct2026 20:29:01.970] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
141:[24Oct2026 20:29:06.119] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/entityculling, mod/lambdynlights, mod/yumi_mc_core, mod/ambientsounds, mod/immediatelyfast, mod/lambdynlights_api, mod/ferritecore, mod/creativecore, mod/transition, mod/trender, mod/pride, mod/sound_physics_remastered, mod/umbra, mod/neoforge, mod/spruceui
179:[24Oct2026 20:29:09.778] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
187:[24Oct2026 20:29:50.935] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
196:[24Oct2026 20:29:50.988] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
202:[24Oct2026 20:29:51.110] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
223:		Umbra 2.12.2 (umbra)
239: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
249: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
259:[24Oct2026 20:29:57.327] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
260:[24Oct2026 20:29:57.327] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
261:[24Oct2026 20:29:57.327] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
323:[24Oct2026 20:30:00.801] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/entityculling, mod/lambdynlights, mod/yumi_mc_core, mod/ambientsounds, mod/immediatelyfast, mod/lambdynlights_api, mod/ferritecore, mod/creativecore, mod/transition, mod/trender, mod/pride, mod/sound_physics_remastered, mod/umbra, mod/neoforge, mod/spruceui
361:[24Oct2026 20:30:04.072] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
371:[24Oct2026 20:30:05.721] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
497:[24Oct2026 20:32:07.051] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

## Shots report
35_mob_primordial_night_180  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
47_mob_watcher_night_180  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
74_hand_luminite_lantern_lit  using=false hand=MAIN_HAND main=1 umbra:luminite_lantern off=0 minecraft:air
82_third_luminite_spear  using=false hand=MAIN_HAND main=1 umbra:luminite_spear off=0 minecraft:air
99_tour_lighthouse  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
101_tour_village_street  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
104_tour_maze  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
107_tour_rift  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
111_tour_citadel_1  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
116_tour_depths_landing  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
120_tour_depths_sanctum  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
133_lantern_night  using=false hand=MAIN_HAND main=1 umbra:luminite_lantern off=0 minecraft:air
134_lantern_night_third  using=false hand=MAIN_HAND main=1 umbra:luminite_lantern off=0 minecraft:air
135_goggles_third  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
136_goggles_night  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air

## Gradle tail
[20:32:06] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[20:32:06] [Server thread/INFO] [minecraft/MinecraftServer]: Stopping server
[20:32:06] [Server thread/INFO] [minecraft/MinecraftServer]: Saving players
[20:32:06] [Server thread/INFO] [minecraft/ServerGamePacketListenerImpl]: Dev lost connection: Disconnected
[20:32:06] [Server thread/INFO] [minecraft/MinecraftServer]: System chat: Dev left the game
[20:32:06] [Server thread/INFO] [minecraft/ServerCommonPacketListenerImpl]: Stopping singleplayer server as player logged out
[20:32:06] [Server thread/INFO] [minecraft/MinecraftServer]: Saving worlds
[20:32:06] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:overworld
[20:32:07] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:the_end
[20:32:07] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:the_nether
[20:32:07] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
[20:32:07] [Server thread/DEBUG] [ne.ne.ne.co.CommonHooks/WP]: Gathered mod list to write to world save shots
[20:32:07] [Server thread/DEBUG] [ne.ne.fm.co.ConfigTracker/CONFIG]: Unloading configs type SYNCED
[20:32:07] [Render thread/INFO] [ChunkBuilder/]: Stopping worker threads
[20:32:07] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 48916da
[20:32:07] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_5-1792873790182.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 2m 17s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
client exited with 0
[20:29:13] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::setSelfPosition(Lnet/minecraft/world/phys/Vec3;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[20:29:13] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::play(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V doesn't use it's CallbackInfo
[20:29:13] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::play(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[20:29:13] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::play(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[20:29:13] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::linearAttenuation2(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V doesn't use it's CallbackInfo
[20:29:13] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::linearAttenuation2(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[20:29:13] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::linearAttenuation2(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[20:29:13] [Download-1/DEBUG] [mixin/]: Mixing OggAudioStreamMixin from ambientsounds.mixins.json into net.minecraft.client.sounds.JOrbisAudioStream
[20:29:13] [Download-1/DEBUG] [mixin/]: ambientsounds.mixins.json:OggAudioStreamMixin from mod ambientsounds: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_21 supports class version 65)
[20:29:13] [Download-1/DEBUG] [mixin/]: Renaming synthetic method lambda$setPositionRandomly$0(F)V to mdf85f33$ambientsounds$lambda$setPositionRandomly$0$0 in ambientsounds.mixins.json:OggAudioStreamMixin from mod ambientsounds
[20:29:19] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[20:29:19] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 602/9771 (6.16%) and 461 parsed for mixin
[20:29:19] [CullThread/INFO] [minecraft/LoggedPrintStream]: [STDOUT]: Shutting down culling task!
[20:29:19] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[20:29:19] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 360fe828
[20:29:19] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792873709027.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 1s
7 actionable tasks: 7 executed
Configuration cache entry stored.
client exited with 0
