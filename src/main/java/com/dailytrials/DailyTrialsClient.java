package com.dailytrials;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.VillagerEntityRenderer;

public class DailyTrialsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModContent.SOMBRA, SombraRenderer::new);
        EntityRendererRegistry.register(ModContent.ESCRIBANO, VillagerEntityRenderer::new);
    }
}
