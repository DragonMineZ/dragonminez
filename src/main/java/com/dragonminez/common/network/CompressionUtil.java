package com.dragonminez.common.network;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionUtil {

	public static byte[] compress(String str) {
		if (str == null || str.isEmpty()) return new byte[0];
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(str.length());
			 GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
			gzip.write(str.getBytes(StandardCharsets.UTF_8));
			gzip.close();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Error compressing packet data", e);
		}
	}

	public static String decompress(byte[] bytes) {
		if (bytes == null || bytes.length == 0) return "";
		try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
			 BufferedReader br = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} catch (IOException e) {
			throw new RuntimeException("Error decompressing packet data", e);
		}
	}
}