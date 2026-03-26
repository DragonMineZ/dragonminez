package com.dragonminez.common.network.S2C;

import com.dragonminez.client.gui.quest.StoryToast;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PartyInviteToastS2C {
    private final String inviterName;

    public PartyInviteToastS2C(String inviterName) {
        this.inviterName = inviterName == null ? "" : inviterName;
    }

    public PartyInviteToastS2C(FriendlyByteBuf buf) {
        this.inviterName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(inviterName);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.getToasts().addToast(new StoryToast(
                    Component.translatable("toast.dragonminez.party.invite.title"),
                    Component.translatable("toast.dragonminez.party.invite.desc", Component.literal(inviterName)),
                    StoryToast.Tone.INFO
            ));
        }));
        context.setPacketHandled(true);
    }
}
