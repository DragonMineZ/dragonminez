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

public class KiTridentModel extends HumanoidModel{
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Reference.MOD_ID, "kiweapons"), "kitrident");
	private final ModelPart trident_right;
	private final ModelPart trident;
	private final ModelPart kisword2;

	public KiTridentModel(ModelPart root) {
        super(root);
        this.trident_right = root.getChild("trident_right");
		this.trident = this.trident_right.getChild("trident");
		this.kisword2 = this.trident.getChild("kisword2");
	}

	public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition trident_right = partdefinition.addOrReplaceChild("trident_right", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition trident = trident_right.addOrReplaceChild("trident", CubeListBuilder.create().texOffs(31, 44).addBox(-7.05F, 40.0F, -54.05F, 2.1F, 8.95F, 2.1F, new CubeDeformation(0.0F))
		.texOffs(48, 29).addBox(-6.85F, 33.0F, -53.85F, 1.7F, 6.95F, 1.7F, new CubeDeformation(0.0F))
		.texOffs(41, 18).addBox(-6.65F, 26.1F, -53.65F, 1.3F, 6.95F, 1.3F, new CubeDeformation(0.0F))
		.texOffs(54, 15).addBox(-6.45F, 19.15F, -53.45F, 0.9F, 6.95F, 0.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 39.0F, 32.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r1 = trident.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 46).addBox(-3.0F, -1.5F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.9591F, 48.7004F, -53.004F, 0.0F, 0.0F, -1.5708F));

		PartDefinition cube_r2 = trident.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(20, 39).addBox(-3.0F, -1.7F, -0.7F, 4.0F, 1.4F, 1.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.9591F, 44.7004F, -53.004F, 0.0F, 0.0F, -1.5708F));

		PartDefinition cube_r3 = trident.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(39, 41).addBox(-4.9F, -1.3F, -0.3F, 5.9F, 0.6F, 0.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.1093F, 55.8091F, -47.0845F, -1.5708F, 0.0873F, -1.5708F));

		PartDefinition cube_r4 = trident.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(45, 47).addBox(-4.9F, -1.5F, -0.5F, 5.9F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.1093F, 49.949F, -47.598F, -1.5708F, 0.0873F, -1.5708F));

		PartDefinition cube_r5 = trident.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(24, 33).addBox(-4.9F, -1.7F, -0.7F, 5.9F, 1.4F, 1.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.1093F, 44.0715F, -48.1122F, -1.5708F, 0.0873F, -1.5708F));

		PartDefinition cube_r6 = trident.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(4, 11).addBox(-6.0F, -2.0F, -1.0F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.1093F, 48.8501F, -50.604F, 1.5708F, 0.7418F, 1.5708F));

		PartDefinition cube_r7 = trident.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(4, 11).addBox(-6.0F, -2.0F, -1.0F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.1093F, 47.8501F, -55.004F, -1.5708F, -0.7854F, 1.5708F));

		PartDefinition cube_r8 = trident.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(23, 5).addBox(-7.0F, -2.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.0093F, 48.6501F, -53.004F, 0.0F, 0.0F, 0.7854F));

		PartDefinition kisword2 = trident.addOrReplaceChild("kisword2", CubeListBuilder.create(), PartPose.offsetAndRotation(6.0F, 39.0F, -53.0F, 0.0436F, 0.0F, 0.0F));

		PartDefinition cube_r9 = kisword2.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(3, 3).mirror().addBox(-1.0F, -1.3F, -0.3F, 6.9F, 0.6F, 0.6F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-12.1093F, 15.2534F, -7.2131F, -1.5708F, 0.0436F, 1.5708F));

		PartDefinition cube_r10 = kisword2.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(25, 17).mirror().addBox(-1.0F, -1.5F, -0.5F, 5.7F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-12.1093F, 10.0584F, -6.9863F, -1.5708F, 0.0436F, 1.5708F));

		PartDefinition cube_r11 = kisword2.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(1, 38).mirror().addBox(-1.0F, -1.7F, -0.7F, 5.7F, 1.4F, 1.4F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-12.1093F, 4.3638F, -6.7377F, -1.5708F, 0.0436F, 1.5708F));

		PartDefinition cube_r12 = kisword2.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(1, 19).mirror().addBox(-1.0F, -2.0F, -1.0F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-9.9907F, 9.6501F, -0.004F, 0.0F, 0.0F, -0.7854F));

		PartDefinition cube_r13 = kisword2.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(20, 28).mirror().addBox(-1.0F, -1.4F, -0.3F, 7.0F, 0.7F, 0.6F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-19.9901F, 12.78F, -0.004F, 0.0F, 0.0F, 1.5272F));

		PartDefinition cube_r14 = kisword2.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(20, 28).mirror().addBox(-1.0F, -1.3F, -0.3F, 7.0F, 0.6F, 0.6F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-6.6901F, 12.78F, -0.004F, 0.0F, 0.0F, 1.5272F));

		PartDefinition cube_r15 = kisword2.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(2, 47).mirror().addBox(-1.0F, -1.5F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-6.8209F, 9.7829F, -0.004F, 0.0F, 0.0F, 1.5272F));

		PartDefinition cube_r16 = kisword2.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(5, 27).mirror().addBox(-1.0F, -1.7F, -0.7F, 4.0F, 1.4F, 1.4F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-6.9954F, 5.7867F, -0.004F, 0.0F, 0.0F, 1.5272F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		trident_right.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}