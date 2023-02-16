package life.savag3.lazy;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

public class LazyClassTransformer {

    private final ClassWriter writer;
    private final ClassReader reader;
    private final ClassNode node;

    public LazyClassTransformer(byte[] bytes) {
        this.writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        this.reader = new ClassReader(bytes);
        this.node = new ClassNode();
    }

    public byte[] transform() {
        reader.accept(node, 0);

        if (!Config.INCLUDE_PRIVATE_METHODS) {
            node.methods.removeIf(methodNode -> (methodNode.access & Opcodes.ACC_PRIVATE) != 0);
        }

        if (!Config.INCLUDE_NATIVE_METHODS) {
            node.methods.removeIf(methodNode -> (methodNode.access & Opcodes.ACC_NATIVE) != 0);
        }

        for (MethodNode method : this.node.methods) {
            if (method.instructions.size() > 0) {

                // Clear Methods
                method.instructions.clear();
                method.tryCatchBlocks.clear();
                method.localVariables.clear();

                // Write new default return
                if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                    method.instructions.add(new InsnNode(Opcodes.RETURN));
                } else {
                    InsnList list = new InsnList();
                    int opcode = resolveReturnType(method);
                    switch (opcode) {
                        case Opcodes.IRETURN -> {
                            list.add(new InsnNode(Opcodes.ICONST_0));
                            list.add(new InsnNode(Opcodes.IRETURN));
                        }
                        case Opcodes.LRETURN -> {
                            list.add(new InsnNode(Opcodes.LCONST_0));
                            list.add(new InsnNode(Opcodes.LRETURN));
                        }
                        case Opcodes.FRETURN -> {
                            list.add(new InsnNode(Opcodes.FCONST_0));
                            list.add(new InsnNode(Opcodes.FRETURN));
                        }
                        case Opcodes.DRETURN -> {
                            list.add(new InsnNode(Opcodes.DCONST_0));
                            list.add(new InsnNode(Opcodes.DRETURN));
                        }
                        case Opcodes.RETURN -> {
                            list.add(new InsnNode(Opcodes.RETURN));
                        }
                        case Opcodes.ARETURN -> {
                            list.add(new InsnNode(Opcodes.ACONST_NULL));
                            list.add(new InsnNode(Opcodes.ARETURN));
                        }
                    }
                    method.instructions.add(list);
                }
            }
        }

        if (!Config.INCLUDE_PUB_STATIC_FIELDS) {
            node.fields.removeIf(fieldNode -> (fieldNode.access & Opcodes.ACC_STATIC) != 0 && (fieldNode.access & Opcodes.ACC_PUBLIC) != 0);
        }

        if (!Config.INCLUDE_PUB_NON_STATIC_FIELDS) {
            node.fields.removeIf(fieldNode -> (fieldNode.access & Opcodes.ACC_STATIC) == 0 && (fieldNode.access & Opcodes.ACC_PUBLIC) != 0);
        }

        if (!Config.INCLUDE_PRI_NON_STATIC_FIELDS) {
            node.fields.removeIf(fieldNode -> (fieldNode.access & Opcodes.ACC_STATIC) == 0 && (fieldNode.access & Opcodes.ACC_PRIVATE) != 0);
        }

        if (!Config.INCLUDE_PRI_STATIC_FIELDS) {
            this.node.fields.removeIf(field -> (field.access & Opcodes.ACC_STATIC) != 0 && (field.access & Opcodes.ACC_PRIVATE) != 0);
        }

        this.node.accept(writer);
        return writer.toByteArray();
    }

    private int resolveReturnType(MethodNode node) {
        Type type = Type.getType(node.desc);
        if (type.getReturnType() == Type.INT_TYPE) return Opcodes.IRETURN;
        if (type.getReturnType() == Type.LONG_TYPE) return Opcodes.LRETURN;
        if (type.getReturnType() == Type.FLOAT_TYPE) return Opcodes.FRETURN;
        if (type.getReturnType() == Type.DOUBLE_TYPE) return Opcodes.DRETURN;
        if (type.getReturnType() == Type.VOID_TYPE) return Opcodes.RETURN;
        return Opcodes.ARETURN;
    }

}
