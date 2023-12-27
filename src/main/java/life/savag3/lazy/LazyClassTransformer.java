package life.savag3.lazy;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
                    methodNode -> (methodNode.access & Opcodes.ACC_PRIVATE) != 0 &&
                            checkMethodAnnotations(methodNode)
            );
        }

        if (!Config.INCLUDE_NATIVE_METHODS) {
            node.methods.removeIf(
                    methodNode -> (methodNode.access & Opcodes.ACC_NATIVE) != 0 &&
                            checkMethodAnnotations(methodNode)
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

                    // Handle Jetbrains contracts.
                    if (methodRequiresContract(method) &&
                            Config.DO_JETBRAINS_CONTRACTS) {
                        handleJetbrainsAnnotation(method);
                    }
                }
            }
        }

        if (!Config.INCLUDE_PUB_STATIC_FIELDS) {
            node.fields.removeIf(
                    fieldNode ->
                            (fieldNode.access & Opcodes.ACC_STATIC) != 0 &&
                                    (fieldNode.access & Opcodes.ACC_PUBLIC) != 0 &&
                                    checkFieldAnnotations(fieldNode)
            );
        }

        if (!Config.INCLUDE_PUB_NON_STATIC_FIELDS) {
            node.fields.removeIf(
                    fieldNode ->
                            (fieldNode.access & Opcodes.ACC_STATIC) == 0 &&
                                    (fieldNode.access & Opcodes.ACC_PUBLIC) != 0 &&
                                    checkFieldAnnotations(fieldNode)
            );
        }

        if (!Config.INCLUDE_PRI_NON_STATIC_FIELDS) {
            node.fields.removeIf(
                    fieldNode ->
                            (fieldNode.access & Opcodes.ACC_STATIC) == 0 &&
                                    (fieldNode.access & Opcodes.ACC_PRIVATE) != 0 &&
                                    checkFieldAnnotations(fieldNode)
            );
        }

        if (!Config.INCLUDE_PRI_STATIC_FIELDS) {
            this.node.fields.removeIf(
                    fieldNode -> (fieldNode.access & Opcodes.ACC_STATIC) != 0 &&
                            (fieldNode.access & Opcodes.ACC_PRIVATE) != 0 &&
                            checkFieldAnnotations(fieldNode)
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

    /**
     * Append a @Contract(_,_->!null) contact to any methods which may now
     * return a null value due to default return values.
     * Helps provide better intellisense to intellij IDE's & intellij errors for "results may be null"
     * @param method MethodNode to append the contract to
     */
    private void handleJetbrainsAnnotation(MethodNode method) {
        AnnotationNode contract = new AnnotationNode("Lorg/jetbrains/annotations/Contract;");
        contract.values = new ArrayList<>();
        contract.values.add("value");

        int paramCount = resolveParamCount(method.desc);
        StringBuilder contractValue = new StringBuilder();
        if (paramCount > 0) {
            IntStream.range(0, paramCount).forEach(x -> contractValue.append("_,"));
        }

        contract.values.add((paramCount > 0 ? contractValue.substring(0, contractValue.length() - 1) : "") + "->!null");

        if (method.invisibleAnnotations == null) {
            method.invisibleAnnotations = new ArrayList<>();
        }
        method.invisibleAnnotations.add(contract);
    }

    /**
     * Resolve the number of parameters for a given method descriptor
     * @param raw Method descriptor
     * @return int number of parameters
     */
    private int resolveParamCount(String raw) {
        int count = 0;
        if (raw.startsWith("()")) return 0;
        String desc = raw.substring(raw.indexOf("(") + 1, raw.indexOf(")") + 1);
        if (desc.isBlank()) return 0;
        for (int x = 0; x < desc.length() - 1; x++) {
            char index = desc.charAt(x);

            // Match Ljava/lang/String;
            if (index == 'L') {
                count++;
                x += desc.substring(x).indexOf(";");
                continue;
            }

            // Match [Ljava/lang/String;
            if (index == '[') {
                count++;
                int z = desc.substring(x).indexOf(";");
                if (z == -1) {
                    z = desc.substring(x).indexOf(")");
                }
                x += z;
                continue;
            }

            if (index == ')') break;

            while (index != ';' && index != ')') {
                x++;
                index = desc.charAt(x);
                count++;
            }
        }

        return count;
    }

    private boolean methodRequiresContract(MethodNode node) {
        // Don't need contracts for methods that cannot be referenced
        if ((node.access & Opcodes.ACC_PRIVATE) != 0 ||
                (node.access & Opcodes.ACC_PROTECTED) != 0) {
            return false;
        }

        // Only need to add contracts to methods which don't return (void | int | long | float | double)
        Type type = Type.getType(node.desc);
        String descriptor = type.getReturnType().getDescriptor();
        return !descriptor.equals("I") &&
                !descriptor.equals("J") &&
                !descriptor.equals("F") &&
                !descriptor.equals("D") &&
                !descriptor.equals("V");
    }
}
