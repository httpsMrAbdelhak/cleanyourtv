package com.cleanyourtv.launcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** A minimal HTTPS GET with timeouts and a size limit. Used off the main thread only. */
final class Http {
    private Http() { }

    private static final int CONNECT_MS = 8_000;
    private static final int READ_MS = 12_000;
    private static final int MAX_TEXT = 2 * 1024 * 1024;
    private static final int MAX_IMAGE = 4 * 1024 * 1024;
    private static final String AGENT = "CleanYourTV/0.2 (Android TV launcher)";

    /** The body as text. bearer may be null. Throws IOException for any answer other than 200. */
    static String text(String url, String bearer) throws IOException {
        return new String(bytes(url, bearer, MAX_TEXT), StandardCharsets.UTF_8);
    }

    static byte[] image(String url) throws IOException {
        return bytes(url, null, MAX_IMAGE);
    }

    private static byte[] bytes(String url, String bearer, int limit) throws IOException {
        if (!url.startsWith("https://")) throw new IOException("https only");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(CONNECT_MS);
            connection.setReadTimeout(READ_MS);
            connection.setRequestProperty("User-Agent", AGENT);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            if (bearer != null) connection.setRequestProperty("Authorization", "Bearer " + bearer);
            int code = connection.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            try (InputStream raw = connection.getInputStream()) {
                InputStream in = "gzip".equalsIgnoreCase(connection.getContentEncoding())
                        ? new java.util.zip.GZIPInputStream(raw) : raw;
                ByteArrayOutputStream out = new ByteArrayOutputStream(16 * 1024);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    if (out.size() > limit) throw new IOException("answer too large");
                }
                return out.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }
}
