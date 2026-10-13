package com.dailytrials;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/** Modelo de la Sombra: fantasma cuadrado (cabeza, cuerpo, cola en dos bloques y dos brazos levantados). */
public class SombraModel extends EntityModel<SombraEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(ModContent.id("sombra"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public SombraModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.tail1 = root.getChild("tail1");
        this.tail2 = root.getChild("tail2");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData r = data.getRoot();
        // El modelo mide 24 unidades (1,5 bloques); y = 0 es la parte de arriba.
        r.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4f, 0f, -4f, 8f, 8f, 8f),
                ModelTransform.pivot(0f, 0f, 0f));
        r.addChild("body", ModelPartBuilder.create().uv(0, 16).cuboid(-4f, 0f, -3f, 8f, 7f, 6f),
                ModelTransform.pivot(0f, 8f, 0f));
        r.addChild("tail1", ModelPartBuilder.create().uv(0, 32).cuboid(-3f, 0f, -2.5f, 6f, 5f, 5f),
                ModelTransform.pivot(0f, 15f, 0f));
        r.addChild("tail2", ModelPartBuilder.create().uv(0, 44).cuboid(-2f, 0f, -2f, 4f, 4f, 4f),
                ModelTransform.pivot(0f, 20f, 0f));
        r.addChild("right_arm", ModelPartBuilder.create().uv(32, 16).cuboid(-1.5f, 0f, -1.5f, 3f, 6f, 3f),
                ModelTransform.pivot(-5.5f, 9f, 0f));
        r.addChild("left_arm", ModelPartBuilder.create().uv(32, 16).mirrored().cuboid(-1.5f, 0f, -1.5f, 3f, 6f, 3f),
                ModelTransform.pivot(5.5f, 9f, 0f));
        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public void setAngles(SombraEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        head.yaw = headYaw * ((float) Math.PI / 180f);
        head.pitch = headPitch * ((float) Math.PI / 180f);

        float t = animationProgress * 0.12f;
        root.pivotY = MathHelper.sin(t) * 1.2f; // flota arriba y abajo

        float ola = MathHelper.sin(animationProgress * 0.25f) * 0.18f;
        rightArm.roll = 0.9f + ola;   // brazos levantados hacia fuera
        leftArm.roll = -(0.9f + ola);
        rightArm.pitch = MathHelper.sin(t * 1.3f) * 0.15f;
        leftArm.pitch = -rightArm.pitch;

        tail1.yaw = MathHelper.sin(t * 1.5f) * 0.25f;   // la cola ondea
        tail2.yaw = MathHelper.sin(t * 1.5f - 0.8f) * 0.4f;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
