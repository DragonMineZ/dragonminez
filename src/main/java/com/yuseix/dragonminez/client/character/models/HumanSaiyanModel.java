package com.yuseix.dragonminez.client.character.models;// Made with Blockbench 4.10.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.stats.DMZStatsAttributes;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class HumanSaiyanModel<T extends LivingEntity> extends PlayerModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(DragonMineZ.MOD_ID, "races"), "humansaiyan");
	private final ModelPart Head;
	private final ModelPart Body;
	private final ModelPart RightArm;
	private final ModelPart LeftArm;
	private final ModelPart RightLeg;
	private final ModelPart LeftLeg;

	public HumanSaiyanModel(ModelPart root) {
        super(root, false);
        this.Head = root.getChild("head");
		this.Body = root.getChild("body");
		this.RightArm = root.getChild("right_arm");
		this.LeftArm = root.getChild("left_arm");
		this.RightLeg = root.getChild("right_leg");
		this.LeftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0, 13, 0));

		PartDefinition Body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 0.0F, 0.0F));


		PartDefinition RightArm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(-5.0F, 2.0F, 0.0F));

		PartDefinition LeftArm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(5.0F, 2.0F, 0.0F));

		PartDefinition RightLeg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(-1.9F, 12.0F, 0.0F));

		PartDefinition LeftLeg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(1.9F, 12.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
		super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);

		DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, pEntity).ifPresent(cap ->{
			var formRelease = cap.getIntValue("formrelease");
			var isTransfOn = cap.getBoolean("transform");
			var groupForm = cap.getStringValue("groupform");
			var dmzform = cap.getStringValue("form");
			var race = cap.getIntValue("race");

			switch (race){
				case 1:
					switch (groupForm){
						case "ssgrades":
							if(dmzform.equals("base") && isTransfOn && formRelease > 1){
								float time = (pEntity.tickCount % 40) / 40.0f; // Oscila cada 40 ticks (2 segundos)
								head.xRot = 0.2f + (float) Math.sin(time * Math.PI) * 0.5f; // Oscila entre 1.2 y 2.5
							}
							break;
						case "ssj":
							break;
						case "":
							if(dmzform.equals("base") && isTransfOn){
								float scaleFactor = 1.0F + 0.03F * (float) Math.sin(pAgeInTicks * 0.4F);
								body.xScale = scaleFactor;
								body.yScale = scaleFactor;
								body.zScale = scaleFactor;

								rightArm.xScale = scaleFactor;
								rightArm.yScale = scaleFactor;
								rightArm.zScale = scaleFactor;

								leftArm.xScale = scaleFactor;
								leftArm.yScale = scaleFactor;
								leftArm.zScale = scaleFactor;

								leftArm.zRot = -0.2f;
								rightArm.zRot = 0.2f;

							} else {
								body.xScale = 1.0f;
								body.yScale = 1.0f;
								body.zScale = 1.0f;
								rightArm.xScale = 1.0f;
								rightArm.yScale = 1.0f;
								rightArm.zScale = 1.0f;
								leftArm.xScale = 1.0f;
								leftArm.yScale = 1.0f;
								leftArm.zScale = 1.0f;
							}
							break;

						default:
							break;
					}

					break;
				case 5:
					if(dmzform.equals("evil")){
						rightArm.xScale = 0.7f;
						leftArm.xScale = 0.7f;
					} else if(dmzform.equals("kid")) {
						rightArm.xScale = 0.7f;
						leftArm.xScale = 0.7f;
					} else {
						rightArm.xScale = 1.0f;
						leftArm.xScale = 1.0f;
					}
					break;
				default:
					break;
			}


		});

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}