package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateCharacterC2S {

    private final String raceName;
    private final String className;
    private final String gender;
    private final int hairId;
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

    public CreateCharacterC2S(Character character) {
        this.raceName = character.getRace();
        this.className = character.getCharacterClass();
        this.gender = character.getGender();
        this.hairId = character.getHairId();
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

    private CreateCharacterC2S(String raceName, String className, String gender, int hairId, int bodyType, int eyesType,
                               int noseType, int mouthType, int tattooType, String hairColor, String bodyColor, String bodyColor2, String bodyColor3,
                               String eye1Color, String eye2Color, String auraColor) {
        this.raceName = raceName;
        this.className = className;
        this.gender = gender;
        this.hairId = hairId;
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

    public static void encode(CreateCharacterC2S msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.raceName);
        buf.writeUtf(msg.className);
        buf.writeUtf(msg.gender);
        buf.writeInt(msg.hairId);
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

    public static CreateCharacterC2S decode(FriendlyByteBuf buf) {
        return new CreateCharacterC2S(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
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

    public static void handle(CreateCharacterC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (!data.getStatus().hasCreatedCharacter()) {
                    data.initializeWithRaceAndClass(msg.raceName, msg.className, msg.gender,
                            msg.hairId, msg.bodyType, msg.eyesType, msg.noseType, msg.mouthType, msg.tattooType,
                            msg.hairColor, msg.bodyColor, msg.bodyColor2, msg.bodyColor3,
                            msg.eye1Color, msg.eye2Color, msg.auraColor);

                    LogUtil.info(Env.COMMON, "Jugador {} creó personaje: Raza={}, Clase={}, Género={}",
                            player.getName().getString(), msg.raceName, msg.className, msg.gender);

                    NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}


