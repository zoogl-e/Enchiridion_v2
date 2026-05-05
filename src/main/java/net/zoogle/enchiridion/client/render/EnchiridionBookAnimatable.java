package net.zoogle.enchiridion.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.api.BookFrontCoverCardState;
import net.zoogle.enchiridion.client.anim.BookAnimationSpec;
import net.zoogle.enchiridion.client.anim.BookAnimState;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

final class EnchiridionBookAnimatable implements GeoAnimatable {
    private static final int ANIMATION_TRANSITION_TICKS = 0;
    static final String MAGIC_TEXT_LEFT_BONE = "magic_text_left";
    static final String MAGIC_TEXT_RIGHT_BONE = "magic_text_right";
    static final String MAGIC_TEXT_FRONT_LEFT_BONE = "magic_text_front_left";

    private static final RawAnimation ARRIVAL_ANIMATION = rawAnimationFor(BookAnimState.ARRIVING);
    private static final RawAnimation OPENING_ANIMATION = rawAnimationFor(BookAnimState.OPENING);
    private static final RawAnimation OPENING_FRONT_ANIMATION = rawAnimationFor(BookAnimState.OPENING_FRONT);
    private static final RawAnimation OPENING_BACK_ANIMATION = rawAnimationFor(BookAnimState.OPENING_BACK);
    private static final RawAnimation CLOSING_ANIMATION = rawAnimationFor(BookAnimState.CLOSING);
    private static final RawAnimation CLOSING_FRONT_ANIMATION = rawAnimationFor(BookAnimState.CLOSING_FRONT);
    private static final RawAnimation CLOSING_BACK_ANIMATION = rawAnimationFor(BookAnimState.CLOSING_BACK);
    private static final RawAnimation IDLE_OPEN_ANIMATION = rawAnimationFor(BookAnimState.IDLE_OPEN);
    private static final RawAnimation IDLE_FRONT_ANIMATION = rawAnimationFor(BookAnimState.IDLE_FRONT);
    private static final RawAnimation IDLE_BACK_ANIMATION = rawAnimationFor(BookAnimState.IDLE_BACK);
    private static final RawAnimation IDLE_SKILLTREE_ANIMATION = rawAnimationFor(BookAnimState.IDLE_SKILLTREE);
    private static final RawAnimation IDLE_FRONT_SKILLTREE_ANIMATION = rawAnimationFor(BookAnimState.IDLE_FRONT_SKILLTREE);
    private static final RawAnimation FLIP_FRONT_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_FRONT);
    private static final RawAnimation FLIP_FRONT_TO_ORIGIN_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_FRONT_TO_ORIGIN);
    private static final RawAnimation FLIP_BACK_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_BACK);
    private static final RawAnimation FLIP_BACK_TO_ORIGIN_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_BACK_TO_ORIGIN);
    private static final RawAnimation FLIP_NEXT_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_NEXT);
    private static final RawAnimation FLIP_PREV_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_PREV);
    private static final RawAnimation FLIP_NEXT_SKILLTREE_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_NEXT_SKILLTREE);
    private static final RawAnimation FLIP_PREV_SKILLTREE_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_PREV_SKILLTREE);
    private static final RawAnimation RIFFLE_NEXT_ANIMATION = rawAnimationFor(BookAnimState.RIFFLING_NEXT);
    private static final RawAnimation RIFFLE_PREV_ANIMATION = rawAnimationFor(BookAnimState.RIFFLING_PREV);
    private static final RawAnimation RIFFLE_NEXT_SKILLTREE_ANIMATION = rawAnimationFor(BookAnimState.RIFFLING_NEXT_SKILLTREE);
    private static final RawAnimation RIFFLE_PREV_SKILLTREE_ANIMATION = rawAnimationFor(BookAnimState.RIFFLING_PREV_SKILLTREE);
    private static final RawAnimation IDLE_CLOSED_ANIMATION = rawAnimationFor(BookAnimState.CLOSED);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private BookAnimState animState = BookAnimState.CLOSED;
    private BookAnimState lastAppliedState = null;
    private float animProgress;
    private ResourceLocation textureLocation = EnchiridionBookGeoModel.DEFAULT_TEXTURE;
    private ResourceLocation magicLeftTextureLocation = EnchiridionBookGeoModel.DEFAULT_TEXTURE;
    private ResourceLocation magicRightTextureLocation = EnchiridionBookGeoModel.DEFAULT_TEXTURE;
    private BookFrontCoverCardState frontCoverCardState = BookFrontCoverCardState.hidden();
    private ReadableSurfaceTarget leftSurfaceTarget = ReadableSurfaceTarget.none();
    private ReadableSurfaceTarget rightSurfaceTarget = ReadableSurfaceTarget.none();

    public void setAnimState(BookAnimState animState) {
        this.animState = animState;
    }

    public BookAnimState animState() {
        return animState;
    }

    public void setAnimProgress(float animProgress) {
        this.animProgress = Math.clamp(animProgress, 0.0f, 1.0f);
    }

    public float animProgress() {
        return animProgress;
    }

    public void setTextureLocation(ResourceLocation textureLocation) {
        this.textureLocation = textureLocation;
    }

    public ResourceLocation textureLocation() {
        return textureLocation;
    }

    public void setMagicTextTextureLocations(ResourceLocation left, ResourceLocation right) {
        this.magicLeftTextureLocation = left;
        this.magicRightTextureLocation = right;
    }

    public ResourceLocation magicLeftTextureLocation() {
        return magicLeftTextureLocation;
    }

    public ResourceLocation magicRightTextureLocation() {
        return magicRightTextureLocation;
    }

    public void setFrontCoverCardState(BookFrontCoverCardState frontCoverCardState) {
        this.frontCoverCardState = frontCoverCardState == null ? BookFrontCoverCardState.hidden() : frontCoverCardState;
    }

    public BookFrontCoverCardState frontCoverCardState() {
        return frontCoverCardState;
    }

    public void setReadableSurfaceTargets(ReadableSurfaceTarget leftSurfaceTarget, ReadableSurfaceTarget rightSurfaceTarget) {
        this.leftSurfaceTarget = leftSurfaceTarget == null ? ReadableSurfaceTarget.none() : leftSurfaceTarget;
        this.rightSurfaceTarget = rightSurfaceTarget == null ? ReadableSurfaceTarget.none() : rightSurfaceTarget;
    }

    public ReadableSurfaceTarget leftSurfaceTarget() {
        return leftSurfaceTarget;
    }

    public ReadableSurfaceTarget rightSurfaceTarget() {
        return rightSurfaceTarget;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "book_screen_controller", ANIMATION_TRANSITION_TICKS, state -> {
            if (animState != lastAppliedState) {
                state.setAnimation(animationForState(animState));
                lastAppliedState = animState;
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.getGameTime();
        }
        return 0.0;
    }

    private static RawAnimation animationForState(BookAnimState state) {
        return switch (state) {
            case ARRIVING -> ARRIVAL_ANIMATION;
            case CLOSING -> CLOSING_ANIMATION;
            case CLOSING_FRONT -> CLOSING_FRONT_ANIMATION;
            case CLOSING_BACK -> CLOSING_BACK_ANIMATION;
            case OPENING -> OPENING_ANIMATION;
            case OPENING_FRONT -> OPENING_FRONT_ANIMATION;
            case OPENING_BACK -> OPENING_BACK_ANIMATION;
            case IDLE_OPEN -> IDLE_OPEN_ANIMATION;
            case IDLE_FRONT -> IDLE_FRONT_ANIMATION;
            case IDLE_BACK -> IDLE_BACK_ANIMATION;
            case IDLE_SKILLTREE -> IDLE_SKILLTREE_ANIMATION;
            case IDLE_FRONT_SKILLTREE -> IDLE_FRONT_SKILLTREE_ANIMATION;
            case FLIPPING_FRONT -> FLIP_FRONT_ANIMATION;
            case FLIPPING_FRONT_TO_ORIGIN -> FLIP_FRONT_TO_ORIGIN_ANIMATION;
            case FLIPPING_BACK -> FLIP_BACK_ANIMATION;
            case FLIPPING_BACK_TO_ORIGIN -> FLIP_BACK_TO_ORIGIN_ANIMATION;
            case FLIPPING_NEXT -> FLIP_NEXT_ANIMATION;
            case FLIPPING_PREV -> FLIP_PREV_ANIMATION;
            case FLIPPING_NEXT_SKILLTREE -> FLIP_NEXT_SKILLTREE_ANIMATION;
            case FLIPPING_PREV_SKILLTREE -> FLIP_PREV_SKILLTREE_ANIMATION;
            case RIFFLING_NEXT -> RIFFLE_NEXT_ANIMATION;
            case RIFFLING_PREV -> RIFFLE_PREV_ANIMATION;
            case RIFFLING_NEXT_SKILLTREE -> RIFFLE_NEXT_SKILLTREE_ANIMATION;
            case RIFFLING_PREV_SKILLTREE -> RIFFLE_PREV_SKILLTREE_ANIMATION;
            case CLOSED -> IDLE_CLOSED_ANIMATION;
        };
    }

    private static RawAnimation rawAnimationFor(BookAnimState state) {
        BookAnimationSpec.Clip clip = BookAnimationSpec.clipFor(state);
        return switch (clip.playbackMode()) {
            case LOOP -> RawAnimation.begin().thenLoop(clip.geckoName());
            case HOLD_ON_LAST_FRAME -> RawAnimation.begin().thenPlayAndHold(clip.geckoName());
        };
    }

    record ReadableSurfaceTarget(String boneName, boolean renderTexture) {
        static ReadableSurfaceTarget none() {
            return new ReadableSurfaceTarget("", false);
        }

        boolean active() {
            return boneName != null && !boneName.isBlank();
        }
    }
}
