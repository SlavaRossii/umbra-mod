package dev.umbra.craft;

import dev.umbra.gear.Gear;
import dev.umbra.registry.ModComponents;
import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The gloom anvil: Umbra's own smithy, with a window like an anvil's (see {@link GloomAnvilMenu}). Worn Umbra gear
 * is mended by its own stuff (luminite, shadow shards, gloom ingots; echo quartz mends anything of Umbra's), a quarter
 * of the wear for each piece, with no experience spent; an artifact is pinned onto a chestplate.
 */
public class GloomAnvilBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape X = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(6, 4, 4, 10, 10, 12), Block.box(0, 10, 3, 16, 16, 13));
    private static final VoxelShape Z = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(4, 4, 6, 12, 10, 10), Block.box(3, 10, 0, 13, 16, 16));
    public static final float MEND = 0.25F;

    public GloomAnvilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? Z : X;
    }

    /** What mends this piece, or null if the anvil does not know it. */
    public static Item materialFor(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals(dev.umbra.Umbra.MODID)) return null;
        String path = id.getPath();
        if (path.startsWith("luminite_")) return ModItems.LUMINITE_CRYSTAL.get();
        if (path.startsWith("shadow_")) return ModItems.SHADOW_SHARD.get();
        if (path.startsWith("gloom_")) return ModItems.GLOOM_INGOT.get();
        return ModItems.ECHO_QUARTZ.get();
    }

    /** The anvil opens its window: gear on the left, its stuff (or an artifact) on the right. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new GloomAnvilMenu(id, inv, net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos)),
                    Component.translatable("block.umbra.gloom_anvil")));
        }
        return InteractionResult.SUCCESS;
    }

    private static void strike(ServerLevel level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, pitch);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 0.5F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 12, 0.3, 0.05, 0.3, 0.08);
    }

    private static boolean take(Player player, Item item) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }
}
