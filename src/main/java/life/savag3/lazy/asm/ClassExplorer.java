package life.savag3.lazy.asm;

import life.savag3.lazy.Lazy;
import life.savag3.lazy.Config;
import life.savag3.lazy.utils.PackageUtils;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.objectweb.asm.*;

import java.util.Arrays;

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
        if (Config.VERBOSE)
            System.out.println(" -- name = " + s + ", extends = " + s1 + ", super = " + s2);

        if (i1 > ACC_ABSTRACT && !Config.INCLUDE_ABSTRACT_CLASSES) {
            System.out.println("Class " + name + " flagged as abstract, continuing...");
            this.cancelled = true;
            return;
        }

        cw.visit(i, i1, s, s1, s2, strings);
        Lazy.instance.getClassCount().incrementAndGet();
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        if(cancelled)
            return;

        if (Config.VERBOSE)
            System.out.println(" --OC-- owner = " + owner + ", name = " + name + ", descriptor = " + descriptor);

        cw.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if(cancelled)
            return;

        if (Config.VERBOSE)
            System.out.println(" --IC-- name = " + name + ", outerName = " + outerName + ", innerName = " + innerName + ", access = " + access);

        cw.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        if(cancelled)
            return;

        if (Config.VERBOSE)
            System.out.println(" --PC-- permittedSubclass = " + permittedSubclass);

        cw.visitPermittedSubclass(permittedSubclass);
    }

    @Override
    public void visitNestMember(String nestMember) {
        if(cancelled)
            return;

        if (Config.VERBOSE)
            System.out.println(" --NM-- nestMember = " + nestMember);

        cw.visitNestMember(nestMember);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if(cancelled)
            return super.visitAnnotation(descriptor, visible);

        if (Config.VERBOSE)
            System.out.println("descriptor = " + descriptor + ", visible = " + visible);

        cw.visitAnnotation(descriptor, visible);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        if(cancelled)
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);

        if (Config.VERBOSE)
            System.out.println(" --TA-- typeRef = " + typeRef + ", typePath = " + typePath + ", descriptor = " + descriptor + ", visible = " + visible);
        cw.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        if (cancelled) // Class was skipped because it was abstract
            return super.visitRecordComponent(name, descriptor, signature);
        if (Config.VERBOSE) System.out.println(" --E-- name=" + name + ",extends=" + descriptor + ",super=" + signature);
        return super.visitRecordComponent(name, descriptor, signature);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (cancelled) // Class was skipped because it was abstract
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        if (Config.VERBOSE) System.out.println(" --M-- name=" + name + ",extends=" + descriptor + ",super=" + signature + ",expt=" + (exceptions == null ? "" : exceptions.length > 0 ? String.join(", ", exceptions) : "") + " {}");

        switch (access) {
            case ACC_PUBLIC + ACC_NATIVE:
            case ACC_PUBLIC + ACC_STATIC + ACC_NATIVE:
            case ACC_PUBLIC + ACC_STATIC + ACC_NATIVE + ACC_FINAL:
            case ACC_PRIVATE + ACC_NATIVE:
            case ACC_PRIVATE + ACC_STATIC + ACC_NATIVE:
            case ACC_PRIVATE + ACC_STATIC + ACC_NATIVE + ACC_FINAL:
                if (!Config.INCLUDE_NATIVE_METHODS)
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                break;

            case ACC_PRIVATE:
            case ACC_PRIVATE + ACC_STATIC:
                if (!Config.INCLUDE_PRIVATE_METHODS)
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                break;
            default:
                MethodVisitor mv = cw.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitInsn(RETURN);
                Lazy.instance.getMethodCount().incrementAndGet();
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        if(cancelled)
            return;

        cw.visitAttribute(attribute);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FieldVisitor visitor = super.visitField(access, name, descriptor, signature, value);
        if (cancelled) // Class was skipped because it was abstract
            return visitor;
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
        if (this.cancelled) // Class was skipped because it was abstract
            return;
        byte[] bytes = this.cw.toByteArray();

        if (PackageUtils.isExempt(this.pack)) {
            if (Config.VERBOSE) System.out.println(" -- Exempt package: " + this.pack);
            return;
        }

        Lazy.instance.add(pack + name + ".class", bytes);
    }
}


