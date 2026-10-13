package com.dailytrials;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.util.Identifier;

/** Renderizador de la Sombra: semitransparente, con ojos morados que brillan en la oscuridad. */
public class SombraRenderer extends MobEntityRenderer<SombraEntity, SombraModel> {
    private static final Identifier TEXTURE = ModContent.id("textures/entity/sombra.png");

    public SombraRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SombraModel(ctx.getPart(SombraModel.LAYER)), 0.4f);
        this.addFeature(new Ojos(this));
    }

    @Override
    public Identifier getTexture(SombraEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderLayer getRenderLayer(SombraEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
        if (showOutline) return RenderLayer.getOutline(TEXTURE);
        return RenderLayer.getEntityTranslucent(TEXTURE);
    }

    /** Ojos que brillan (textura sombra_eyes.png). */
    private static class Ojos extends EyesFeatureRenderer<SombraEntity, SombraModel> {
        private static final RenderLayer EYES = RenderLayer.getEyes(ModContent.id("textures/entity/sombra_eyes.png"));

        public Ojos(FeatureRendererContext<SombraEntity, SombraModel> context) {
            super(context);
        }

        @Override
        public RenderLayer getEyesTexture() {
            return EYES;
        }
    }
}
