package net.zoogle.enchiridion.client.render;

import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.Enchiridion;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

final class EnchiridionBookGeoModel extends GeoModel<EnchiridionBookAnimatable> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Enchiridion.MODID, "geo/enchiridion.geo.json");
    static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Enchiridion.MODID, "textures/gui/enchiridion.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Enchiridion.MODID, "animations/enchiridion.animation.json");

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
        applyCoverStatePageVisibility(animatable);
    }

    private void applyCoverStatePageVisibility(EnchiridionBookAnimatable animatable) {
        boolean frontCoverState = switch (animatable.animState()) {
            case IDLE_FRONT -> true;
            default -> false;
        };
        boolean backCoverState = switch (animatable.animState()) {
            case IDLE_BACK -> true;
            default -> false;
        };

        setHidden("page_1", frontCoverState);
        setHidden("page_2", frontCoverState);
        setHidden("page_3", frontCoverState);
        setHidden("page_4", frontCoverState);
        for (int index = 1; index <= 6; index++) {
            setHidden("a_page_5_" + index, frontCoverState);
        }

        for (int index = 1; index <= 6; index++) {
            setHidden("a_page_6_" + index, backCoverState);
            setHidden("a_page_7_" + index, backCoverState);
        }
        setHidden("page_8", backCoverState);
        setHidden("page_9", backCoverState);
        setHidden("page_10", backCoverState);
        setHidden("page_11", backCoverState);
    }

    private void setHidden(String boneName, boolean hidden) {
        GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            bone.setHidden(hidden);
        }
    }
}
