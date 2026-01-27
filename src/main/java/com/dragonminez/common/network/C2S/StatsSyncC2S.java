package com.dragonminez.common.network.C2S;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StatsSyncC2S {

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
	private final String hairColor;
	private final String bodyColor;
	private final String bodyColor2;
	private final String bodyColor3;
	private final String eye1Color;
	private final String eye2Color;
	private final String auraColor;

	public StatsSyncC2S(Character character) {
		this.raceName = character.getRace();
		this.gender = character.getGender();
		this.characterClass = character.getCharacterClass();
		this.hairId = character.getHairId();
		this.customHair = character.getHairBase();
		this.bodyType = character.getBodyType();
		this.eyesType = character.getEyesType();
		this.noseType = character.getNoseType();
		this.mouthType = character.getMouthType();
		this.tattooType = character.getTattooType();
		this.hairColor = character.getHairColor();
		this.bodyColor = character.getBodyColor();
		this.bodyColor2 = character.getBodyColor2();
		this.bodyColor3 = character.getBodyColor3();
		this.eye1Color = character.getEye1Color();
		this.eye2Color = character.getEye2Color();
		this.auraColor = character.getAuraColor();
	}

	public static void encode(StatsSyncC2S msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.raceName);
		buf.writeUtf(msg.gender);
		buf.writeUtf(msg.characterClass);
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
		buf.writeUtf(msg.hairColor);
		buf.writeUtf(msg.bodyColor);
		buf.writeUtf(msg.bodyColor2);
		buf.writeUtf(msg.bodyColor3);
		buf.writeUtf(msg.eye1Color);
		buf.writeUtf(msg.eye2Color);
		buf.writeUtf(msg.auraColor);
	}

	public static StatsSyncC2S decode(FriendlyByteBuf buf) {
		String raceName = buf.readUtf();
		String gender = buf.readUtf();
		String characterClass = buf.readUtf();
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
		String hairColor = buf.readUtf();
		String bodyColor = buf.readUtf();
		String bodyColor2 = buf.readUtf();
		String bodyColor3 = buf.readUtf();
		String eye1Color = buf.readUtf();
		String eye2Color = buf.readUtf();
		String auraColor = buf.readUtf();

		return new StatsSyncC2S(
			raceName, gender, characterClass, hairId, customHair, bodyType, eyesType,
			noseType, mouthType, tattooType, hairColor, bodyColor, bodyColor2, bodyColor3,
			eye1Color, eye2Color, auraColor
		);
	}

	private StatsSyncC2S(String raceName, String gender, String characterClass, int hairId, CustomHair customHair, int bodyType, int eyesType,
	                     int noseType, int mouthType, int tattooType, String hairColor, String bodyColor, String bodyColor2, String bodyColor3,
	                     String eye1Color, String eye2Color, String auraColor) {
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
		this.hairColor = hairColor;
		this.bodyColor = bodyColor;
		this.bodyColor2 = bodyColor2;
		this.bodyColor3 = bodyColor3;
		this.eye1Color = eye1Color;
		this.eye2Color = eye2Color;
		this.auraColor = auraColor;
	}

	public static void handle(StatsSyncC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var character = data.getCharacter();

				character.setRace(msg.raceName);
				character.setGender(msg.gender);
				character.setCharacterClass(msg.characterClass);
				character.setHairId(msg.hairId);
				character.setHairBase(msg.customHair);
				character.setBodyType(msg.bodyType);
				character.setEyesType(msg.eyesType);
				character.setNoseType(msg.noseType);
				character.setMouthType(msg.mouthType);
				character.setTattooType(msg.tattooType);
				character.setHairColor(msg.hairColor);
				character.setBodyColor(msg.bodyColor);
				character.setBodyColor2(msg.bodyColor2);
				character.setBodyColor3(msg.bodyColor3);
				character.setEye1Color(msg.eye1Color);
				character.setEye2Color(msg.eye2Color);
				character.setAuraColor(msg.auraColor);

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
