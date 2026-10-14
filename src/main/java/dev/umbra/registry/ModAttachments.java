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
                    .sync(ByteBufCodecs.FLOAT)
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
                    .copyOnDeath()
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

    private ModAttachments() {}
}
