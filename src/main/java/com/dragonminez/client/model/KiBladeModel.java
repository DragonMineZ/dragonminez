package com.dragonminez.client.model;

import com.dragonminez.Reference;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class KiBladeModel extends HumanoidModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "kiweapons"), "ki_blade");
	public final ModelPart right_arm;
	private final ModelPart blade_right;

	public KiBladeModel(ModelPart root) {
        super(root);
        this.right_arm = root.getChild("right_arm");
		this.blade_right = this.right_arm.getChild("blade_right");
	}

	public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));

		PartDefinition blade_right = right_arm.addOrReplaceChild("blade_right", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 9.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(1, 0).addBox(-1.5F, 13.0F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(3, 2).addBox(-1.5F, 9.0F, -2.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(3, 2).addBox(-1.5F, 9.0F, 1.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(3, 2).addBox(-1.25F, 13.0F, -2.5F, 0.5F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(3, 2).addBox(-1.25F, 13.0F, 1.5F, 0.5F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(4, 2).addBox(-1.0F, 9.0F, 2.5F, 0.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(4, 2).addBox(-1.0F, 16.0F, 2.0F, 0.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(4, 2).addBox(-1.0F, 16.0F, -3.0F, 0.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(4, 2).addBox(-1.0F, 19.0F, -2.0F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(3, 1).addBox(-1.0F, 21.0F, -1.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(4, 2).addBox(-1.0F, 19.0F, 1.0F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(4, 2).addBox(-1.0F, 9.0F, -3.5F, 0.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(3, 2).addBox(-1.15F, 16.0F, -2.0F, 0.25F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-1.15F, 19.0F, -1.0F, 0.25F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(3, 2).addBox(-1.15F, 16.0F, 1.0F, 0.25F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-1.5F, 16.0F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(40, 16).addBox(-3.25F, 7.0F, -2.25F, 4.5F, 4.0F, 4.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition plano_r1 = blade_right.addOrReplaceChild("plano_r1", CubeListBuilder.create().texOffs(45, 21).addBox(-2.25F, -2.0F, 0.0F, 4.5F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 9.0F, 2.9F, -0.3054F, 0.0F, 0.0F));

		PartDefinition plano_r2 = blade_right.addOrReplaceChild("plano_r2", CubeListBuilder.create().texOffs(46, 21).addBox(-0.5F, -2.0F, -1.5F, 1.5F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0266F, 6.9813F, 1.6728F, -2.8661F, 0.7264F, 3.1013F));

		PartDefinition plano_r3 = blade_right.addOrReplaceChild("plano_r3", CubeListBuilder.create().texOffs(46, 21).addBox(-1.25F, -2.25F, 1.25F, 1.5F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0441F, 7.5057F, -1.5162F, 3.0047F, 0.7003F, -3.0122F));

		PartDefinition plano_r4 = blade_right.addOrReplaceChild("plano_r4", CubeListBuilder.create().texOffs(46, 21).addBox(-0.75F, -1.25F, 0.5F, 1.5F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0473F, 6.8431F, 2.258F, -0.1745F, 0.7854F, 0.0F));

		PartDefinition plano_r5 = blade_right.addOrReplaceChild("plano_r5", CubeListBuilder.create().texOffs(46, 21).addBox(-1.0F, -3.5F, 0.25F, 1.5F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.3536F, 9.0F, -2.4964F, 0.1745F, 0.7854F, 0.0F));

		PartDefinition plano_r6 = blade_right.addOrReplaceChild("plano_r6", CubeListBuilder.create().texOffs(45, 21).addBox(-2.25F, -2.0F, 0.0F, 4.5F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 9.0F, -2.85F, 0.3054F, 0.0F, 0.0F));

		PartDefinition plano_r7 = blade_right.addOrReplaceChild("plano_r7", CubeListBuilder.create().texOffs(45, 16).addBox(0.0F, -2.0F, -2.25F, 0.0F, 4.0F, 4.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.9F, 9.0F, 0.0F, 0.0F, 0.0F, -0.3054F));

		PartDefinition plano_r8 = blade_right.addOrReplaceChild("plano_r8", CubeListBuilder.create().texOffs(45, 16).addBox(0.0F, -2.0F, -2.25F, 0.0F, 4.0F, 4.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.85F, 9.0F, 0.0F, 0.0F, 0.0F, 0.3054F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

    @Override
    public void setupAnim(LivingEntity pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
    }

    @Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}