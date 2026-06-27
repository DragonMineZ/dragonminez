package com.dragonminez.common.combat.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionHelper {
    public static String gzipCompress(String uncompressed) {
        if (uncompressed == null || uncompressed.isEmpty()) {
            return uncompressed;
        }
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(uncompressed.getBytes());
            gzipStream.close();
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress string", e);
        }
    }

    public static String gzipDecompress(String compressed) {
        if (compressed == null || compressed.isEmpty()) {
            return compressed;
        }
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(compressed));
             GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
             ByteArrayOutputStream resultStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzipStream.read(buffer)) > 0) {
                resultStream.write(buffer, 0, length);
            }
            return resultStream.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress string", e);
        }
    }
}
