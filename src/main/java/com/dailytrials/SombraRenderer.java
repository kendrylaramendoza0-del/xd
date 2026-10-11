package com.dailytrials;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

/** Usa la skin propia: assets/dailytrials/textures/entity/sombra.png */
public class SombraRenderer extends ZombieEntityRenderer {
    private static final Identifier TEXTURE = ModContent.id("textures/entity/sombra.png");

    public SombraRenderer(EntityRendererFactory.Context ctx) { super(ctx); }

    @Override
    public Identifier getTexture(ZombieEntity entity) { return TEXTURE; }
}
