package dev.umbra.light;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.TorchBlock;

/** A luminite torch: living darkness cannot put it out. */
public class EternalTorchBlock extends TorchBlock implements DarknessProof {
    public EternalTorchBlock(SimpleParticleType particle, Properties properties) {
        super(particle, properties);
    }
}
