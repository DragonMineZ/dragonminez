package com.dragonminez.common.network.C2S;

import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.hair.HairSanitizer;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.wish.wishes.ReCustomizeWish;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StatsSyncC2S {

	private static final int RACE_NAME_CAP = 32;
	private static final int GENDER_CAP = 16;
	private static final int CLASS_NAME_CAP = 32;
	private static final int BONE_NAME_CAP = 64;
	private static final int COLOR_CAP = 8;

	private final String raceName;
	private final String gender;
	private final String characterClass;
	private final int hairId;
	private final CustomHair customHair;
	private final int bodyType;
	private final int eyesType;
	private final int noseType;
	private final int mouthType;
	private final int tattooType;
	private final float boobScale;
	private final String activeHeadBone;
	private final String hairColor;
	private final String bodyColor;
	private final String bodyColor2;
	private final String bodyColor3;
	private final String eye1Color;
	private final String eye2Color;
	private final String auraColor;
	private final boolean renderHairBase;

	public StatsSyncC2S(Character character) {
		this.raceName = character.getRace();
		this.gender = character.getGender();
		this.characterClass = character.getCharacterClass();
		this.hairId = character.getHairId();
		this.customHair = character.getOwnHairStyle(HairStyleSlot.BASE);
		this.bodyType = character.getBodyType();
		this.eyesType = character.getEyesType();
		this.noseType = character.getNoseType();
		this.mouthType = character.getMouthType();
		this.tattooType = character.getTattooType();
		this.boobScale = character.getBoobScale();
		this.activeHeadBone = character.getActiveHeadBone();
		this.hairColor = character.getHairColor();
		this.bodyColor = character.getBodyColor();
		this.bodyColor2 = character.getBodyColor2();
		this.bodyColor3 = character.getBodyColor3();
		this.eye1Color = character.getEye1Color();
		this.eye2Color = character.getEye2Color();
		this.auraColor = character.getAuraColor();
		this.renderHairBase = character.isRenderHairBase();
	}

	public static void encode(StatsSyncC2S msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.raceName, RACE_NAME_CAP);
		buf.writeUtf(msg.gender, GENDER_CAP);
		buf.writeUtf(msg.characterClass, CLASS_NAME_CAP);
		buf.writeInt(msg.hairId);
		boolean hasCustomHair = msg.customHair != null;
		buf.writeBoolean(hasCustomHair);
		if (hasCustomHair) {
			msg.customHair.writeToBuffer(buf);
		}
		buf.writeInt(msg.bodyType);
		buf.writeInt(msg.eyesType);
		buf.writeInt(msg.noseType);
		buf.writeInt(msg.mouthType);
		buf.writeInt(msg.tattooType);
		buf.writeFloat(msg.boobScale);
		buf.writeUtf(msg.activeHeadBone, BONE_NAME_CAP);
		buf.writeUtf(msg.hairColor, COLOR_CAP);
		buf.writeUtf(msg.bodyColor, COLOR_CAP);
		buf.writeUtf(msg.bodyColor2, COLOR_CAP);
		buf.writeUtf(msg.bodyColor3, COLOR_CAP);
		buf.writeUtf(msg.eye1Color, COLOR_CAP);
		buf.writeUtf(msg.eye2Color, COLOR_CAP);
		buf.writeUtf(msg.auraColor, COLOR_CAP);
		buf.writeBoolean(msg.renderHairBase);
	}

	public static StatsSyncC2S decode(FriendlyByteBuf buf) {
		String raceName = buf.readUtf(RACE_NAME_CAP);
		String gender = buf.readUtf(GENDER_CAP);
		String characterClass = buf.readUtf(CLASS_NAME_CAP);
		int hairId = buf.readInt();
		CustomHair customHair = null;
		if (buf.readBoolean()) {
			customHair = CustomHair.readFromBuffer(buf);
		}
		int bodyType = buf.readInt();
		int eyesType = buf.readInt();
		int noseType = buf.readInt();
		int mouthType = buf.readInt();
		int tattooType = buf.readInt();
		float boobScale = buf.readFloat();
		String activeHeadBone = buf.readUtf(BONE_NAME_CAP);
		String hairColor = buf.readUtf(COLOR_CAP);
		String bodyColor = buf.readUtf(COLOR_CAP);
		String bodyColor2 = buf.readUtf(COLOR_CAP);
		String bodyColor3 = buf.readUtf(COLOR_CAP);
		String eye1Color = buf.readUtf(COLOR_CAP);
		String eye2Color = buf.readUtf(COLOR_CAP);
		String auraColor = buf.readUtf(COLOR_CAP);
		boolean renderHairBase = buf.readBoolean();

		return new StatsSyncC2S(
				raceName, gender, characterClass, hairId, customHair, bodyType, eyesType,
				noseType, mouthType, tattooType, boobScale, activeHeadBone, hairColor, bodyColor, bodyColor2, bodyColor3,
				eye1Color, eye2Color, auraColor, renderHairBase
		);
	}

	private StatsSyncC2S(String raceName, String gender, String characterClass, int hairId, CustomHair customHair, int bodyType, int eyesType,
	                     int noseType, int mouthType, int tattooType, float boobScale, String activeHeadBone, String hairColor, String bodyColor, String bodyColor2, String bodyColor3,
	                     String eye1Color, String eye2Color, String auraColor, boolean renderHairBase) {
		this.raceName = raceName;
		this.gender = gender;
		this.characterClass = characterClass;
		this.hairId = hairId;
		this.customHair = customHair;
		this.bodyType = bodyType;
		this.eyesType = eyesType;
		this.noseType = noseType;
		this.mouthType = mouthType;
		this.tattooType = tattooType;
		this.boobScale = boobScale;
		this.activeHeadBone = activeHeadBone;
		this.hairColor = hairColor;
		this.bodyColor = bodyColor;
		this.bodyColor2 = bodyColor2;
		this.bodyColor3 = bodyColor3;
		this.eye1Color = eye1Color;
		this.eye2Color = eye2Color;
		this.auraColor = auraColor;
		this.renderHairBase = renderHairBase;
	}

	public static void handle(StatsSyncC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			if (!ConfigManager.isRaceLoaded(msg.raceName)) {
				LogUtil.warn(com.dragonminez.Env.COMMON, "Rejected StatsSyncC2S from '{}': unknown race '{}'", player.getGameProfile().getName(), msg.raceName);
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data ->
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player));
				return;
			}

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var character = data.getCharacter();

				if (!data.getStatus().isHasCreatedCharacter()) {
					character.setRace(msg.raceName);
					character.setGender(msg.gender);
					character.setCharacterClass(msg.characterClass);
					if (ConfigManager.getRaceCharacter(msg.raceName) != null) character.setHasSaiyanTail(ConfigManager.getRaceCharacter(msg.raceName).getHasSaiyanTail());
				} else if (ReCustomizeWish.isPending(player) && ConfigManager.getRaceStats(character.getRaceName()).getAllClasses().contains(msg.characterClass)) {
					// Race/gender stay locked after creation, but the recustomization wish may switch class
					character.setCharacterClass(msg.characterClass);
				}
				character.setHairId(msg.hairId);
				if (msg.customHair != null) HairSanitizer.sanitizeAndLog(msg.customHair, HairStyleSlot.BASE, player.getGameProfile().getName());
				character.setHairStyle(HairStyleSlot.BASE, msg.customHair);
				character.setBodyType(msg.bodyType);
				character.setEyesType(msg.eyesType);
				character.setNoseType(msg.noseType);
				character.setMouthType(msg.mouthType);
				character.setTattooType(msg.tattooType);
				character.setBoobScale(msg.boobScale);
				character.setActiveHeadBone(msg.activeHeadBone);
				character.setHairColor(msg.hairColor);
				character.setBodyColor(msg.bodyColor);
				character.setBodyColor2(msg.bodyColor2);
				character.setBodyColor3(msg.bodyColor3);
				character.setEye1Color(msg.eye1Color);
				character.setEye2Color(msg.eye2Color);
				character.setAuraColor(msg.auraColor);
				character.setRenderHairBase(msg.renderHairBase);

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}