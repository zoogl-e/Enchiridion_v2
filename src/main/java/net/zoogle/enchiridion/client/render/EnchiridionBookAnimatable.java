package net.zoogle.enchiridion.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
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
    private static final RawAnimation ARRIVAL_ANIMATION = rawAnimationFor(BookAnimState.ARRIVING);
    private static final RawAnimation OPENING_ANIMATION = rawAnimationFor(BookAnimState.OPENING);
    private static final RawAnimation CLOSING_ANIMATION = rawAnimationFor(BookAnimState.CLOSING);
    private static final RawAnimation IDLE_OPEN_ANIMATION = rawAnimationFor(BookAnimState.IDLE_OPEN);
    private static final RawAnimation FLIP_NEXT_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_NEXT);
    private static final RawAnimation FLIP_PREV_ANIMATION = rawAnimationFor(BookAnimState.FLIPPING_PREV);
    private static final RawAnimation RIFFLE_NEXT_ANIMATION = rawAnimationFor(BookAnimState.RIFFLING_NEXT);
    private static final RawAnimation RIFFLE_PREV_ANIMATION = rawAnimationFor(BookAnimState.RIFFLING_PREV);
    private static final RawAnimation IDLE_CLOSED_ANIMATION = rawAnimationFor(BookAnimState.CLOSED);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private BookAnimState animState = BookAnimState.CLOSED;
    private BookAnimState lastAppliedState = null;
    private ResourceLocation textureLocation = EnchiridionBookGeoModel.DEFAULT_TEXTURE;
    private ResourceLocation magicLeftTextureLocation = EnchiridionBookGeoModel.DEFAULT_TEXTURE;
    private ResourceLocation magicRightTextureLocation = EnchiridionBookGeoModel.DEFAULT_TEXTURE;

    public void setAnimState(BookAnimState animState) {
        this.animState = animState;
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "book_screen_controller", 0, state -> {
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
            case OPENING -> OPENING_ANIMATION;
            case IDLE_OPEN -> IDLE_OPEN_ANIMATION;
            case FLIPPING_NEXT -> FLIP_PREV_ANIMATION;
            case FLIPPING_PREV -> FLIP_NEXT_ANIMATION;
            case RIFFLING_NEXT -> RIFFLE_PREV_ANIMATION;
            case RIFFLING_PREV -> RIFFLE_NEXT_ANIMATION;
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
}
