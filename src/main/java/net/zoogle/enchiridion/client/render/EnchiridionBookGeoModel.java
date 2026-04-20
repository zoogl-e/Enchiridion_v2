package net.zoogle.enchiridion.client.render;

import net.minecraft.resources.ResourceLocation;
import net.zoogle.enchiridion.Enchiridion;
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
}
