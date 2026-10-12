package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

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
	private static final int MAX_COMPRESSED_BYTES = 4 * 1024 * 1024;
	private static final int MAX_TEXT_BYTES = 16 * 1024 * 1024;

	private final String playerName;
	private final byte[] compressed;

	public DebugFileS2C(String playerName, String text) {
		this.playerName = playerName;
		this.compressed = compress(text);
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
			byte[] bytes = gzip.readNBytes(MAX_TEXT_BYTES);
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (IOException exception) {
			return "Failed to decode debug data: " + exception.getMessage();
		}
	}
}
