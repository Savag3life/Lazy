package life.savag3.lazy.asm;

import life.savag3.lazy.Lazy;
import life.savag3.lazy.Config;
import life.savag3.lazy.utils.PackageUtils;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.objectweb.asm.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.*;

public class ClassExplorer extends ClassVisitor {

    private final ClassWriter cw;
    private String name;
    private String pack;

    private boolean cancelled;

    public ClassExplorer() {
        super(ASM8);
        this.cw = new ClassWriter(0);
    }

    @Override
    @NonNull
    public void visit(int i, int i1, String s, String s1, String s2, String[] strings) {
        name = s.split("/")[s.split("/").length - 1];
        pack = s.substring(0, s.length() - name.length());
        if (Config.VERBOSE) System.out.println(" -- name=" + s + ",extends=" + s1 + ",super=" + s2 + "{}");

        if (i1 > ACC_ABSTRACT && !Config.INCLUDE_ABSTRACT_CLASSES) {
            System.out.println("Class " + name + " flagged as abstract, continuing...");
            this.cancelled = true;
            return;
        }

        this.cancelled = PackageUtils.isBlacklistedPackage(pack);
        if (this.cancelled) return;

        cw.visit(i, i1, s, s1, s2, strings);
        Lazy.instance.getClassCount().incrementAndGet();
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        if (cancelled) return super.visitRecordComponent(name, descriptor, signature);
        if (Config.VERBOSE) System.out.println(" --E-- name=" + name + ",extends=" + descriptor + ",super=" + signature);
        return super.visitRecordComponent(name, descriptor, signature);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (cancelled) return super.visitMethod(access, name, descriptor, signature, exceptions);
        if (Config.VERBOSE) System.out.println(" --M-- name=" + name + ",extends=" + descriptor + ",super=" + signature + ",expt=" + (exceptions == null ? "" : exceptions.length > 0 ? String.join(", ", exceptions) : "") + " {}");
        if (!name.startsWith("lambda")) {
            if (access == ACC_PRIVATE || access == ACC_PRIVATE + ACC_STATIC) {
                if (!Config.INCLUDE_PRIVATE_METHODS) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }
            MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
            mv.visitInsn(RETURN);
            Lazy.instance.getMethodCount().incrementAndGet();
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FieldVisitor visitor = super.visitField(access, name, descriptor, signature, value);
        if (cancelled) return visitor;
        if (Config.VERBOSE) System.out.print(" --F-- access=" + access + ",name=" + name + ",extends=" + descriptor + ",super=" + signature + ",value=" + (value == null ? "" : value.toString()) + ";");

        switch (access) {
            case ACC_STATIC + ACC_PUBLIC:
            case ACC_STATIC + ACC_PUBLIC + ACC_FINAL:
            case ACC_STATIC + ACC_PUBLIC + ACC_TRANSIENT:
            case ACC_STATIC + ACC_PUBLIC + ACC_TRANSIENT + ACC_FINAL:
                if (Config.INCLUDE_PUB_STATIC_FIELDS) addField(access, name, descriptor, signature, value);
                break;

            case ACC_STATIC + ACC_PRIVATE:
            case ACC_STATIC + ACC_PRIVATE + ACC_FINAL:
            case ACC_STATIC + ACC_PRIVATE + ACC_TRANSIENT:
            case ACC_STATIC + ACC_PRIVATE + ACC_TRANSIENT + ACC_FINAL:
                if (Config.INCLUDE_PRI_STATIC_FIELDS) addField(access, name, descriptor, signature, value);
                break;

            case ACC_PRIVATE:
            case ACC_PRIVATE + ACC_FINAL:
            case ACC_PRIVATE + ACC_TRANSIENT:
            case ACC_PRIVATE + ACC_TRANSIENT + ACC_FINAL:
                if (Config.INCLUDE_PRI_NON_STATIC_FIELDS) addField(access, name, descriptor, signature, value);
                break;

            case ACC_PUBLIC:
            case ACC_PUBLIC + ACC_FINAL:
            case ACC_PUBLIC + ACC_TRANSIENT:
            case ACC_PUBLIC + ACC_TRANSIENT + ACC_FINAL:
                if (Config.INCLUDE_PUB_NON_STATIC_FIELDS) addField(access, name, descriptor, signature, value);
                break;

            case 16409: // ENUM access
                if (Config.INCLUDE_ENUM_DATA) {
                    if (Config.VERBOSE) System.out.print(" -- Adding ENUM data\n");
                    cw.visitField(access, name, descriptor, signature, value).visitEnd();
                    Lazy.instance.getFieldCount().incrementAndGet();
                }
                break;
            default:
                if (Config.VERBOSE) System.out.print("\n");
        }
        return visitor;
    }

    private void addField(int access, String name, String descriptor, String signature, Object value) {
        if (Config.VERBOSE) System.out.print(" -- Added field to class\n");
        cw.visitField(access, name, descriptor, signature, value).visitEnd();
        Lazy.instance.getFieldCount().incrementAndGet();
    }

    @Override
    @SneakyThrows
    public void visitEnd() {
        if (this.cancelled) return;
        byte[] bytes = this.cw.toByteArray();
        Lazy.instance.add(pack + name + ".class", bytes);
    }
}


