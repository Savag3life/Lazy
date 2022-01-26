package life.savag3.lazy.utils;

import life.savag3.lazy.Lazy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DiskUtils {

    private HashMap<String, Lock> locks = new HashMap<>();

    public byte[] readBytes(File file) throws IOException {
        int length = (int) file.length();
        byte[] output = new byte[length];
        InputStream in = new FileInputStream(file);
        int offset = 0;
        while (offset < length) offset += in.read(output, offset, (length - offset));
        in.close();
        return output;
    }

    public byte[] getBytes(InputStream in) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
            byte[] buffer = new byte[0xFFFF];
            for (int len; (len = in.read(buffer)) != -1;)
                os.write(buffer, 0, len);
            os.flush();
            return os.toByteArray();
        }
    }

    public void writeBytes(File file, byte[] bytes) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        out.write(bytes);
        out.close();
    }

    public void write(File file, String content) throws IOException {
        writeBytes(file, utf8(content));
    }

    public String read(File file) throws IOException {
        return utf8(readBytes(file));
    }

    public void safeWriteContent(File file, String content) {
        safeWriteContent(file, content, true);
    }


    public void safeWriteContent(File file, String content, boolean sync) {
        String name = file.getName();

        if (!file.exists())
            try { file.createNewFile(); } catch (IOException er) { }

        final Lock lock;

        if (locks.containsKey(name)) lock = locks.get(name);
        else {
            ReadWriteLock rwl = new ReentrantReadWriteLock();
            lock = rwl.writeLock();
            locks.put(name, lock);
        }

        if (sync) {
            lock.lock();
            try { write(file, content); }
            catch (IOException e) { e.printStackTrace(); }
            finally { lock.unlock(); }
        } else {
            Lazy.instance.getAsync().submit(() -> {
                lock.lock();
                try { write(file, content); }
                catch (IOException e) { e.printStackTrace(); }
                finally { lock.unlock(); }
            });
        }
    }

    public String safeReadContent(File file) {
        try { return read(file);
        } catch (IOException e) { return null; }
    }

    public byte[] utf8(String string) { return string.getBytes(StandardCharsets.UTF_8); }

    public String utf8(byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8); }

    public void copyFile(InputStream in, File out) throws Exception {
        try (InputStream fis = in; FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) fos.write(buf, 0, i);
        }
    }
}
