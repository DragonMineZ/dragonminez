package com.dragonminez.client.init.entities.model.ki;

import com.dragonminez.Reference;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;


public class KiBallModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "tech"), "ki_ballexplosion");
	private final ModelPart ball;

	public KiBallModel(ModelPart root) {
		this.ball = root.getChild("ball");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition ball = partdefinition.addOrReplaceChild("ball", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(16, 15).addBox(-7.0F, -7.0F, -9.0F, 14.0F, 14.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(16, 15).addBox(-7.0F, -7.0F, 8.0F, 14.0F, 14.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(17, 15).addBox(-6.0F, -6.0F, 9.0F, 12.0F, 12.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(17, 15).addBox(-6.0F, -6.0F, -10.0F, 12.0F, 12.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(3, 2).addBox(-9.0F, -7.0F, -7.0F, 1.0F, 14.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(2, 2).addBox(8.0F, -7.0F, -7.0F, 1.0F, 14.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(4, 4).addBox(9.0F, -6.0F, -6.0F, 1.0F, 12.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(4, 4).addBox(-10.0F, -6.0F, -6.0F, 1.0F, 12.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(3, 2).addBox(-7.0F, -9.0F, -7.0F, 14.0F, 1.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(6, 4).addBox(-6.0F, -10.0F, -6.0F, 12.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(6, 4).addBox(-6.0F, 9.0F, -6.0F, 12.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(3, 2).addBox(-7.0F, 8.0F, -7.0F, 14.0F, 1.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float speedX = 1.7F;
        float speedY = 1.9F;
        float speedZ = 1.8F;

        this.ball.xRot = ageInTicks * speedX;
        this.ball.yRot = ageInTicks * speedY;
        this.ball.zRot = ageInTicks * speedZ;
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		ball.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}