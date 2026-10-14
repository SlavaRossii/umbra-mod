package dev.umbra.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A model baked from {@link GeneratedModels}; its animation is a function of the render state
 * (see {@link Animations}). Every frame starts from the rest pose.
 */
public class UmbraModel extends EntityModel<UmbraRenderState> {
    private final Function<String, ModelPart> parts;
    private final BiConsumer<Function<String, ModelPart>, UmbraRenderState> animator;

    public UmbraModel(ModelPart root, BiConsumer<Function<String, ModelPart>, UmbraRenderState> animator) {
        super(root);
        this.parts = root.createPartLookup();
        this.animator = animator;
    }

    @Override
    public void setupAnim(UmbraRenderState state) {
        super.setupAnim(state);
        this.resetPose();
        animator.accept(parts, state);
    }
}
