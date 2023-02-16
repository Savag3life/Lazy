package life.savag3.lazy.utils;

import com.google.gson.Gson;
import lombok.NonNull;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class Persist {

    private final Gson gson;

    public Persist(@NonNull Gson gson) {
        this.gson = gson;
    }

    public static @NonNull String getName(@NonNull Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    public static @NonNull String getName(@NonNull Object o) {
        return getName(o.getClass());
    }

    public static @NonNull String getName(@NonNull Type type) {
        return getName(type.getClass());
    }

    public @NonNull File getFile(@NonNull String name) {
        return new File(name + ".json");
    }

    public @NonNull File getFile(@NonNull Class<?> clazz) {
        return getFile(getName(clazz));
    }

    public @NonNull File getFile(@NonNull Object obj) {
        return getFile(getName(obj));
    }

    @SuppressWarnings("UnusedReturnValue")
    public <T> T loadOrSaveDefault(@NonNull T def, @NonNull Class<T> clazz, @NonNull String name) {
        return loadOrSaveDefault(def, clazz, getFile(name));
    }

    public <T> T loadOrSaveDefault(@NonNull T def, @NonNull Class<T> clazz, @NonNull File file) {
        if (!file.exists()) {
            System.out.println("Creating default: " + file);
            this.save(def, file);
            return def;
        }

        final T loaded = this.load(clazz, file);

        if (loaded == null) {
            System.out.println("Using default as I failed to load: " + file);
            final File backup = new File(file.getPath() + "_bad");
            if (backup.exists())
                if (!backup.delete()) {
                    System.out.println("Failed to delete backup: " + backup);
                }

            System.out.println("Backing up copy of bad file to: " + backup);
            if (!file.renameTo(backup)) {
                System.out.println("Failed to backup: " + file);
            }
            return def;
        }
        return loaded;
    }

    public void save(@NonNull Object instance) {
        save(instance, getFile(instance));
    }

    public void save(@NonNull Object instance, @NonNull String name) { save(instance, getFile(name)); }

    public void save(@NonNull Object instance, @NonNull File file) {
        try {
            Files.writeString(file.toPath(), this.gson.toJson(instance));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> T load(@NonNull Class<T> clazz) {
        return load(clazz, getFile(clazz));
    }

    public <T> T load(@NonNull Class<T> clazz, @NonNull String name) {
        return load(clazz, getFile(name));
    }

    public <T> T load(@NonNull Class<T> clazz, @NonNull File file) {
        byte[] content = null;
        try {
            content = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (content == null) return null;
        try { return this.gson.fromJson(new String(content), clazz); }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T load(@NonNull Type typeOfT, @NonNull String name) {
        return (T) load(typeOfT, getFile(name));
    }

    @SuppressWarnings("unchecked")
    public <T> T load(@NonNull Type typeOfT, @NonNull File file) {
        byte[] content = null;
        try {
            content = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (content == null) return null;
        try {
            return (T) this.gson.fromJson(new String(content), typeOfT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
