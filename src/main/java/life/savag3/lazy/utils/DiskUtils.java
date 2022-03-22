package life.savag3.lazy.utils;

import lombok.NonNull;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class DiskUtils {

    public static byte[] read(File file) {
        int length = (int) file.length();
        byte[] bytes = new byte[length];
        try (FileInputStream in = new FileInputStream(file)) {
            int offset = 0;
            while (offset < length) offset += in.read(bytes, offset, length - offset);
            return bytes;
        } catch (IOException ignored) {
            return null;
        }
    }

    public static byte[] read(@NonNull InputStream in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) out.write(buffer, 0, length);
            return out.toByteArray();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void write(File file, byte[] bytes) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        } catch (IOException ignored) { }
    }

    public static String toUTF8(byte[] bytes) {
        if (bytes == null) return null;
        return new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
    }

    public static byte[] toUTF8(String string) {
        if (string == null) return null;
        return string.getBytes(StandardCharsets.UTF_8);
    }
}
