package dev.umbra.registry;

import com.mojang.serialization.Codec;
import dev.umbra.Umbra;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.network.codec.ByteBufCodecs;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Umbra.MODID);

    /** Packed BlockPos of the player's home (last bed spawn), Long.MIN_VALUE = not set. */
    public static final Supplier<AttachmentType<Long>> HOME = ATTACHMENTS.register("home",
            () -> AttachmentType.builder(() -> Long.MIN_VALUE)
                    .serialize(Codec.LONG.fieldOf("pos"))
                    .copyOnDeath()
                    .build());

    /** Sanity 0..100, synced to the client for the HUD bar. Resets to full after death. */
    public static final Supplier<AttachmentType<Float>> SANITY = ATTACHMENTS.register("sanity",
            () -> AttachmentType.builder(() -> 100.0F)
                    .serialize(Codec.FLOAT.fieldOf("value"))
                    .sync(ModAttachments::ownerOnly, ByteBufCodecs.FLOAT)
                    .build());

    /** Set once the Primordial Shadow is defeated: your shadow fights for you from then on. */
    public static final Supplier<AttachmentType<Boolean>> FREED = ATTACHMENTS.register("freed",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("value"))
                    .copyOnDeath()
                    .build());

    /** Journal entries the player has unlocked, in order. */
    public static final Supplier<AttachmentType<java.util.List<String>>> JOURNAL = ATTACHMENTS.register("journal",
            () -> AttachmentType.<java.util.List<String>>builder(() -> java.util.List.of())
                    .serialize(Codec.STRING.listOf().fieldOf("entries"))
                    .sync(ModAttachments::ownerOnly, net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(net.minecraft.network.codec.ByteBufCodecs.list()))
                    .copyOnDeath()
                    .build());

    /** /umbra storm: a storm for this player regardless of the clock (synced, so their screen goes grey too). */
    public static final Supplier<AttachmentType<Boolean>> STORM_TEST = ATTACHMENTS.register("storm_test",
            () -> AttachmentType.builder(() -> false)
                    .sync(ModAttachments::ownerOnly, net.minecraft.network.codec.ByteBufCodecs.BOOL)
                    .build());

    /** Whether the player already received the journal. */
    public static final Supplier<AttachmentType<Boolean>> GOT_JOURNAL = ATTACHMENTS.register("got_journal",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("value"))
                    .copyOnDeath()
                    .build());

    /** While in a nightmare: packed overworld BlockPos to wake up at. Long.MIN_VALUE = not dreaming. */
    public static final Supplier<AttachmentType<Long>> NIGHTMARE = ATTACHMENTS.register("nightmare",
            () -> AttachmentType.builder(() -> Long.MIN_VALUE)
                    .serialize(Codec.LONG.fieldOf("return"))
                    .build());

    /** Where the player came into the citadel (gate position), to send them back there. */
    public static final Supplier<AttachmentType<Long>> CITADEL_RETURN = ATTACHMENTS.register("citadel_return",
            () -> AttachmentType.builder(() -> Long.MIN_VALUE)
                    .serialize(Codec.LONG.fieldOf("gate"))
                    .build());

    /** On a village cell's chosen chunk: packed bell position once built; MIN = not built, MAX = no village here. */
    public static final Supplier<AttachmentType<Long>> VILLAGE = ATTACHMENTS.register("village",
            () -> AttachmentType.builder(() -> Long.MIN_VALUE)
                    .serialize(Codec.LONG.fieldOf("bell"))
                    .build());

    /** Kills per creature, as "id:count" (the bestiary opens up as you learn a creature). */
    public static final Supplier<AttachmentType<java.util.List<String>>> BESTIARY = ATTACHMENTS.register("bestiary",
            () -> AttachmentType.<java.util.List<String>>builder(() -> java.util.List.of())
                    .serialize(Codec.STRING.listOf().fieldOf("kills"))
                    .sync(ModAttachments::ownerOnly, net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(net.minecraft.network.codec.ByteBufCodecs.list()))
                    .copyOnDeath()
                    .build());

    /** Accepted errands, as "id@kills at the time". */
    public static final Supplier<AttachmentType<java.util.List<String>>> QUESTS_ACTIVE = ATTACHMENTS.register("quests_active",
            () -> AttachmentType.<java.util.List<String>>builder(() -> java.util.List.of())
                    .serialize(Codec.STRING.listOf().fieldOf("quests"))
                    .sync(ModAttachments::ownerOnly, net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(net.minecraft.network.codec.ByteBufCodecs.list()))
                    .copyOnDeath()
                    .build());

    /** Finished errands. */
    public static final Supplier<AttachmentType<java.util.List<String>>> QUESTS_DONE = ATTACHMENTS.register("quests_done",
            () -> AttachmentType.<java.util.List<String>>builder(() -> java.util.List.of())
                    .serialize(Codec.STRING.listOf().fieldOf("quests"))
                    .sync(ModAttachments::ownerOnly, net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(net.minecraft.network.codec.ByteBufCodecs.list()))
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}

    /** On a server, a player's mind (sanity, journal, errands) is sent to that player alone, not to everyone nearby. */
    public static boolean ownerOnly(net.neoforged.neoforge.attachment.IAttachmentHolder holder, net.minecraft.server.level.ServerPlayer to) {
        return holder == to;
    }
}
