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

public class KiDiscModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "tech"), "ki_disc");
	private final ModelPart kidisc;

	public KiDiscModel(ModelPart root) {
		this.kidisc = root.getChild("kidisc");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition kidisc = partdefinition.addOrReplaceChild("kidisc", CubeListBuilder.create().texOffs(0, 19).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(1, 52).addBox(6.0F, 0.0F, -5.5F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(44, 55).addBox(7.0F, 0.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 51).addBox(-5.5F, 0.0F, -7.0F, 11.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 62).addBox(-4.0F, 0.0F, -8.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 52).addBox(-7.0F, 0.0F, -5.5F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(30, 55).addBox(-8.0F, 0.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 51).addBox(-5.5F, 0.0F, 6.0F, 11.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(39, 62).addBox(-4.0F, 0.0F, 7.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float degreesPerTick = 120.0F;

        this.kidisc.yRot = (float) Math.toRadians(ageInTicks * degreesPerTick);

        this.kidisc.xRot = 0.0F;
        this.kidisc.zRot = 0.0F;
    }

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		kidisc.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}