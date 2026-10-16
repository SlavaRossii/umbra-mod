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
 * The gloom anvil: Umbra's own smithy. Strike it with worn Umbra gear and it mends a quarter of the wear for one piece
 * of the gear's own stuff from your pack (luminite, shadow shards, gloom ingots; echo quartz mends anything of
 * Umbra's), with no experience spent. Hold a chestplate and an artifact in the other hand to pin the artifact on.
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

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        // pinning an artifact
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        String artifact = other.is(ModItems.ASH_CLASP.get()) ? Gear.ASH_CLASP : other.is(ModItems.ARCHITECT_SEAL.get()) ? Gear.ARCHITECT_SEAL : null;
        if (artifact != null && stack.is(ItemTags.CHEST_ARMOR)) {
            stack.set(ModComponents.ARTIFACT.get(), artifact);
            if (!player.isCreative()) other.shrink(1);
            strike(server, pos, 1.2F);
            player.sendOverlayMessage(Component.translatable("block.umbra.gloom_anvil.pinned", Component.translatable("artifact.umbra." + artifact)).withStyle(ChatFormatting.GOLD));
            return InteractionResult.SUCCESS;
        }
        // mending
        if (!stack.isDamageableItem() || !stack.isDamaged()) {
            player.sendOverlayMessage(Component.translatable("block.umbra.gloom_anvil.whole").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        Item mat = materialFor(stack);
        if (mat == null) {
            player.sendOverlayMessage(Component.translatable("block.umbra.gloom_anvil.foreign").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (!player.isCreative()) {
            if (!take(player, mat) && (mat == ModItems.ECHO_QUARTZ.get() || !take(player, ModItems.ECHO_QUARTZ.get()))) {
                player.sendOverlayMessage(Component.translatable("block.umbra.gloom_anvil.need", new ItemStack(mat).getHoverName()).withStyle(ChatFormatting.DARK_PURPLE));
                return InteractionResult.FAIL;
            }
        }
        int mend = Math.max(1, Math.round(stack.getMaxDamage() * MEND));
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - mend));
        strike(server, pos, 0.9F);
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
