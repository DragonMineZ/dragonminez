package com.dragonminez.client.init.entities.model.ki;// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


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

public class KiBlockModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "tech"), "ki_block2");
	private final ModelPart ball;

	public KiBlockModel(ModelPart root) {
		this.ball = root.getChild("ball");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition ball = partdefinition.addOrReplaceChild("ball", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(29, 75).addBox(-7.0F, -7.0F, -9.0F, 14.0F, 14.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(70, 45).addBox(-6.0F, -6.0F, -10.0F, 12.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.0F, 0.0F));

		PartDefinition cube_r1 = ball.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(70, 45).addBox(-6.0F, -6.0F, -0.5F, 12.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.25F, 9.5F, 0.0F, 0.0F, 1.5708F, -1.5708F));

		PartDefinition cube_r2 = ball.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(70, 45).addBox(-6.0F, -6.0F, -0.5F, 12.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.25F, -9.5F, 0.0F, 0.0F, 1.5708F, 1.5708F));

		PartDefinition cube_r3 = ball.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(70, 45).addBox(-6.0F, -6.0F, -0.5F, 12.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.5F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r4 = ball.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(70, 45).addBox(-6.0F, -6.0F, -0.5F, 12.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.5F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r5 = ball.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(70, 45).addBox(-6.0F, -6.0F, -0.5F, 12.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 9.5F, 0.0F, 3.1416F, 0.0F));

		PartDefinition cube_r6 = ball.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(29, 75).addBox(-7.0F, -7.0F, -0.5F, 14.0F, 14.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.5F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r7 = ball.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(29, 75).addBox(-7.0F, -7.0F, -0.5F, 14.0F, 14.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 8.5F, 0.0F, 0.0F, -1.5708F, 1.5708F));

		PartDefinition cube_r8 = ball.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(29, 75).addBox(-7.0F, -7.0F, -0.5F, 14.0F, 14.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.5F, 0.0F, 0.0F, -1.5708F, -1.5708F));

		PartDefinition cube_r9 = ball.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(29, 75).addBox(-7.0F, -7.0F, -0.5F, 14.0F, 14.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.5F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r10 = ball.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(29, 75).addBox(-7.0F, -7.0F, -0.5F, 14.0F, 14.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 8.5F, 0.0F, 3.1416F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float velocidadGlobal = 4.8f;

        this.ball.xRot = ageInTicks * velocidadGlobal * 4.5f;
        this.ball.yRot = ageInTicks * velocidadGlobal * 4.5f;
        this.ball.zRot = ageInTicks * velocidadGlobal * 4.5f;
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		ball.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}