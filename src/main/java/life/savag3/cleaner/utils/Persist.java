package life.savag3.cleaner.utils;


import com.google.gson.Gson;

import java.io.File;
import java.lang.reflect.Type;

public class Persist {

    private Gson gson;
    private DiskUtils disk;

    public Persist(Gson gson, DiskUtils disk) {
        this.gson = gson;
        this.disk = disk;
    }

    public static String getName(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    public static String getName(Object o) {
        return getName(o.getClass());
    }

    public static String getName(Type type) {
        return getName(type.getClass());
    }

    public File getFile(String name) {
        return new File(name + ".json");
    }

    public File getFile(Class<?> clazz) {
        return getFile(getName(clazz));
    }

    public File getFile(Object obj) {
        return getFile(getName(obj));
    }

    public File getFile(Type type) {
        return getFile(getName(type));
    }

    public <T> T loadOrSaveDefault(T def, Class<T> clazz) {
        return loadOrSaveDefault(def, clazz, getFile(clazz));
    }

    public <T> T loadOrSaveDefault(T def, Class<T> clazz, String name) {
        return loadOrSaveDefault(def, clazz, getFile(name));
    }

    public <T> T loadOrSaveDefault(T def, Class<T> clazz, File file) {
        if (!file.exists()) {
            System.out.println("Creating default: " + file);
            this.save(def, file);
            return def;
        }

        T loaded = this.load(clazz, file);

        if (loaded == null) {
            System.out.println("Using default as I failed to load: " + file);
            File backup = new File(file.getPath() + "_bad");
            if (backup.exists()) backup.delete();
            System.out.println("Backing up copy of bad file to: " + backup);
            file.renameTo(backup);
            return def;
        }
        return loaded;
    }

    public void save(Object instance) {
        save(instance, getFile(instance));
    }

    public void save(Object instance, String name) { save(instance, getFile(name)); }

    public void save(Object instance, File file) {
        this.disk.safeWriteContent(file, this.gson.toJson(instance));
    }

    public <T> T load(Class<T> clazz) {
        return load(clazz, getFile(clazz));
    }

    public <T> T load(Class<T> clazz, String name) {
        return load(clazz, getFile(name));
    }

    public <T> T load(Class<T> clazz, File file) {
        String content = this.disk.safeReadContent(file);
        if (content == null) return null;
        try { return this.gson.fromJson(content, clazz); }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    public <T> T load(Type typeOfT, String name) {
        return (T) load(typeOfT, getFile(name));
    }

    public <T> T load(Type typeOfT, File file) {
        String content = this.disk.safeReadContent(file);
        if (content == null) return null;
        try { return (T) this.gson.fromJson(content, typeOfT); }
        catch (Exception ex) { System.out.println(ex.getMessage()); }
        return null;
    }

}
