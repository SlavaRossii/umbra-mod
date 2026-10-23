# light

- LambDynamicLights - Dynamic Lights (`lambdynamiclights`) 4.13.0+26.3 — lambdynamiclights-4.13.0+26.3.jar
- **P7dR8mSH**: NO build for 26.3 NeoForge (no NeoForge builds at all)

## Inside the jars: dynamiclights
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/allay.json
```
{
	"match": {
		"type": "minecraft:allay"
	},
	"luminance": 8
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/blaze.json
```
{
	"match": {
		"type": "minecraft:blaze"
	},
	"luminance": {
		"type": "lambdynlights:wet_sensitive",
		"dry": 10,
		"wet": 4
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/block_display.json
```
{
	"match": {
		"type": "minecraft:block_display"
	},
	"luminance": {
		"type": "lambdynlights:display",
		"luminance": {
			"type": "lambdynlights:display/block"
		}
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/creeper.json
```
{
	"match": {
		"type": "minecraft:creeper"
	},
	"luminance": {
		"type": "lambdynlights:creeper"
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/dragon_fireball.json
```
{
	"match": {
		"type": "minecraft:dragon_fireball"
	},
	"luminance": 14
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/enderman.json
```
{
	"match": {
		"type": "minecraft:enderman"
	},
	"luminance": {
		"type": "lambdynlights:enderman"
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/fireball.json
```
{
	"match": {
		"type": "minecraft:fireball"
	},
	"luminance": {
		"type": "lambdynlights:water_sensitive",
		"out_of_water": 14
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/glow_item_frame.json
```
{
	"match": {
		"type": "minecraft:glow_item_frame"
	},
	"luminance": 12
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/glow_squid.json
```
{
	"match": {
		"type": "minecraft:glow_squid"
	},
	"luminance": {
		"type": "lambdynlights:glow_squid"
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/item.json
```
{
	"match": {
		"type": "minecraft:item"
	},
	"luminance": {
		"type": "lambdynlights:item_entity"
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/item_display.json
```
{
	"match": {
		"type": "minecraft:item_display"
	},
	"luminance": {
		"type": "lambdynlights:display",
		"luminance": {
			"type": "lambdynlights:display/item"
		}
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/item_frame.json
```
{
	"match": {
		"type": [
			"minecraft:item_frame",
			"minecraft:glow_item_frame"
		]
	},
	"luminance": {
		"type": "lambdynlights:item_frame"
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/magma_cube.json
```
{
	"match": {
		"type": "minecraft:magma_cube"
	},
	"luminance": {
		"type": "lambdynlights:cube_mob_squish",
		"low": 8,
		"high": 11,
		"threshold": 0.6
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/small_fireball.json
```
{
	"match": {
		"type": "minecraft:small_fireball"
	},
	"luminance": {
		"type": "lambdynlights:water_sensitive",
		"out_of_water": 12
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/spectral_arrow.json
```
{
	"match": {
		"type": "minecraft:spectral_arrow"
	},
	"luminance": {
		"type": "lambdynlights:arrow/derived_from_self_item"
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/throwable_item_projectiles.json
```
{
	"match": {
		"type": [
			"minecraft:egg",
			"minecraft:ender_pearl",
			"minecraft:experience_bottle",
			"minecraft:snowball",
			"minecraft:splash_potion"
		]
	},
	"luminance": {
		"type": "lambdynlights:projectile/throwable_item"
	}
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/entity/wither_skull.json
```
{
	"match": {
		"type": "minecraft:wither_skull"
	},
	"luminance": 12
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/blaze.json
```
{
	"match": {
		"items": [
			"minecraft:blaze_powder",
			"minecraft:blaze_rod"
		]
	},
	"luminance": 10
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/campfire.json
```
{
	"match": {
		"items": [
			"minecraft:campfire",
			"minecraft:soul_campfire"
		]
	},
	"luminance": {
		"type": "block_self"
	},
	"water_sensitive": true
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/fire_charge.json
```
{
	"match": {
		"items": "minecraft:fire_charge"
	},
	"luminance": 10,
	"water_sensitive": true
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/glow_berries.json
```
{
	"match": {
		"items": "minecraft:glow_berries"
	},
	"luminance": 14
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/glow_ink_sac.json
```
{
	"match": {
		"items": "minecraft:glow_ink_sac"
	},
	"luminance": 12
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/glowstone_dust.json
```
{
	"match": {
		"items": "minecraft:glowstone_dust"
	},
	"luminance": 10
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/lava_bucket.json
```
{
	"match": {
		"items": "minecraft:lava_bucket"
	},
	"luminance": {
		"type": "block",
		"block": "minecraft:lava"
	},
	"water_sensitive": true
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/nether_star.json
```
{
	"match": {
		"items": "minecraft:nether_star"
	},
	"luminance": 8
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/prismarine_crystals.json
```
{
	"match": {
		"items": "minecraft:prismarine_crystals"
	},
	"luminance": 10
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/spectral_arrow.json
```
{
	"match": {
		"items": "minecraft:spectral_arrow"
	},
	"luminance": 8
}

```
### lambdynamiclights-4.13.0+26.3.jar: assets/lambdynlights/dynamiclights/item/torch.json
```
{
	"match": {
		"items": [
			"minecraft:torch",
			"minecraft:redstone_torch",
			"minecraft:soul_torch",
			"minecraft:copper_torch"
		]
	},
	"luminance": {
		"type": "block_self"
	},
	"water_sensitive": true
}

```
### lambdynamiclights-4.13.0+26.3.jar: LICENSE_lambdynamiclights
```
The Lambda License

Copyright (c) 2024 LambdAurora <email@lambdaurora.dev>


# 0. Foreword

The Lambda License is an open license meant to better protect the rights of
software authors. It does not adhere to the usual definition of "Free Open
Source Software" (FOSS), but rather we aim to strike a middle ground between
the freedom of consumers and the protection of the authors' work.

We believe traditional FOSS licenses allow companies or other individuals to
exploit software authors freely by having their work shared in a way they
don't agree with or that is detrimental to their business. This license aims
to give control of artifacts back to the authors and support an open code
community, while still allowing source code to be easily modified and shared.

In short, you may share or modify the source code as you wish, as long as the
work stays under this license, and if you have modified the code, you must:
(a) State your changes if you don't use an SVC like Git
(b) List the original authors (as listed in the copyright at the top of this
license)

When sharing binaries, you must adhere to a few more rules. In particular, you
will need written approval from the original authors. If they don't answer you
after 60 days, they are considered to be implicitly agreeing. Please note that
you have to request in good faith, spam won't be accepted. You will also need
to make the source code available and document where to find it.

If you are running a modified version of this software
```
### lambdynamiclights-4.13.0+26.3.jar: META-INF/jars/lambdynamiclights-api-4.13.0+26.3.jar
```
PK    A             	   META-INF/ PK           PK    !                META-INF/MANIFEST.MF����0Ew��hC�@d5�"Ƹ?�a^��M�^\��8�{ONL-�^\�y2\�J&�� 7G���Z�8�Fo��"6mK5A7oJM��+��p4F�n��T-~���hK�b?�ȟ�_���b��An����/�/qF=e�%j��M�~�y�O�\:3� F�7PK;�Eء   K  PK    A                dev/ PK           PK    A                dev/lambdaurora/ PK           PK    A                dev/lambdaurora/lambdynlights/ PK           PK    A             "   dev/lambdaurora/lambdynlights/api/ PK           PK    A             <   dev/lambdaurora/lambdynlights/api/DynamicLightsContext.class;�o�>W^v&F���2���ܤ��Ң��D�2/'3=��X?� Sߥ2/173�,✟W�ZQ����� ��XҐ����\�� �Y��V�_Z��ꛘ���Z��ਡ�C�*�n}O�FX32H��d�Tb3ޕ(�!��]q�B:ɳN��e��Ep[���%	�%���Z���-3'��A[ ���������a���� 
{v0����ad��L< PK��e�   �  PK    A             @   dev/lambdaurora/lambdynlights/api/DynamicLightsInitializer.class�P�NA}��!
!v��x=W�/`��P��؜C�=����(� >ʰ�Ņjfޛɛ�v�� 1�j��\�C��ٜ�[��7FK��V!J��1��4)��'��[� �΂ׇ#����B��Ў����m���_�)�&��FΊ�"B�)7+��MX��K��vW*)�妒>���''���N}��aBh��¦�E�"��g��`�N ����?�@���O54@>�&�J��Z{PK����   j  PK    A             4   dev/lambdaurora/lambdynlights/api/package-info.classM�=n�@�g�*� � ���J�T��@����gֻ�zm�W��r(���L13�7�w:�x�S�Ha�s��iN�w�n}�Fʟ�jjD7�����%�*�+�/C[���,(�v����a�����e���l����Jj��Z(�������RWmÙ��;�_�1���o��7Ex�z�d�x$��PK�<���   �   PK    A             +   dev/lambdauro
```

## Result
- title screen: reached
- world tour: done (13 pictures)

## Graphics
[23Oct2026 15:32:57.960] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4
[23Oct2026 15:33:50.877] [Render thread/INFO] [net.minecraft.client.Minecraft/]: Using graphics backend OpenGL, using drivers: 4.6 (Core Profile) Mesa 25.2.8-0ubuntu0.24.04.4

## Errors and warnings in the logs (Umbra, mixins, crashes)
3:[23Oct2026 15:32:50.629] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
12:[23Oct2026 15:32:50.791] [ForkJoinPool.commonPool-worker-3/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
24:		Umbra 2.12.1 (umbra)
35: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
41: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
47:[23Oct2026 15:32:56.446] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
48:[23Oct2026 15:32:56.447] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
49:[23Oct2026 15:32:56.447] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
108:[23Oct2026 15:32:58.559] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/lambdynlights, mod/yumi_mc_core, mod/pride, mod/lambdynlights_api, mod/umbra, mod/neoforge, mod/spruceui
132:[23Oct2026 15:33:01.985] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
139:[23Oct2026 15:33:43.950] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Game directory: /home/runner/work/umbra-mod/umbra-mod/run-client
148:[23Oct2026 15:33:44.109] [ForkJoinPool.commonPool-worker-2/INFO] [net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator/CORE]: Got mod coordinates umbra%%/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main:umbra%%/home/runner/work/umbra-mod/umbra-mod/build/resources/main from env
160:		Umbra 2.12.1 (umbra)
171: - minecraft (composite(filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar)), filtered(jar(/home/runner/work/umbra-mod/umbra-mod/build/moddev/artifacts/minecraft-patched-26.3.0.76.jar))))
177: - umbra (composite(folder(/home/runner/work/umbra-mod/umbra-mod/build/classes/java/main), folder(/home/runner/work/umbra-mod/umbra-mod/build/resources/main)))
183:[23Oct2026 15:33:49.465] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.creative_flight
184:[23Oct2026 15:33:49.465] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.gliding_flight
185:[23Oct2026 15:33:49.465] [main/ERROR] [net.minecraft.server.Bootstrap/]: Missing translations: neoforge.swim_speed
243:[23Oct2026 15:33:51.519] [Render thread/INFO] [net.minecraft.server.packs.resources.ReloadableResourceManager/]: Reloading ResourceManager: vanilla, vanilla, mod_resources, mod/lambdynlights, mod/yumi_mc_core, mod/pride, mod/lambdynlights_api, mod/umbra, mod/neoforge, mod/spruceui
267:[23Oct2026 15:33:54.396] [Render thread/INFO] [dev.umbra.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
275:[23Oct2026 15:33:56.050] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
392:[23Oct2026 15:35:51.994] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside

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
[15:35:51] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 482/10462 (4.61%) and 131 parsed for mixin
[15:35:51] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:35:51] [Server thread/INFO] [minecraft/MinecraftServer]: Stopping server
[15:35:51] [Server thread/INFO] [minecraft/MinecraftServer]: Saving players
[15:35:51] [Server thread/INFO] [minecraft/ServerGamePacketListenerImpl]: Dev lost connection: Disconnected
[15:35:51] [Server thread/INFO] [minecraft/MinecraftServer]: System chat: Dev left the game
[15:35:51] [Server thread/INFO] [minecraft/ServerCommonPacketListenerImpl]: Stopping singleplayer server as player logged out
[15:35:51] [Server thread/INFO] [minecraft/MinecraftServer]: Saving worlds
[15:35:51] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:overworld
[15:35:51] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/umbra:underside
[15:35:52] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:the_end
[15:35:52] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[shots]'/minecraft:the_nether
[15:35:52] [Server thread/DEBUG] [ne.ne.ne.co.CommonHooks/WP]: Gathered mod list to write to world save shots
[15:35:52] [Server thread/DEBUG] [ne.ne.fm.co.ConfigTracker/CONFIG]: Unloading configs type SYNCED
[15:35:52] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 1bd107f3
[15:35:52] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_5-1792769623200.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 2m 9s
7 actionable tasks: 2 executed, 5 up-to-date
Configuration cache entry stored.
client exited with 0
[15:33:01] [Render thread/INFO] [minecraft/TextureAtlas]: Created: 512x512x0 minecraft:textures/atlas/chest.png-atlas
[15:33:01] [Render thread/INFO] [minecraft/TextureAtlas]: Created: 512x512x0 minecraft:textures/atlas/shulker_boxes.png-atlas
[15:33:01] [Render thread/INFO] [ne.ne.ne.cl.en.an.js.AnimationLoader/]: Loaded 0 entity animations
[15:33:01] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 451/8610 (5.24%) and 131 parsed for mixin
[15:33:01] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mods loaded with 4 warning(s)
[15:33:01] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod lambdynlights uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[15:33:01] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod pride uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[15:33:01] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod lambdynlights_api uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[15:33:01] [Render thread/WARN] [ne.ne.ne.cl.lo.ClientModLoader/LOADING]: Mod spruceui uses the deprecated `logoFile` property; change to `bannerFile` and/or (for square icons) `iconFile`
[15:33:01] [Render thread/INFO] [de.um.Umbra/]: UMBRA CI: the mod loading warnings screen is up; going on past it
[15:33:01] [Render thread/DEBUG] [io.ne.ut.in.ThreadLocalRandom/]: -Dio.netty.initialSeedUniquifier: 0xb394a28173e0fd83
[15:33:11] [Render thread/INFO] [de.um.Umbra/]: UMBRA CLIENTCHECK: done
[15:33:11] [Render thread/DEBUG] [ne.ne.fm.cl.tr.ClassTransformStatistics/]: Transformed/total loaded classes: 453/8688 (5.21%) and 131 parsed for mixin
[15:33:11] [Render thread/INFO] [minecraft/Minecraft]: Stopping!
[15:33:12] [Render thread/INFO] [ne.ne.fm.lo.FMLLoader/]: Closing FML Loader 20531934
[15:33:12] [Render thread/INFO] [ne.ne.fm.ModLoader/]: Clearing ModLoader
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1792769564560.json

[Incubating] Problems report is available at: file:///home/runner/work/umbra-mod/umbra-mod/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 30s
7 actionable tasks: 6 executed, 1 from cache
Configuration cache entry stored.
client exited with 0
