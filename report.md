# sodium_light

- Sodium (`sodium`) mc26.3-0.9.3-beta.1-neoforge — sodium-neoforge-0.9.3-beta.1+mc26.3.jar
- LambDynamicLights - Dynamic Lights (`lambdynamiclights`) 4.13.0+26.3 — lambdynamiclights-4.13.0+26.3.jar
- **P7dR8mSH**: NO build for 26.3 NeoForge (no NeoForge builds at all)

## Result
- title screen: reached
- world tour: done (13 pictures)

## Graphics
[23Oct2026 15:32:58.043] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 15:32:58.046] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
[23Oct2026 15:33:10.203] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4
[23Oct2026 15:34:03.955] [main/INFO] [Sodium-GraphicsAdapterProbe/]: Searching for graphics cards...
[23Oct2026 15:34:03.957] [main/WARN] [Sodium-GraphicsAdapterProbe/]: Could not find any graphics adapters! Probably the device is not on a bus we can probe, or there are no devices supporting 3D acceleration.
[23Oct2026 15:34:15.322] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 15:32:57.983] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 15:32:58.050] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
18:[23Oct2026 15:32:58.240] [ForkJoinPool.commonPool-worker-1/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
31:		Umbra 2.12.1 (umbra)
42: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
49: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
57:[23Oct2026 15:33:07.331] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
58:[23Oct2026 15:33:07.332] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
59:[23Oct2026 15:33:07.332] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
119:[23Oct2026 15:33:11.340] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/lambdynlights, mod/yumi_mc_core, mod/pride, mod/lambdynlights_api, mod/umbra, mod/neoforge, mod/spruceui
147:[23Oct2026 15:33:16.359] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
154:[23Oct2026 15:34:03.885] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
163:[23Oct2026 15:34:03.960] [main/WARN] [Sodium-Workarounds/]: Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [NO_ERROR_CONTEXT_UNSUPPORTED]
169:[23Oct2026 15:34:04.139] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
182:		Umbra 2.12.1 (umbra)
193: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
200: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
208:[23Oct2026 15:34:12.956] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
209:[23Oct2026 15:34:12.956] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
210:[23Oct2026 15:34:12.956] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
268:[23Oct2026 15:34:16.424] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/sodium, mod/lambdynlights, mod/yumi_mc_core, mod/pride, mod/lambdynlights_api, mod/umbra, mod/neoforge, mod/spruceui
296:[23Oct2026 15:34:20.679] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
303:[23Oct2026 15:34:23.080] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
419:[23Oct2026 15:36:21.700] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

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

## Gradle tail
[15:36:21] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:36:21] [Server thread/INFO] [minecraft/MinecraftServer]: Stopping server
[15:36:21] [Server thread/INFO] [minecraft/MinecraftServer]: Saving players
[15:36:21] [Server thread/INFO] [minecraft/ServerGamePacketListenerImpl]: Dev lost connection: Disconnected
[15:36:21] [Server thread/INFO] [minecraft/MinecraftServer]: System chat: Dev left the game
[15:36:21] [Server thread/INFO] [minecraft/ServerCommonPacketListenerImpl]: Stopping singleplayer server as player logged out
[15:36:21] [Server thread/INFO] [minecraft/MinecraftServer]: Saving worlds
[15:36:21] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:overworld
[15:36:21] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
[15:36:21] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:the_end
[15:36:21] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:the_nether
[15:36:21] [Server thread/DEBUG] [ne.ne.ne.co.CommonHooks/WP]: Gathered mod list to write to world save shots
[15:36:21] [Server thread/DEBUG] [ne.ne.fm.co.ConfigTracker/CONFIG]: Unloading configs type SYNCED
[15:36:21] [Render thread/INFO] [ChunkBuilder/]: Stopping worker threads
[15:36:22] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 5c0773c2
[15:36:22] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_5-1792769642836.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 2m 20s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
client exited with 0
[15:33:15] [Render thread/INFO] [minecraft/TextureAtlas]: Created: 512x512x0 minecraft:textures/atlas/chest.png-atlas
[15:33:15] [Render thread/INFO] [minecraft/TextureAtlas]: Created: 512x512x0 minecraft:textures/atlas/shulker_boxes.png-atlas
[15:33:15] [Render thread/INFO] [ne.ne.ne.cl.en.an.js.AnimationLoader/]: Loaded 0 entity animations
[15:33:15] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 529/8996 (5.88%) and 313 parsed for mixin
[15:33:15] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mods loaded with 4 warning(s)
[15:33:15] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod lambdynlights uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[15:33:15] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod pride uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[15:33:15] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod lambdynlights_api uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[15:33:15] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod spruceui uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[15:33:16] [Render thread/INFO] [de.um.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
[15:33:16] [Render thread/DEBUG] [io.ne.ut.in.ThreadLocalRandom/]: -Dio.netty.initialSeedUniquifier: 0x494e900c55d749d8
[15:33:26] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[15:33:26] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 531/9117 (5.82%) and 313 parsed for mixin
[15:33:26] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:33:26] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 12837172
[15:33:26] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792769556811.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 56s
7 actionable tasks: 7 executed
Configuration cache entry stored.
client exited with 0
