package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateTechniqueC2S {
	private final String name;
	private final String type;
	private final String utility;
	private final float damage;
	private final float speed;
	private final float size;
	private final int armorPen;
	private final int cast;
	private final int cooldown;
	private final int colorInterior;
	private final int colorExterior;
	private final int colorOutline;
	private final String secondaryEffectType;
	private final String affectedStat;
	private final float secondaryIntensity;
	private final int secondaryDuration;

	public CreateTechniqueC2S(String name, String type, String utility, float damage, float speed, float size,
							  int armorPen, int cast, int cooldown, int colorInterior, int colorExterior, int colorOutline,
							  String secondaryEffectType, String affectedStat, float secondaryIntensity, int secondaryDuration) {
		this.name = name;
		this.type = type;
		this.utility = utility;
		this.damage = damage;
		this.speed = speed;
		this.size = size;
		this.armorPen = armorPen;
		this.cast = cast;
		this.cooldown = cooldown;
		this.colorInterior = colorInterior;
		this.colorExterior = colorExterior;
		this.colorOutline = colorOutline;
		this.secondaryEffectType = secondaryEffectType;
		this.affectedStat = affectedStat;
		this.secondaryIntensity = secondaryIntensity;
		this.secondaryDuration = secondaryDuration;
	}

	public CreateTechniqueC2S(FriendlyByteBuf buf) {
		this.name = buf.readUtf();
		this.type = buf.readUtf();
		this.utility = buf.readUtf();
		this.damage = buf.readFloat();
		this.speed = buf.readFloat();
		this.size = buf.readFloat();
		this.armorPen = buf.readInt();
		this.cast = buf.readInt();
		this.cooldown = buf.readInt();
		this.colorInterior = buf.readInt();
		this.colorExterior = buf.readInt();
		this.colorOutline = buf.readInt();
		this.secondaryEffectType = buf.readUtf();
		this.affectedStat = buf.readUtf();
		this.secondaryIntensity = buf.readFloat();
		this.secondaryDuration = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(this.name);
		buf.writeUtf(this.type);
		buf.writeUtf(this.utility);
		buf.writeFloat(this.damage);
		buf.writeFloat(this.speed);
		buf.writeFloat(this.size);
		buf.writeInt(this.armorPen);
		buf.writeInt(this.cast);
		buf.writeInt(this.cooldown);
		buf.writeInt(this.colorInterior);
		buf.writeInt(this.colorExterior);
		buf.writeInt(this.colorOutline);
		buf.writeUtf(this.secondaryEffectType != null ? this.secondaryEffectType : "NONE");
		buf.writeUtf(this.affectedStat != null ? this.affectedStat : "");
		buf.writeFloat(this.secondaryIntensity);
		buf.writeInt(this.secondaryDuration);
	}

	public boolean handle(Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				KiAttackData technique = new KiAttackData();
				technique.setName((name == null || name.trim().isEmpty()) ? "New Skill" : name.trim());
				technique.setAuthor(player.getName().getString());

				KiAttackData.KiType parsedType;
				try {
					parsedType = KiAttackData.KiType.valueOf(type);
				} catch (Exception ignored) {
					parsedType = KiAttackData.KiType.SMALL_BALL;
				}
				technique.setKiType(parsedType);

				KiAttackData.Utility parsedUtility;
				try {
					parsedUtility = KiAttackData.Utility.valueOf(utility);
				} catch (Exception ignored) {
					parsedUtility = KiAttackData.Utility.DAMAGE;
				}
				if (!allowsUtilityForType(parsedType)) parsedUtility = KiAttackData.Utility.DAMAGE;
				technique.setUtility(parsedUtility);

				float[] normalized = KiAttackData.normalizeStatsForType(parsedType, damage, size, speed, armorPen);
				technique.setDamageMultiplier(normalized[0]);
				technique.setSize(normalized[1]);
				technique.setSpeed(normalized[2]);
				technique.setArmorPenetration(Math.round(normalized[3]));
				technique.setCastTime(Mth.clamp(cast, 0, 200));
				technique.setCooldown(Mth.clamp(cooldown, 0, 400));
				technique.setColorInterior(colorInterior & 0xFFFFFF);
				technique.setColorExterior(colorExterior & 0xFFFFFF);
				technique.setColorOutline(colorOutline & 0xFFFFFF);
				technique.setAnimation("");

				KiAttackData.SecondaryEffectType parsedSecondary;
				try { parsedSecondary = KiAttackData.SecondaryEffectType.valueOf(secondaryEffectType); }
				catch (Exception ignored) { parsedSecondary = KiAttackData.SecondaryEffectType.NONE; }
				technique.setSecondaryEffectType(parsedSecondary);
				if (affectedStat == null || affectedStat.isEmpty()) technique.setAffectedStat(null);
				else try { technique.setAffectedStat(KiAttackData.AffectedStat.valueOf(affectedStat)); }
				catch (Exception ignored) { technique.setAffectedStat(null); }
				technique.setSecondaryIntensity(secondaryIntensity);
				technique.setSecondaryDuration(secondaryDuration);

				technique.calculateDerivedValues();

				data.getTechniques().unlockTechnique(technique);
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
		return true;
	}

	private static boolean allowsUtilityForType(KiAttackData.KiType type) {
		return type == KiAttackData.KiType.SHIELD || type == KiAttackData.KiType.AREA;
	}
}

