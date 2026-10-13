package dev.umbra.registry;

import com.mojang.serialization.Codec;
import dev.umbra.Umbra;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Umbra.MODID);

    /** Packed BlockPos of the player's home (last bed spawn), Long.MIN_VALUE = not set. */
    public static final Supplier<AttachmentType<Long>> HOME = ATTACHMENTS.register("home",
            () -> AttachmentType.builder(() -> Long.MIN_VALUE)
                    .serialize(Codec.LONG.fieldOf("pos"))
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
