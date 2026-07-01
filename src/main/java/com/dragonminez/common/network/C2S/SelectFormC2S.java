package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectFormC2S {

	private final String group;
	private final String form;
	private final boolean stack;

	public SelectFormC2S(String group, String form, boolean stack) {
		this.group = group != null ? group : "";
		this.form = form != null ? form : "";
		this.stack = stack;
	}

	public SelectFormC2S(FriendlyByteBuf buffer) {
		this.group = buffer.readUtf();
		this.form = buffer.readUtf();
		this.stack = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(group);
		buffer.writeUtf(form);
		buffer.writeBoolean(stack);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (player.hasEffect(MainEffects.STUN.get())) return;
			if (group.isEmpty() || form.isEmpty()) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (stack) {
					if (!TransformationsHelper.isSelectableStackForm(data, group, form)) return;
					data.getStatus().setSelectedAction(ActionMode.STACK);
					data.getCharacter().setSelectedStackFormGroup(group);
					data.getCharacter().setSelectedStackForm(form);
				} else {
					if (!TransformationsHelper.isSelectableForm(data, group, form)) return;
					data.getStatus().setSelectedAction(ActionMode.FORM);
					data.getCharacter().setSelectedFormGroup(group);
					data.getCharacter().setSelectedForm(form);
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
