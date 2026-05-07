package net.zoogle.enchiridion.client.render;

import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.Enchiridion;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

final class EnchiridionBookGeoModel extends GeoModel<EnchiridionBookAnimatable> {
    private static final float COVER_HIDE_SETTLE_PROGRESS = 0.96f;
    private static final float COVER_HIDE_OPENING_SETTLE_PROGRESS = 0.92f;
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Enchiridion.MODID, "geo/enchiridion.geo.json");
    static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Enchiridion.MODID, "textures/gui/enchiridion.png");
    static final ResourceLocation BLANK_FRONT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Enchiridion.MODID, "textures/gui/enchiridion_blank.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Enchiridion.MODID, "animations/enchiridion.animation.json");
    private static final String BOOKMARK_BONE = "bookmark";
    private static final float BOOKMARK_HIDE_ALPHA_THRESHOLD = 0.05f;
    private static final float BOOKMARK_TUCK_DOWN = -2.3f;
    private static final float BOOKMARK_TUCK_BACK = -0.85f;
    private static final float BOOKMARK_OPEN_POP_UP = 2.0f;
    private static final float BOOKMARK_HOVER_LIFT = 0.85f;
    private static final float BOOKMARK_CLICK_DIP = 0.7f;
    private static final float BOOKMARK_TUCK_ROT_X = 0.32f;

    @Override
    public ResourceLocation getModelResource(EnchiridionBookAnimatable animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(EnchiridionBookAnimatable animatable) {
        return animatable.textureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(EnchiridionBookAnimatable animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(EnchiridionBookAnimatable animatable, long instanceId, AnimationState<EnchiridionBookAnimatable> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        GeoBone archetypeCard = getAnimationProcessor().getBone("archetype_card");
        if (archetypeCard != null) {
            archetypeCard.setHidden(!animatable.frontCoverCardState().visible());
        }
        applyBookmarkPose(animatable);
        applyCoverStatePageVisibility(animatable);
    }

    private void applyBookmarkPose(EnchiridionBookAnimatable animatable) {
        GeoBone bookmark = getAnimationProcessor().getBone(BOOKMARK_BONE);
        if (bookmark == null) {
            return;
        }
        float alpha = animatable.bookmarkAlpha();
        boolean hidden = alpha <= BOOKMARK_HIDE_ALPHA_THRESHOLD;
        bookmark.setHidden(hidden);
        if (hidden) {
            return;
        }

        float tuck = animatable.bookmarkTuckAmount();
        float pop = animatable.bookmarkPopAmount();
        float hover = animatable.bookmarkHoverAmount();
        float click = animatable.bookmarkClickAmount();

        float offsetY = (BOOKMARK_TUCK_DOWN * tuck)
                + (BOOKMARK_OPEN_POP_UP * pop)
                + (BOOKMARK_HOVER_LIFT * hover)
                - (BOOKMARK_CLICK_DIP * click);
        float offsetZ = BOOKMARK_TUCK_BACK * tuck;
        bookmark.setPosY(offsetY);
        bookmark.setPosZ(offsetZ);
        bookmark.setRotX(BOOKMARK_TUCK_ROT_X * tuck);
    }

    private void applyCoverStatePageVisibility(EnchiridionBookAnimatable animatable) {
        boolean settledBackFlip = animatable.animState() == net.zoogle.enchiridion.client.anim.BookAnimState.FLIPPING_BACK
                && animatable.animProgress() >= COVER_HIDE_SETTLE_PROGRESS;
        boolean settledBackOpening = animatable.animState() == net.zoogle.enchiridion.client.anim.BookAnimState.OPENING_BACK
                && animatable.animProgress() >= COVER_HIDE_OPENING_SETTLE_PROGRESS;
        boolean backCoverState = switch (animatable.animState()) {
            case IDLE_BACK -> true;
            default -> false;
        } || settledBackFlip || settledBackOpening;

        setHidden("a_page_5_1", false);
        for (int index = 2; index <= 6; index++) {
            setHidden("a_page_5_" + index, false);
        }

        setHidden("a_page_6_1", false);
        for (int index = 2; index <= 6; index++) {
            setHidden("a_page_6_" + index, backCoverState);
            setHidden("a_page_7_" + index, backCoverState);
        }
        setHidden("a_page_7_1", backCoverState);
        setHidden("page_8", backCoverState);
        setHidden("page_9", backCoverState);
        setHidden("page_10", backCoverState);
        setHidden("page_11", false);
    }

    private void setHidden(String boneName, boolean hidden) {
        GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            bone.setHidden(hidden);
        }
    }
}
