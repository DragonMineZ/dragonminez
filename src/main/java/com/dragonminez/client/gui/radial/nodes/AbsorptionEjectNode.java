package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.network.C2S.RacialSlotActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AbsorptionEjectNode extends AbstractRadialNode {
	private static final long CONFIRM_WINDOW_MS = 5000;

	private final int index;
	private final RacialData.AbsorptionSlot slot;
	private long pendingConfirmSince = 0L;

	public AbsorptionEjectNode(int index, RacialData.AbsorptionSlot slot) {
		this.index = index;
		this.slot = slot;
	}

	private boolean isPendingConfirm() {
		return pendingConfirmSince > 0 && (System.currentTimeMillis() - pendingConfirmSince) < CONFIRM_WINDOW_MS;
	}

	@Override
	public Component label(StatsData stats) {
		if (isPendingConfirm()) return Component.translatable("gui.dragonminez.racial.majin.eject_confirm");
		String sourceName = slot.sourceName();
		return (sourceName == null || sourceName.isEmpty())
				? Component.translatable("gui.dragonminez.racial.majin.eject_slot", index + 1)
				: Component.literal(sourceName);
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("racial");
	}

	@Override
	public boolean visible(StatsData stats) {
		return true;
	}

	@Override
	public int labelColor(StatsData stats) {
		return isPendingConfirm() ? RED : 0xFFFFFF;
	}

	@Override
	public void onSelect(StatsData stats) {
		if (isPendingConfirm()) {
			pendingConfirmSince = 0L;
			NetworkHandler.sendToServer(new RacialSlotActionC2S(RacialSlotActionC2S.SlotAction.EJECT, index));
			playToggle(false);
		} else {
			pendingConfirmSince = System.currentTimeMillis();
			playClick();
			var player = Minecraft.getInstance().player;
			if (player != null) {
				player.displayClientMessage(Component.translatable("message.dragonminez.racial.majin.eject_confirm"), true);
			}
		}
	}
}
