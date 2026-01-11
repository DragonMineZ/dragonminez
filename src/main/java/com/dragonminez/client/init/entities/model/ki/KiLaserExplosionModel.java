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

public class KiLaserExplosionModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "tech"), "ki_laser_exp");
	private final ModelPart kiball;
	private final ModelPart kiball2;

	public KiLaserExplosionModel(ModelPart root) {
		this.kiball = root.getChild("kiball");
		this.kiball2 = this.kiball.getChild("kiball2");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition kiball = partdefinition.addOrReplaceChild("kiball", CubeListBuilder.create(), PartPose.offset(-0.5062F, 20.3688F, -0.3188F));

		PartDefinition cube_r1 = kiball.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(29, 9).addBox(-1.5F, -0.5F, -5.0F, 3.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.2563F, -2.1688F, 1.3088F, -0.7854F, 0.0F, 0.7854F));

		PartDefinition cube_r2 = kiball.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(29, 9).addBox(-1.5F, 0.5F, -5.0F, 3.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2437F, 2.2312F, 1.3088F, 0.7854F, 0.0F, 0.7854F));

		PartDefinition cube_r3 = kiball.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(31, 9).addBox(-0.5F, -1.5F, -5.0F, 0.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1937F, -0.0188F, 1.3188F, 0.0F, 0.7854F, 0.0F));

		PartDefinition cube_r4 = kiball.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(32, 9).addBox(0.5F, -1.5F, -5.0F, 0.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.1563F, -0.0188F, 1.3188F, 0.0F, -0.7854F, 0.0F));

		PartDefinition cube_r5 = kiball.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(29, 9).addBox(-1.5F, -0.5F, -5.0F, 3.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2438F, -2.2688F, 1.3088F, -0.7854F, 0.0F, -0.7854F));

		PartDefinition cube_r6 = kiball.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(29, 9).addBox(-1.5F, 0.5F, -5.0F, 3.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.2563F, 2.2312F, 1.3088F, 0.7854F, 0.0F, -0.7854F));

		PartDefinition cube_r7 = kiball.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(29, 9).addBox(-1.5F, -0.5F, -5.0F, 3.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0062F, -3.1188F, 1.3188F, -0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r8 = kiball.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(29, 9).addBox(-1.5F, 0.5F, -5.0F, 3.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0062F, 3.1312F, 1.3188F, 0.7854F, 0.0F, 0.0F));

		PartDefinition kiball2 = kiball.addOrReplaceChild("kiball2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.8032F, 0.0F, 0.0F, -0.4363F));

		PartDefinition cube_r9 = kiball2.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, -0.5F, -3.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.2563F, -2.1688F, 1.0557F, -0.7854F, 0.0F, 0.7854F));

		PartDefinition cube_r10 = kiball2.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, 0.5F, -3.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2437F, 2.2313F, 1.0557F, 0.7854F, 0.0F, 0.7854F));

		PartDefinition cube_r11 = kiball2.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(3, 23).addBox(-0.5F, -1.5F, -3.0F, 0.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1937F, -0.0188F, 1.0657F, 0.0F, 0.7854F, 0.0F));

		PartDefinition cube_r12 = kiball2.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(3, 23).addBox(0.5F, -1.5F, -3.0F, 0.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.1563F, -0.0188F, 1.0657F, 0.0F, -0.7854F, 0.0F));

		PartDefinition cube_r13 = kiball2.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, -0.5F, -3.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2438F, -2.2687F, 1.0557F, -0.7854F, 0.0F, -0.7854F));

		PartDefinition cube_r14 = kiball2.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, 0.5F, -3.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.2563F, 2.2313F, 1.0557F, 0.7854F, 0.0F, -0.7854F));

		PartDefinition cube_r15 = kiball2.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, -0.5F, -3.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0063F, -3.1187F, 1.0657F, -0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r16 = kiball2.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, 0.5F, -3.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0063F, 3.1313F, 1.0657F, 0.7854F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float speed = 0.5F;
        this.kiball.zRot = ageInTicks * speed;
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		kiball.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}