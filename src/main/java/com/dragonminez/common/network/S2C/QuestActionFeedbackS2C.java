package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class QuestActionFeedbackS2C {
    private final Component message;

    public QuestActionFeedbackS2C(Component message) {
        this.message = message == null ? Component.empty() : message;
    }

    public QuestActionFeedbackS2C(FriendlyByteBuf buf) {
        this.message = buf.readJsonWithCodec(ComponentSerialization.CODEC);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeJsonWithCodec(ComponentSerialization.CODEC, message);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleQuestActionFeedback(message)));
        context.setPacketHandled(true);
    }
}
