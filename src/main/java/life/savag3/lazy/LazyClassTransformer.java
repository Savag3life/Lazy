package life.savag3.lazy;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.function.Predicate;

/**
 * Class transformer to handle method emptying and field exclusion
 *
 * @author Jacob C (Savag3life)
 * @since 2023-09-04
 */
public class LazyClassTransformer {

    private final ClassWriter writer;
    private final ClassReader reader;
    private final ClassNode node;

    public LazyClassTransformer(byte[] bytes) {
        this.writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        this.reader = new ClassReader(bytes);
        this.node = new ClassNode();
    }

    /**
     * Handle transforming the class file. We'll first check for method options & then field options.
     *
     * @return The transformed class file bytes
     */
    public byte[] transform() {
        reader.accept(node, 0);

        if (!Config.INCLUDE_PRIVATE_METHODS) {
            node.methods.removeIf(
                    methodNode -> (methodNode.access & Opcodes.ACC_PRIVATE) != 0 && checkMethodAnnotations(methodNode)
            );
        }

        if (!Config.INCLUDE_NATIVE_METHODS) {
            node.methods.removeIf(
                    methodNode -> (methodNode.access & Opcodes.ACC_NATIVE) != 0 && checkMethodAnnotations(methodNode)
            );
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
                    resolveReturnType(method, list);
                    method.instructions.add(list);
                }
            }
        }

        if (!Config.INCLUDE_PUB_STATIC_FIELDS) {
            node.fields.removeIf(
                    fieldNode ->
                            (fieldNode.access & Opcodes.ACC_STATIC) != 0 &&
                                    (fieldNode.access & Opcodes.ACC_PUBLIC) != 0 && checkFieldAnnotations(fieldNode)
            );
        }

        if (!Config.INCLUDE_PUB_NON_STATIC_FIELDS) {
            node.fields.removeIf(
                    fieldNode ->
                            (fieldNode.access & Opcodes.ACC_STATIC) == 0 &&
                                    (fieldNode.access & Opcodes.ACC_PUBLIC) != 0 && checkFieldAnnotations(fieldNode)
            );
        }

        if (!Config.INCLUDE_PRI_NON_STATIC_FIELDS) {
            node.fields.removeIf(
                    fieldNode ->
                            (fieldNode.access & Opcodes.ACC_STATIC) == 0 &&
                                    (fieldNode.access & Opcodes.ACC_PRIVATE) != 0 && checkFieldAnnotations(fieldNode)
            );
        }

        if (!Config.INCLUDE_PRI_STATIC_FIELDS) {
            this.node.fields.removeIf(
                    fieldNode -> (fieldNode.access & Opcodes.ACC_STATIC) != 0 &&
                            (fieldNode.access & Opcodes.ACC_PRIVATE) != 0 && checkFieldAnnotations(fieldNode)
            );
        }

        this.node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Check if a given method node contains any of the retention annotations
     * listed in the config
     *
     * @param node MethodNode to check
     * @return boolean if the method contains any of the annotations we want to keep
     */
    private boolean checkMethodAnnotations(MethodNode node) {
        return checkAnnotations(node.visibleAnnotations) && checkAnnotations(node.invisibleAnnotations);
    }

    /**
     * Check if the given field node contains any of the retention annotations
     * listed in the config
     *
     * @param node FieldNode to check
     * @return boolean if the field contains any of the annotations we want to keep
     */
    private boolean checkFieldAnnotations(FieldNode node) {
        return checkAnnotations(node.visibleAnnotations) && checkAnnotations(node.invisibleAnnotations);
    }

    /**
     * Check if the given list of annotations contains any of the retention annotations
     * we want to keep
     *
     * @param nodes List of AnnotationNodes to check
     * @return boolean if the list contains any of the annotations we want to keep
     */
    private boolean checkAnnotations(List<AnnotationNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return false;
        return nodes.stream().noneMatch(annotation -> Config.RETENTION_ANNOTATIONS.contains(annotation.desc));
    }

    /**
     * Resolve the return type for the given node type along with a default return
     * so that decompilers don't complain about missing returns
     *
     * @param node MethodNode to resolve
     * @param list InsnList to add to
     */
    private void resolveReturnType(MethodNode node, InsnList list) {
        Type type = Type.getType(node.desc);
        switch (type.getReturnType().getDescriptor()) {
            case "I" -> {
                list.add(new InsnNode(Opcodes.ICONST_0));
                list.add(new InsnNode(Opcodes.IRETURN));
            }
            case "J" -> {
                list.add(new InsnNode(Opcodes.LCONST_0));
                list.add(new InsnNode(Opcodes.LRETURN));
            }
            case "F" -> {
                list.add(new InsnNode(Opcodes.FCONST_0));
                list.add(new InsnNode(Opcodes.FRETURN));
            }
            case "D" -> {
                list.add(new InsnNode(Opcodes.DCONST_0));
                list.add(new InsnNode(Opcodes.DRETURN));
            }
            case "V" -> {
                list.add(new InsnNode(Opcodes.RETURN));
            }
            default -> {
                list.add(new InsnNode(Opcodes.ACONST_NULL));
                list.add(new InsnNode(Opcodes.ARETURN));
            }
        }
    }
}
