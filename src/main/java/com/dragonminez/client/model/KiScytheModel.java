package com.dragonminez.client.model;

import com.dragonminez.Reference;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class KiScytheModel extends HumanoidModel {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Reference.MOD_ID, "kiweapons"), "kiscythe");
	public final ModelPart scythe_right;
    public final ModelPart hoja;

	public KiScytheModel(ModelPart root) {
        super(root);
        this.scythe_right = root.getChild("scythe_right");
		this.hoja = this.scythe_right.getChild("hoja");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition scythe_right = partdefinition.addOrReplaceChild("scythe_right", CubeListBuilder.create().texOffs(0, 31).addBox(-6.75F, -15.0F, -11.0F, 1.5F, 1.5F, 31.0F, new CubeDeformation(0.0F))
		.texOffs(1, 43).addBox(-6.5F, -14.75F, 20.0F, 1.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(39, 29).addBox(-6.75F, -15.0F, -22.0F, 1.5F, 1.5F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition hoja = scythe_right.addOrReplaceChild("hoja", CubeListBuilder.create().texOffs(42, 50).addBox(-2.25F, 6.75F, -17.5F, 2.0F, 2.25F, 7.5F, new CubeDeformation(0.0F))
		.texOffs(19, 44).addBox(-2.0F, 7.0F, -19.0F, 1.5F, 1.75F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(17, 39).addBox(-1.75F, 7.25F, -21.0F, 1.0F, 1.25F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.75F, -22.0F, -12.0F));

		PartDefinition cube_r1 = hoja.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(18, 3).addBox(-1.0F, -12.0F, 2.25F, 1.0F, 3.0F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, 33.25F, 14.25F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r2 = hoja.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(9, 15).addBox(-1.0F, -12.0F, 1.25F, 1.0F, 5.0F, 3.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, 36.0F, 8.25F, 1.1781F, 0.0F, 0.0F));

		PartDefinition cube_r3 = hoja.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -12.0F, 0.25F, 1.0F, 7.0F, 4.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, 34.5F, 1.0F, 0.9163F, 0.0F, 0.0F));

		PartDefinition cube_r4 = hoja.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(28, 2).addBox(-1.0F, -12.0F, -0.5F, 1.0F, 6.0F, 5.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, 32.5F, -5.0F, 0.6109F, 0.0F, 0.0F));

		PartDefinition cube_r5 = hoja.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(42, 1).addBox(-1.0F, -12.0F, -0.5F, 1.0F, 8.0F, 5.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, 26.5F, -10.25F, 0.48F, 0.0F, 0.0F));

		PartDefinition cube_r6 = hoja.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, -12.0F, -1.0F, 1.0F, 10.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, 18.25F, -15.25F, 0.0873F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		scythe_right.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}