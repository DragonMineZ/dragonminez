package com.dragonminez.common.network.S2C;

import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.network.CompressionUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncWeaponRegistryS2C {

	private final byte[] compressedData;

	public SyncWeaponRegistryS2C(String json) {
		this.compressedData = CompressionUtil.compress(json);
	}

	public SyncWeaponRegistryS2C(FriendlyByteBuf buffer) {
		this.compressedData = buffer.readByteArray();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeByteArray(this.compressedData);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			String json = CompressionUtil.decompress(this.compressedData);
			WeaponRegistry.decodeRegistry(json);
		});
		ctx.get().setPacketHandled(true);
	}
}