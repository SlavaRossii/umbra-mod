package dev.umbra.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A pale glowing mushroom of the abyss. Purely decorative, gives off a little light. */
public class GloomMushroomBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 9, 12);

    public GloomMushroomBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
