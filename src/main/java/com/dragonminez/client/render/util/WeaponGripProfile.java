package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.*;

public enum WeaponGripProfile {

	SWORD {
		@Override
		public void applyRight(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(-0.05, 0.135, -0.1);
		}
		@Override
		public void applyLeft(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-25));
			ps.mulPose(Axis.ZP.rotationDegrees(180));
			ps.translate(-0.06, -0.38, -0.4);
		}
	},

	TOOL {
		@Override
		public void applyRight(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(-0.05, 0.135, -0.1);
		}
		@Override
		public void applyLeft(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-25));
			ps.mulPose(Axis.ZP.rotationDegrees(180));
			ps.translate(-0.06, -0.38, -0.4);
		}
	},

	TRIDENT {
		@Override
		public void applyRight(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.mulPose(Axis.YP.rotationDegrees(180));
			ps.translate(0.05, 0.0, -0.2);
		}
		@Override
		public void applyLeft(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.mulPose(Axis.YP.rotationDegrees(180));
			ps.translate(-0.05, 0.0, -0.2);
		}
	},

	BOW {
		@Override
		public void applyRight(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(0.02, 0.135, -0.1);
		}
		@Override
		public void applyLeft(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-180));
			ps.mulPose(Axis.YP.rotationDegrees(12));
			ps.mulPose(Axis.ZP.rotationDegrees(-12));
			ps.translate(0.1, 0.05, -0.16);
		}
	},

	CROSSBOW {
		@Override
		public void applyRight(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(-0.05, 0.135, -0.1);
		}
		@Override
		public void applyLeft(PoseStack ps) {
			ps.mulPose(Axis.ZP.rotationDegrees(60));
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(-0.42, 0.135, 0.1);
		}
	},

	SHIELD {
		@Override
		public void applyRight(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(-0.15, 0.135, -0.05);
		}
		@Override
		public void applyLeft(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.mulPose(Axis.YP.rotationDegrees(180));
			ps.translate(-0.03, 0.135, -1.39);
		}
	},

	SHIELD_ACTIVE {
		@Override
		public void applyRight(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(-0.15, 0.135, -0.05);
		}
		@Override
		public void applyLeft(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(45));
			ps.mulPose(Axis.YP.rotationDegrees(125));
			ps.mulPose(Axis.ZP.rotationDegrees(-95));
			ps.translate(-0.80, 0.75, -0.45);
		}
	},

	// Bloque / ItemBlock
	BLOCK {
		@Override
		public void applyRight(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(-0.05, 0.135, -0.1);
		}
		@Override
		public void applyLeft(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(0, 0.1, -0.1);
		}
	},

	// Todo lo demás (comida, tótem, objetos planos, items de mods sin clase conocida)
	DEFAULT {
		@Override
		public void applyRight(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(-0.05, 0.135, -0.1);
		}
		@Override
		public void applyLeft(PoseStack ps) {
			ps.mulPose(Axis.XP.rotationDegrees(-90));
			ps.translate(0.055, 0.13, -0.1);
		}
	};

	public abstract void applyRight(PoseStack ps);
	public abstract void applyLeft(PoseStack ps);

	public void apply(PoseStack ps, boolean isLeft) {
		if (isLeft) applyLeft(ps);
		else applyRight(ps);
	}

	public static WeaponGripProfile resolve(Item item, boolean isUsing, String weaponTypeHint) {
		if (weaponTypeHint != null && !weaponTypeHint.isEmpty()) {
			WeaponGripProfile custom = resolveFromWeaponType(weaponTypeHint);
			if (custom != null) return custom;
		}

		if (item instanceof ShieldItem) return isUsing ? SHIELD_ACTIVE : SHIELD;
		if (item instanceof TridentItem) return TRIDENT;
		if (item instanceof BowItem) return BOW;
		if (item instanceof CrossbowItem) return CROSSBOW;
		if (item instanceof BlockItem) return BLOCK;
		if (item instanceof SwordItem) return SWORD;
		if (item instanceof TieredItem) return TOOL;

		return DEFAULT;
	}

	private static WeaponGripProfile resolveFromWeaponType(String category) {
		return switch (category.toLowerCase()) {
			case "sword", "katana", "claymore", "greatsword", "scimitar", "dagger", "knife" -> SWORD;
			case "spear", "lance", "polearm", "naginata" -> TRIDENT;
			case "staff", "bo_staff" -> SWORD;
			default -> null;
		};
	}
}