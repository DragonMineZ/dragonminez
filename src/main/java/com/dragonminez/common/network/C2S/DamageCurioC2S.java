package com.dragonminez.common.network.C2S;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

public class DamageCurioC2S {
	private final String slotId;
	private final int slotIndex;
	private final int damageAmount;

	public DamageCurioC2S(String slotId, int slotIndex, int damageAmount) {
		this.slotId = slotId == null ? "" : slotId.trim();
		this.slotIndex = slotIndex;
		this.damageAmount = damageAmount;
	}

	public DamageCurioC2S(FriendlyByteBuf buf) {
		this.slotId = buf.readUtf(256);
		this.slotIndex = buf.readInt();
		this.damageAmount = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(this.slotId, 256);
		buf.writeInt(this.slotIndex);
		buf.writeInt(this.damageAmount);
	}

	public boolean handle(Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || slotId.isEmpty()) return;

			if (damageAmount <= 0) return;

			CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
				var handler = inv.getCurios().get(slotId);
				if (handler != null) {
					if (slotIndex < 0 || slotIndex >= handler.getStacks().getSlots()) return;
					ItemStack stack = handler.getStacks().getStackInSlot(slotIndex);

					if (!stack.isEmpty() && stack.isDamageableItem()) {
						stack.hurtAndBreak(damageAmount, player, (entity) -> {});

						if (stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage()) handler.getStacks().setStackInSlot(slotIndex, ItemStack.EMPTY);
					}
				}
			});
		});
		context.setPacketHandled(true);
		return true;
	}
}