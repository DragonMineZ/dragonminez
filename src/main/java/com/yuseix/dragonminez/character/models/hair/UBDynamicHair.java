package com.yuseix.dragonminez.character.models.hair;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderHighlightEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Explanation of implementation and approach.
 *
 * This class is a model that represents a dynamically rendered hair model for the player.
 * We design this off a "stack and strand" approach. Each of the five visible face directions for the player has a grid
 * of 4x4 "stacks" of hair. Each stack has 4 "strands" of hair.
 *
 * Each stack has a configurable "length", which says how many strands are visible in the stack.
 * Stacks can also be entirely hidden.
 *
 * The hair model is rendered on the player's head, and the base strand should always connect flatly to the face it's on.
 *
 * The hair model is generated entirely in the createBodyLayer method, which does *not* have access to the player's data.
 * This means we must all do all transformations after the point of creation.
 *
 * This is partially done because the initial creation of the model is quite expensive, and we definitely don't want to
 * be doing that every frame.
 */

public class UBDynamicHair extends HumanoidModel<AbstractClientPlayer> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "custommodel"), "main");
    private final ModelPart TopHair;
    private final ModelPart LeftHair;
    private final ModelPart RightHair;
    private final ModelPart BackHair;
    private final Map<String, ModelPart> hairStrands = new HashMap<>();
    private static final float scaleFactor = 0.99F;
    public UBDynamicHair(ModelPart root) {
        super(root);
        root = root.getChild("head");
        this.TopHair = root.getChild("TopHair");
        this.LeftHair = root.getChild("LeftHair");
        this.RightHair = root.getChild("RightHair");
        this.BackHair = root.getChild("BackHair");
        populateStrandMap(root);
    }

    private static String generateStrandId(String face, int rowId, int stackId, int strandId) {
        return String.format("%s_row%d_stack%d_strand%d", face, rowId, stackId, strandId);
    }

    private static CubeListBuilder createHairStrand(int yOffset, float scale) {
        CubeListBuilder builder = CubeListBuilder.create();
        builder.texOffs(6, 0).addBox(
            -0.5F * scale, yOffset, -1.5F * scale,  // position
            2.0F * scale, 2.0F, 2.0F * scale        // dimensions
        );
        return builder;
    }

    private static void addHairRow(PartDefinition parent, String face, int rowNum, float offsetZ, Map<String, ModelPart> strandMap) {
        PartDefinition row = parent.addOrReplaceChild("Row" + rowNum, CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, offsetZ));
        
        for (int stackId = 0; stackId < 4; stackId++) {
            PartDefinition stack = row.addOrReplaceChild(
                "HairStack" + (((rowNum-1) * 4) + stackId + 1),
                CubeListBuilder.create(),
                PartPose.offset(-3.5F + (2.0F * stackId), 0.0F, -2.5F)
            );

            // Create four individual strands for each stack
            float scale = 1.0F;
            for (int strandId = 0; strandId < 4; strandId++) {
                String strandKey = generateStrandId(face, rowNum, stackId + 1, strandId + 1);
                PartDefinition strand = stack.addOrReplaceChild(
                    "Strand" + (strandId + 1),
                    createHairStrand(-2 * (strandId + 1), scale),
                    PartPose.offset(0.0F, 0.0F, 0.0F)
                );
                scale *= scaleFactor;
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition partdefinition = meshdefinition.getRoot().getChild("head");

        // Top Hair
        PartDefinition TopHair = partdefinition.addOrReplaceChild("TopHair", CubeListBuilder.create(), PartPose.offset(0.0F, -8.0F, 0.0F));
        for (int i = 0; i < 4; i++) {
            addHairRow(TopHair, "top", i + 1, i * 2.0F, null);
        }

        // Left Hair
        PartDefinition LeftHair = partdefinition.addOrReplaceChild("LeftHair", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.0F, -4.0F, 0.0F, 0.0F, 0.0F, -1.5708F));
        for (int i = 0; i < 4; i++) {
            addHairRow(LeftHair, "left", i + 1, i * 2.0F, null);
        }

        // Right Hair
        PartDefinition RightHair = partdefinition.addOrReplaceChild("RightHair", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0F, -4.0F, 0.0F, 0.0F, 0.0F, 1.5708F));
        for (int i = 0; i < 4; i++) {
            addHairRow(RightHair, "right", i + 1, i * 2.0F, null);
        }

        // Back Hair
        PartDefinition BackHair = partdefinition.addOrReplaceChild("BackHair", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -4.0F, 4.0F, -1.5708F, 0.0F, 1.5708F));
        for (int i = 0; i < 4; i++) {
            addHairRow(BackHair, "back", i + 1, i * 2.0F, null);
        }

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    // Add this method to populate the strand map after construction
    private void populateStrandMap(ModelPart root) {
        String[] faces = {"Top", "Left", "Right", "Back"};

        for (String face : faces) {
            ModelPart facePart = root.getChild(face + "Hair");
            for (int row = 1; row <= 4; row++) {
                ModelPart rowPart = facePart.getChild("Row" + row);
                for (int stack = 1; stack <= 4; stack++) {
                    ModelPart stackPart = rowPart.getChild("HairStack" + ((row-1) * 4 + stack));
                    for (int strand = 1; strand <= 4; strand++) {
                        String strandId = generateStrandId(face, row, stack, strand);
                        hairStrands.put(strandId, stackPart.getChild("Strand" + strand));
                    }
                }
            }
        }
    }

    /**
     * Curves a hair stack by rotating each strand progressively
     * @param stack The hair stack ModelPart containing the strands
     * @param xCurve X-axis rotation in radians
     * @param zCurve Z-axis rotation in radians
     */
    public void curveHairStack(ModelPart stack, float xCurve, float zCurve) {
        float multiplier = 1.0f;
        for (int i = 1; i <= 4; i++) {
            ModelPart strand = stack.getChild("Strand" + i);
            if (strand != null) {
                // Apply cumulative rotation to each subsequent strand
                strand.xRot = xCurve * multiplier;
                strand.zRot = zCurve * multiplier;
                multiplier += 0.5f; // Each strand curves more than the previous
            }
        }
    }

    /**
     * Curves a specific hair stack using its identifier
     * @param face The face (top, left, right, back)
     * @param row Row number (1-4)
     * @param stack Stack number (1-4)
     * @param xCurve X-axis rotation in radians
     * @param zCurve Z-axis rotation in radians
     */
    public void curveHairStackById(String face, int row, int stack, float xCurve, float zCurve) {
        ModelPart facePart = switch (face.toLowerCase()) {
            case "top" -> TopHair;
            case "left" -> LeftHair;
            case "right" -> RightHair;
            case "back" -> BackHair;
            default -> throw new IllegalArgumentException("Invalid face: " + face);
        };

        ModelPart rowPart = facePart.getChild("Row" + row);
        ModelPart stackPart = rowPart.getChild("HairStack" + ((row-1) * 4 + stack));
        curveHairStack(stackPart, xCurve, zCurve);
    }

    @Override
    public void setupAnim(AbstractClientPlayer pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        // Front Row
        curveHairStackById("top", 1, 1, -0.1f, 0.1f);
        curveHairStackById("top", 1, 2, -0.1f, 0.1f);
        curveHairStackById("top", 1, 3, 1.5f, -0.15f);
        curveHairStackById("top", 1, 4, -0.1f, -0.1f);

        // Second Row
        curveHairStackById("top", 2, 1, -0.1f, 0.1f);
        curveHairStackById("top", 2, 2, -0.1f, 0.1f);
        curveHairStackById("top", 2, 3, -0.1f, 0.1f);
        curveHairStackById("top", 2, 4, -0.1f, -0.1f);

        // Third Row
        curveHairStackById("top", 3, 1, 0.1f, 0.1f);
        curveHairStackById("top", 3, 2, 0.1f, 0.1f);
        curveHairStackById("top", 3, 3, 0.1f, -0.1f);
        curveHairStackById("top", 3, 4, 0.1f, -0.1f);

        // Fourth Row
        curveHairStackById("top", 4, 1, 0.1f, 0.1f);
        curveHairStackById("top", 4, 2, 0.1f, 0.1f);
        curveHairStackById("top", 4, 3, 0.1f, -0.1f);
        curveHairStackById("top", 4, 4, 0.1f, -0.1f);
        super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        TopHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        LeftHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        RightHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        BackHair.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}