package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DebugFileS2C {
	private static final int MAX_COMPRESSED_BYTES = 1000 * 1000;
	private static final int MAX_TEXT_BYTES = 16 * 1024 * 1024;

	private final String playerName;
	private final byte[] compressed;

	public DebugFileS2C(String playerName, String text) {
		this.playerName = playerName;
		this.compressed = compress(text);
	}

	public boolean fitsInPacket() {
		return compressed.length <= MAX_COMPRESSED_BYTES;
	}

	public DebugFileS2C(FriendlyByteBuf buf) {
		this.playerName = buf.readUtf(64);
		this.compressed = buf.readByteArray(MAX_COMPRESSED_BYTES);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(playerName, 64);
		buf.writeByteArray(compressed);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleDebugFile(playerName, decompress(compressed))));
		context.setPacketHandled(true);
	}

	private static byte[] compress(String text) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			gzip.write(text.getBytes(StandardCharsets.UTF_8));
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
		return out.toByteArray();
	}

	private static String decompress(byte[] data) {
		try (InputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
			byte[] bytes = gzip.readNBytes(MAX_TEXT_BYTES + 1);
			if (bytes.length > MAX_TEXT_BYTES) return null;
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (IOException exception) {
			return null;
		}
	}
}
