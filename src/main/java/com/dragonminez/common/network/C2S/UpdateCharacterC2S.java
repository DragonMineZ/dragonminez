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

public class UpdateCharacterC2S {
	private final String className;
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

	public UpdateCharacterC2S(Character character) {
		this.className = character.getCharacterClass();
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

	public UpdateCharacterC2S(String className, int hairId, CustomHair customHair, int bodyType, int eyesType,
							  int noseType, int mouthType, int tattooType, String hairColor, String bodyColor,
							  String bodyColor2, String bodyColor3, String eye1Color, String eye2Color, String auraColor) {
		this.className = className;
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

	public static void encode(UpdateCharacterC2S msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.className);
		buf.writeInt(msg.hairId);
		boolean hasCustomHair = msg.customHair != null;
		buf.writeBoolean(hasCustomHair);
		if (hasCustomHair) msg.customHair.writeToBuffer(buf);
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

	public static UpdateCharacterC2S decode(FriendlyByteBuf buf) {
		String className = buf.readUtf();
		int hairId = buf.readInt();
		CustomHair customHair = null;
		if (buf.readBoolean()) {
			customHair = CustomHair.readFromBuffer(buf);
		}

		return new UpdateCharacterC2S(
				className,
				hairId,
				customHair,
				buf.readInt(),
				buf.readInt(),
				buf.readInt(),
				buf.readInt(),
				buf.readInt(),
				buf.readUtf(),
				buf.readUtf(),
				buf.readUtf(),
				buf.readUtf(),
				buf.readUtf(),
				buf.readUtf(),
				buf.readUtf()
		);
	}

	public static void handle(UpdateCharacterC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				Character c = data.getCharacter();
				c.setCharacterClass(msg.className);
				c.setHairId(msg.hairId);
				if (msg.customHair != null) c.setHairBase(msg.customHair);
				c.setBodyType(msg.bodyType);
				c.setEyesType(msg.eyesType);
				c.setNoseType(msg.noseType);
				c.setMouthType(msg.mouthType);
				c.setTattooType(msg.tattooType);
				c.setHairColor(msg.hairColor);
				c.setBodyColor(msg.bodyColor);
				c.setBodyColor2(msg.bodyColor2);
				c.setBodyColor3(msg.bodyColor3);
				c.setEye1Color(msg.eye1Color);
				c.setEye2Color(msg.eye2Color);
				c.setAuraColor(msg.auraColor);
				player.refreshDimensions();
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}