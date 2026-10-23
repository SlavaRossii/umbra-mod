# sound

- Sound Physics Remastered (`sound-physics-remastered`) neoforge-1.5.2+26.3 — sound-physics-remastered-neoforge-1.5.2+26.3.jar
- AmbientSounds (`ambientsounds`) 6.3.11 — AmbientSounds_NEOFORGE_v6.3.11_mc26.3.jar
- **presence-footsteps**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- **auditory**: NO build for 26.3 NeoForge (no NeoForge builds at all)
- **sounds**: no such project on Modrinth
- **dynamic-surroundings-resurrected**: no such project on Modrinth
- CreativeCore (`creativecore`) 2.14.22 — CreativeCore_NEOFORGE_v2.14.22_mc26.3.jar (needed by ambientsounds)

## Result
- title screen: reached
- world tour: done (7 pictures)

## Graphics
[23Oct2026 15:33:27.471] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4
[23Oct2026 15:34:28.432] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 15:33:15.785] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 15:33:16.021] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
23:		Umbra 2.12.1 (umbra)
30: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
35: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
97:[23Oct2026 15:33:29.191] [modloading-worker-0/ERROR] [creativecore/]: java.util.NoSuchElementException: No value present
98:[23Oct2026 15:33:29.191] [modloading-worker-0/ERROR] [creativecore/]: Could not load default style
99:[23Oct2026 15:33:29.273] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/ambientsounds, mod/sound_physics_remastered, mod/umbra, mod/creativecore, mod/neoforge
133:[23Oct2026 15:34:17.903] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
142:[23Oct2026 15:34:18.135] [ForkJoinPool.commonPool-worker-1/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
153:		Umbra 2.12.1 (umbra)
160: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
165: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
227:[23Oct2026 15:34:30.090] [modloading-worker-0/ERROR] [creativecore/]: java.util.NoSuchElementException: No value present
228:[23Oct2026 15:34:30.090] [modloading-worker-0/ERROR] [creativecore/]: Could not load default style
229:[23Oct2026 15:34:30.215] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/ambientsounds, mod/sound_physics_remastered, mod/umbra, mod/creativecore, mod/neoforge
264:[23Oct2026 15:34:36.709] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
356:[23Oct2026 15:35:41.114] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

## Shots report
47_mob_watcher_night_180  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
74_hand_luminite_lantern_lit  using=false hand=MAIN_HAND main=1 umbra:luminite_lantern off=0 minecraft:air
104_tour_maze  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
107_tour_rift  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
116_tour_depths_landing  using=false hand=MAIN_HAND main=0 minecraft:air off=0 minecraft:air
133_lantern_night  using=false hand=MAIN_HAND main=1 umbra:luminite_lantern off=0 minecraft:air
134_lantern_night_third  using=false hand=MAIN_HAND main=1 umbra:luminite_lantern off=0 minecraft:air

## Gradle tail
[15:35:40] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 472/10461 (4.51%) and 77 parsed for mixin
[15:35:40] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:35:40] [Server thread/INFO] [minecraft/MinecraftServer]: Stopping server
[15:35:40] [Server thread/INFO] [minecraft/MinecraftServer]: Saving players
[15:35:40] [Server thread/INFO] [minecraft/ServerGamePacketListenerImpl]: Dev lost connection: Disconnected
[15:35:40] [Server thread/INFO] [minecraft/MinecraftServer]: System chat: Dev left the game
[15:35:40] [Server thread/INFO] [minecraft/ServerCommonPacketListenerImpl]: Stopping singleplayer server as player logged out
[15:35:40] [Server thread/INFO] [minecraft/MinecraftServer]: Saving worlds
[15:35:40] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:overworld
[15:35:41] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
[15:35:41] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:the_end
[15:35:41] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:the_nether
[15:35:41] [Server thread/DEBUG] [ne.ne.ne.co.CommonHooks/WP]: Gathered mod list to write to world save shots
[15:35:41] [Server thread/DEBUG] [ne.ne.fm.co.ConfigTracker/CONFIG]: Unloading configs type SYNCED
[15:35:41] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 2455c95c
[15:35:41] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_5-1792769656851.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 25s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
client exited with 0
[15:33:37] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::setSelfPosition(Lnet/minecraft/world/phys/Vec3;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[15:33:37] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::setSelfPosition(Lnet/minecraft/world/phys/Vec3;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[15:33:37] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::play(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V doesn't use it's CallbackInfo
[15:33:37] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::play(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[15:33:37] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::play(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[15:33:37] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::linearAttenuation2(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V doesn't use it's CallbackInfo
[15:33:37] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::linearAttenuation2(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V has 0 override(s) in child classes
[15:33:37] [Sound engine/DEBUG] [mixin/]: sound_physics_remastered.mixins.json:SourceMixin from mod sound_physics_remastered->@Inject::linearAttenuation2(FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V won't be passed a CallbackInfo as a result
[15:33:37] [Download-1/DEBUG] [mixin/]: Mixing OggAudioStreamMixin from ambientsounds.mixins.json into net.minecraft.client.sounds.JOrbisAudioStream
[15:33:37] [Download-1/DEBUG] [mixin/]: ambientsounds.mixins.json:OggAudioStreamMixin from mod ambientsounds: Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_21 supports class version 65)
[15:33:37] [Download-1/DEBUG] [mixin/]: Renaming synthetic method lambda$setPositionRandomly$0(F)V to md3ab220$ambientsounds$lambda$setPositionRandomly$0$0 in ambientsounds.mixins.json:OggAudioStreamMixin from mod ambientsounds
[15:33:43] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[15:33:43] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 442/8677 (5.09%) and 75 parsed for mixin
[15:33:43] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:33:43] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 6799cded
[15:33:43] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792769581472.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 48s
7 actionable tasks: 6 executed, 1 from cache
Configuration cache entry stored.
client exited with 0
