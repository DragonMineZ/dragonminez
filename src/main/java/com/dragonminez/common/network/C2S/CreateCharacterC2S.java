package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.util.MutantManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateCharacterC2S {

	private final String raceName;
	private final String className;
	private final String gender;
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

	public CreateCharacterC2S(Character character) {
		this.raceName = character.getRace();
		this.className = character.getCharacterClass();
		this.gender = character.getGender();
		this.hairId = character.getHairId();
		this.customHair = character.getHairBase();
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
	}

	private CreateCharacterC2S(String raceName, String className, String gender, int hairId, CustomHair customHair, int bodyType, int eyesType,
							   int noseType, int mouthType, int tattooType, float boobScale, String activeHeadBone, String hairColor, String bodyColor, String bodyColor2, String bodyColor3,
							   String eye1Color, String eye2Color, String auraColor) {
		this.raceName = raceName;
		this.className = className;
		this.gender = gender;
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
	}

	private static final int RACE_NAME_CAP = 32;
	private static final int CLASS_NAME_CAP = 32;
	private static final int GENDER_CAP = 16;
	private static final int BONE_NAME_CAP = 64;
	private static final int COLOR_CAP = 8;

	public static void encode(CreateCharacterC2S msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.raceName, RACE_NAME_CAP);
		buf.writeUtf(msg.className, CLASS_NAME_CAP);
		buf.writeUtf(msg.gender, GENDER_CAP);
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
	}

	public static CreateCharacterC2S decode(FriendlyByteBuf buf) {
		String raceName = buf.readUtf(RACE_NAME_CAP);
		String className = buf.readUtf(CLASS_NAME_CAP);
		String gender = buf.readUtf(GENDER_CAP);
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

		return new CreateCharacterC2S(
				raceName, className, gender, hairId, customHair, bodyType, eyesType,
				noseType, mouthType, tattooType, boobScale, activeHeadBone, hairColor, bodyColor, bodyColor2, bodyColor3,
				eye1Color, eye2Color, auraColor
		);
	}

	public static void handle(CreateCharacterC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) {
					data.initializeWithRaceAndClass(msg.raceName, msg.className, msg.gender,
							msg.hairId, msg.customHair, msg.bodyType, msg.eyesType, msg.noseType, msg.mouthType, msg.tattooType, msg.boobScale,
							msg.activeHeadBone, msg.hairColor, msg.bodyColor, msg.bodyColor2, msg.bodyColor3,
							msg.eye1Color, msg.eye2Color, msg.auraColor);
					data.getCharacter().setSelectedFormGroup(TransformationsHelper.getGroupWithFirstAvailableForm(data));
					data.getCharacter().setSelectedForm(TransformationsHelper.getFirstAvailableForm(data));
					data.getCharacter().setSelectedStackFormGroup(TransformationsHelper.getGroupWithFirstAvailableStackForm(data));
					data.getCharacter().setSelectedStackForm(TransformationsHelper.getFirstAvailableStackForm(data));
					player.refreshDimensions();
					player.setHealth(player.getMaxHealth());
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
					MutantManager.rollForPlayer(player, data);
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}
}


