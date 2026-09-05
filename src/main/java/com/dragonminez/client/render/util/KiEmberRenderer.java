package com.dragonminez.client.render.util;

import com.dragonminez.client.render.shader.DMZShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Fire-ash flakes shed by a ki ball, shared by every ki renderer so waves and blasts throw the
 * same debris.
 *
 * A flake tears loose at the ball's surface, flares out, is dragged backwards into the attack's
 * wake, then shrinks and burns away. Each one is a flat camera-facing quad whose outline and
 * colour bands are carved in the fragment shader, so the caller only supplies placement.
 *
 * Distances and sizes are in ball radii: call this with the pose already scaled to the ball.
 */
public final class KiEmberRenderer {
	private static final int   EMBER_COUNT = 10;
	private static final float EMBER_PERIOD = 11.0F;
	private static final float EMBER_IGNITE = 0.12F;
	private static final float EMBER_FADE_FROM = 0.74F;
	private static final float EMBER_R_START = 0.88F;
	private static final float EMBER_R_END = 4.20F;
	/** Flakes flare out big and collapse to the small travelling size within the first third. */
	private static final float EMBER_SIZE_BIRTH = 0.46F;
	private static final float EMBER_SIZE_DEATH = 0.10F;
	private static final float EMBER_COOL = 0.7F;
	private static final float EMBER_BACKDRAFT = 1.85F;

	/** Backwards along the attack, for callers already working in the projectile's rotated frame. */
	public static final Vec3 LOCAL_BACK = new Vec3(0.0D, 0.0D, -1.0D);
	/** A charging orb has no forward wake to be dragged into, so its flakes barely trail. */
	public static final float CHARGE_BACKDRAFT = 0.35F;

	private KiEmberRenderer() {}

	/**
	 * @param back      backwards along the attack, in the CURRENT pose's local space
	 * @param backdraft how strongly the wake drags the flakes back; 1 for a moving attack
	 * @param sizeScale multiplies flake size, for balls that should shed heavier debris
	 */
	public static void render(PoseStack poseStack, Matrix4f proj, float[] coreColor, float[] borderColor,
	                          float[] outlineColor, float ageInTicks, float alphaMultiplier,
	                          Vec3 back, float backdraft, float sizeScale) {
		ShaderInstance shader = DMZShaders.ki3dShader;
		if (shader == null) return;

		float cycles = ageInTicks / EMBER_PERIOD;

		shader.safeGetUniform("ProjMat").set(proj);
		shader.safeGetUniform("zCut").set(-1.0f);
		shader.safeGetUniform("zCutFar").set(2.0f);
		shader.safeGetUniform("blotchMode").set(0.0f);
		shader.safeGetUniform("flameMode").set(0.0f);
		shader.safeGetUniform("splatMode").set(1.0f);
		shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
		shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);

		VertexBuffer mesh = KiMeshFactory.getQuadMesh();
		mesh.bind();

		for (int i = 0; i < EMBER_COUNT; i++) {
			float raw = cycles + (float) i / EMBER_COUNT;
			float phase = raw - (float) Math.floor(raw);
			int seed = i * 131 + (int) Math.floor(raw) * 7919;

			float ignite = Math.min(1.0F, phase / EMBER_IGNITE);
			float burnout = 1.0F - Mth.clamp((phase - EMBER_FADE_FROM) / (1.0F - EMBER_FADE_FROM), 0.0F, 1.0F);
			float alpha = ignite * burnout * alphaMultiplier;
			if (alpha <= 0.01F) continue;

			// Thrown clear fast then coasting.
			float travel = 1.0F - (1.0F - phase) * (1.0F - phase);
			float radius = EMBER_R_START + (EMBER_R_END - EMBER_R_START) * travel;

			// Shares the flight easing, so a flake is fattest at the instant it tears loose and
			// has collapsed to its travelling size by the time it is clear of the ball. Death
			// size stays well above zero: the shader eats the flake away as it ages, and if the
			// quad collapsed too there would be nothing left to watch crumble.
			float size = Mth.lerp(travel, EMBER_SIZE_BIRTH, EMBER_SIZE_DEATH)
					* (0.75F + hash01(seed + 41) * 0.5F) * sizeScale;
			if (size <= 1.0E-4F) continue;

			float dz = hash01(seed) * 2.0F - 1.0F;
			float ring = (float) Math.sqrt(Math.max(0.0F, 1.0F - dz * dz));
			float angle = hash01(seed + 97) * 2.0F * (float) Math.PI;
			float dx = ring * (float) Math.cos(angle);
			float dy = ring * (float) Math.sin(angle);

			// Backdraft: the attack drives forward hard enough to drag anything it sheds into
			// its wake, so the flake leaves radially and then curls back down the attack.
			float drag = EMBER_BACKDRAFT * backdraft * travel * travel;

			// Cooling: white-hot on release, settling towards the border colour as it dies.
			float cool = phase * phase * EMBER_COOL;
			shader.safeGetUniform("colorCore").set(
					Mth.lerp(cool, coreColor[0], borderColor[0]),
					Mth.lerp(cool, coreColor[1], borderColor[1]),
					Mth.lerp(cool, coreColor[2], borderColor[2]));
			shader.safeGetUniform("alphaMult").set(alpha);
			// Identity seed picks which slice of the noise field this flake is cut from, so
			// two flakes are different shapes rather than the same one rotated.
			shader.safeGetUniform("time").set(hash01(seed + 7) * 64.0f);
			// Age drives both the reshaping and how much of the flake has burned away.
			shader.safeGetUniform("splatLife").set(phase);

			poseStack.pushPose();
			poseStack.translate(dx * radius + back.x * drag, dy * radius + back.y * drag, dz * radius + back.z * drag);
			faceCamera(poseStack);
			poseStack.scale(size, size, size);
			shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
			shader.apply();
			mesh.drawWithShader(poseStack.last().pose(), proj, shader);
			poseStack.popPose();
		}

		VertexBuffer.unbind();
		shader.safeGetUniform("splatMode").set(0.0f);
		shader.safeGetUniform("splatLife").set(0.0f);
		shader.clear();
	}

	/**
	 * Turns the current pose square to the screen, keeping its position and uniform scale.
	 * An entity-render pose already carries the camera rotation, so flattening its 3x3 back to
	 * a plain scale is all a billboard needs -- and it works from any call site, whether or not
	 * the caller has rotated into the attack's own frame.
	 */
	private static void faceCamera(PoseStack poseStack) {
		Matrix4f m = poseStack.last().pose();
		float scale = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
		m.m00(scale); m.m01(0.0F); m.m02(0.0F);
		m.m10(0.0F); m.m11(scale); m.m12(0.0F);
		m.m20(0.0F); m.m21(0.0F); m.m22(scale);
	}

	/** Cheap deterministic 0..1 hash, so a flake holds one direction and shape for its whole life. */
	private static float hash01(int seed) {
		int h = seed * 374761393 + 668265263;
		h = (h ^ (h >>> 13)) * 1274126177;
		return ((h ^ (h >>> 16)) & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
	}
}
