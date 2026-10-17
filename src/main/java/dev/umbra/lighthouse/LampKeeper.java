package dev.umbra.lighthouse;

import dev.umbra.journal.Journal;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Yan, the keeper of the lighthouse: the one who vanished at the well. He cannot be hurt and does not trade; he tells
 * what he knows and reacts to how brightly the lamp burns.
 */
public class LampKeeper extends PathfinderMob {
    public LampKeeper(EntityType<? extends LampKeeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    /** The lamp of the lighthouse: above the platform saved when it was raised (or, for one built by hand, at x, z). */
    public static BlockPos findLamp(ServerLevel level, double x, double z) {
        BlockPos built = LighthouseManager.built(level);
        if (built != null) {
            BlockPos lamp = built.above(LighthouseFeature.LAMP_Y);
            if (level.getBlockState(lamp).is(ModBlocks.LIGHTHOUSE_LAMP.get())) return lamp;
        }
        int lx = (int) Math.floor(x), lz = (int) Math.floor(z);
        for (int dz : new int[]{0, -LighthouseFeature.HOUSE_Z})            // on the axis, or seen from the keeper's door
            for (int y = 250; y > 5; y--) {
                BlockPos p = new BlockPos(lx, y, lz + dz);
                if (level.getBlockState(p).is(ModBlocks.LIGHTHOUSE_LAMP.get())) return p;
            }
        return null;
    }

    public static int lampLevel(ServerLevel level, double x, double z) {
        BlockPos p = findLamp(level, x, z);
        return p == null ? 0 : level.getBlockState(p).getValue(LighthouseLampBlock.LEVEL);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp) || !(this.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        int lamp = lampLevel(level, getX(), getZ());
        this.playSound(dev.umbra.registry.ModSounds.YAN.value(), 1.0F, getVoicePitch());
        // 2.6: after an ending, crouch and speak to him to turn the circle (New Game+)
        if (dev.umbra.ending.Endings.chosen(sp) && sp.isShiftKeyDown()) {
            dev.umbra.ending.Cycle.begin(sp);
            sp.sendSystemMessage(Component.translatable("dialogue.umbra.yan.cycle").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return InteractionResult.SUCCESS;
        }
        // 2.6: once the lamp burns, Yan has errands (the lamp's own reward comes first)
        if (lamp >= 1 && Journal.has(sp, "yan_1") && !dev.umbra.ending.Endings.chosen(sp)
                && !(lamp >= LighthouseLampBlock.MAX_LEVEL && !Journal.has(sp, "yan_4")) && YanErrands.talk(sp)) return InteractionResult.SUCCESS;
        String line;
        if (dev.umbra.ending.Endings.isLight(sp)) {
            line = "dialogue.umbra.yan.dawn";
            Journal.unlock(sp, "yan_dawn");
        } else if (dev.umbra.ending.Endings.isDark(sp)) {
            line = "dialogue.umbra.yan.dark";
        } else if (!Journal.has(sp, "yan_1")) {
            Journal.unlock(sp, "yan_1");
            line = "dialogue.umbra.yan.intro";
        } else if (lamp <= 0) {
            line = "dialogue.umbra.yan.dead";
        } else if (lamp >= LighthouseLampBlock.MAX_LEVEL) {
            line = "dialogue.umbra.yan.max";
            Journal.unlock(sp, "yan_3");
            if (!Journal.has(sp, "yan_4")) {
                Journal.unlock(sp, "yan_4");
                reward(level, sp);
            }
        } else {
            line = "dialogue.umbra.yan.level" + lamp;
        }
        sp.sendSystemMessage(Component.translatable(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (dev.umbra.ending.Endings.chosen(sp)) sp.sendSystemMessage(Component.translatable("dialogue.umbra.yan.offer").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        return InteractionResult.SUCCESS;
    }

    private void reward(ServerLevel level, ServerPlayer player) {
        for (ItemStack s : new ItemStack[]{new ItemStack(ModItems.LUMINITE_CRYSTAL.get(), 6), new ItemStack(ModItems.SHADOW_SHARD.get(), 4),
                new ItemStack(ModItems.KEEPER_PAGE.get(), 1)}) {
            level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1.0, getZ(), s));
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // Yan's own voice: an old man's mumble, a cough, the clink of his lantern now and then
        return this.random.nextInt(5) == 0 ? dev.umbra.registry.ModSounds.LANTERN_CLINK.value() : dev.umbra.registry.ModSounds.YAN.value();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return dev.umbra.registry.ModSounds.YAN_HURT.value();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public float getVoicePitch() {
        return 0.95F + this.random.nextFloat() * 0.1F;
    }
}
