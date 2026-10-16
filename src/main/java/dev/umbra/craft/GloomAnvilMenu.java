package dev.umbra.craft;

import dev.umbra.gear.Gear;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModComponents;
import dev.umbra.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The gloom anvil's window: a piece of gear on the left, its own stuff (or an artifact) on the right, the result on
 * the far right. Each piece of stuff mends a quarter of the wear; an artifact is pinned onto a chestplate. No
 * experience is ever asked.
 */
public class GloomAnvilMenu extends ItemCombinerMenu {
    public static final int GEAR = 0, STUFF = 1, RESULT = 2;
    private int cost;

    public GloomAnvilMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public GloomAnvilMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(dev.umbra.registry.ModMenus.GLOOM_ANVIL.get(), id, inventory, access, ItemCombinerMenuSlotDefinition.create()
                .withSlot(GEAR, 27, 47, s -> true)
                .withSlot(STUFF, 76, 47, s -> true)
                .withResultSlot(RESULT, 134, 47)
                .build());
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(ModBlocks.GLOOM_ANVIL.get());
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasItem) {
        return cost > 0;
    }

    /** What the anvil would make of these two, and how much of the right-hand stack it takes. */
    public static ItemStack result(ItemStack gear, ItemStack stuff, int[] costOut) {
        costOut[0] = 0;
        if (gear.isEmpty() || stuff.isEmpty()) return ItemStack.EMPTY;
        String artifact = stuff.is(ModItems.ASH_CLASP.get()) ? Gear.ASH_CLASP : stuff.is(ModItems.ARCHITECT_SEAL.get()) ? Gear.ARCHITECT_SEAL : null;
        if (artifact != null) {
            if (!gear.is(ItemTags.CHEST_ARMOR) || artifact.equals(gear.get(ModComponents.ARTIFACT.get()))) return ItemStack.EMPTY;
            ItemStack out = gear.copyWithCount(1);
            out.set(ModComponents.ARTIFACT.get(), artifact);
            costOut[0] = 1;
            return out;
        }
        if (!gear.isDamageableItem() || !gear.isDamaged()) return ItemStack.EMPTY;
        Item mat = GloomAnvilBlock.materialFor(gear);
        if (mat == null || !(stuff.is(mat) || stuff.is(ModItems.ECHO_QUARTZ.get()))) return ItemStack.EMPTY;
        int per = Math.max(1, Math.round(gear.getMaxDamage() * GloomAnvilBlock.MEND));
        int need = (gear.getDamageValue() + per - 1) / per;
        int use = Math.min(need, stuff.getCount());
        ItemStack out = gear.copyWithCount(1);
        out.setDamageValue(Math.max(0, gear.getDamageValue() - use * per));
        costOut[0] = use;
        return out;
    }

    @Override
    public void createResult() {
        int[] c = new int[1];
        ItemStack out = result(this.inputSlots.getItem(GEAR), this.inputSlots.getItem(STUFF), c);
        cost = c[0];
        this.resultSlots.setItem(0, out);
        this.broadcastChanges();
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        this.inputSlots.setItem(GEAR, ItemStack.EMPTY);
        ItemStack stuff = this.inputSlots.getItem(STUFF);
        stuff.shrink(cost);
        this.inputSlots.setItem(STUFF, stuff);
        cost = 0;
        this.access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.9F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 0.5F);
        });
    }
}
