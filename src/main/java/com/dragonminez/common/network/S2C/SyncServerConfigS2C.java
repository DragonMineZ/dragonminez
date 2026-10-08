package com.dragonminez.common.network.S2C;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.ClientPacketHandler;
import com.dragonminez.common.network.CompressionUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncServerConfigS2C {
	private final String configPath;
	private final byte[] payload;
	private final boolean reset;

	public SyncServerConfigS2C(String configPath, String jsonPayload, boolean reset) {
		this(configPath, CompressionUtil.compress(jsonPayload), reset);
	}

	public SyncServerConfigS2C(String configPath, byte[] compressedPayload, boolean reset) {
		this.configPath = configPath;
		this.payload = compressedPayload != null ? compressedPayload : new byte[0];
		this.reset = reset;
	}

	public SyncServerConfigS2C(FriendlyByteBuf buf) {
		this.configPath = buf.readUtf();
		this.payload = buf.readByteArray();
		this.reset = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(configPath);
		buf.writeByteArray(payload);
		buf.writeBoolean(reset);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (reset) ConfigManager.beginServerSyncBatch();
			String json = CompressionUtil.decompress(payload);
			if (ConfigManager.applySpecificSyncedConfig(configPath, json)) ClientPacketHandler.handleConfigSyncCommitted();
		}));
		ctx.get().setPacketHandled(true);
	}
}
