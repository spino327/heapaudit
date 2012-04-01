package com.foursquare.heapaudit;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class HeapMethod extends HeapUtil implements MethodVisitor {

    public HeapMethod(MethodVisitor mv,
                      String source,
                      String methodId,
                      boolean suppressAuditing,
                      boolean debugAuditing,
                      boolean traceAuditing,
                      boolean injectRecorder) {

        this.mv = new MethodAdapter(mv);

        this.source = source;

        this.id = methodId;

        this.suppressAuditing = suppressAuditing;

        this.debugAuditing = debugAuditing;

        this.traceAuditing = traceAuditing;

        this.injectRecorder = injectRecorder && HeapUtil.inject(id);

        log(debugAuditing,
            "\t{ # METHOD " + id);

    }

    public final MethodAdapter mv;

    private final String source;

    private final String id;

    private boolean suppressAuditing;

    private final boolean debugAuditing;

    private final boolean traceAuditing;

    private final boolean injectRecorder;

    public HeapVariables lvs = null;

    public AnnotationVisitor visitAnnotationDefault() {

        log(debugAuditing,
            "\t\tvisitAnnotationDefault()");

        return mv.visitAnnotationDefault();

    }

    public AnnotationVisitor visitAnnotation(String desc,
                                             boolean visible) {

        log(debugAuditing,
            "\t\tvisitAnnotation(" + desc + ", " + visible + ")");

        if (desc.equals("Lcom/foursquare/heapaudit/HeapRecorder$Suppress;")) {

            suppressAuditing = true;

        }

        return mv.visitAnnotation(desc,
                                  visible);

    }

    public AnnotationVisitor visitParameterAnnotation(int parameter,
                                                      String desc,
                                                      boolean visible) {

        log(debugAuditing,
            "\t\tvisitParameterAnnotation()");

        return mv.visitParameterAnnotation(parameter,
                                           desc,
                                           visible);
    }

    public void visitAttribute(Attribute attr) {

        log(debugAuditing,
            "\t\tvisitAttribute(" + attr.type + ")");

        mv.visitAttribute(attr);

    }

    public void visitCode() {

        mv.visitCode();

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitCode() " + source + ":" + id);

        visitEnter();

    }

    public void visitFrame(int type,
                           int nLocal,
                           Object[] local,
                           int nStack,
                           Object[] stack) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitFrame()");

        mv.visitFrame(type,
                      nLocal,
                      local,
                      nStack,
                      stack);

    }

    public void visitInsn(int opcode) {

         log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitInsn(" + opcode + ")");

        switch (opcode) {

        case Opcodes.ARETURN:

        case Opcodes.DRETURN:

        case Opcodes.FRETURN:

        case Opcodes.IRETURN:

        case Opcodes.LRETURN:

        case Opcodes.RETURN:

            visitReturn();

            break;

        }

        mv.visitInsn(opcode);

    }

    public void visitLdcInsn(Object cst) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitLdcInsn(" + cst + ")");

        mv.visitLdcInsn(cst);

    }

    public void visitIincInsn(int var,
                              int increment) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitIincInsn()");

        mv.visitIincInsn(var,
                         increment);

    }

    public void visitVarInsn(int opcode,
                             int var) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitVarInsn(" + opcode + ", " + var + ")");

        switch (opcode) {

        case Opcodes.RET:

            visitReturn();

            break;

        }

        mv.visitVarInsn(opcode,
                        var);

    }

    public void visitFieldInsn(int opcode,
                               String owner,
                               String name,
                               String desc) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitFieldInsn(" + opcode + ", " + owner + ", " + name + ", " + desc + ")");

        mv.visitFieldInsn(opcode,
                          owner,
                          name,
                          desc);

    }

    public void visitIntInsn(int opcode,
                             int operand) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitIntInsn(" + opcode + ", " + operand + ")");

        switch (opcode) {

        case Opcodes.NEWARRAY:

            HeapNEWARRAY.before(debugAuditing,
                                traceAuditing,
                                mv,
                                operand);

            break;

        default:

        }

        mv.visitIntInsn(opcode,
                        operand);

        switch (opcode) {

        case Opcodes.NEWARRAY:

            HeapNEWARRAY.after(debugAuditing,
                               traceAuditing,
                               mv,
                               operand);

            break;

        default:

        }

    }

    private int allocating = 0;

    public void visitTypeInsn(int opcode,
                              String type) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitTypeInsn(" + opcode + ", " + type + ")");

        switch (opcode) {

        case Opcodes.NEW:

            ++allocating;

            break;

        case Opcodes.ANEWARRAY:

            HeapANEWARRAY.before(debugAuditing,
                                 traceAuditing,
                                 mv,
                                 type);

            break;

        default:

        }

        mv.visitTypeInsn(opcode,
                         type);

        switch (opcode) {

        case Opcodes.ANEWARRAY:

            HeapANEWARRAY.after(debugAuditing,
                                traceAuditing,
                                mv,
                                type);

            break;

        default:

        }

    }

    public void visitMethodInsn(int opcode,
                                String owner,
                                String name,
                                String signature) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitMethodInsn(" + opcode + ", " + owner + ", " + name + ", " + signature + ")");

        switch (opcode) {

        case Opcodes.INVOKESPECIAL:

            if (name.equals("<init>")) {

                if (allocating > 0) {

                    HeapNEW.before(debugAuditing,
                                   traceAuditing,
                                   mv,
                                   lvs,
                                   signature);

                }

            }
            else if (owner.equals("java/lang/Thread") &&
                     name.equals("init")) {

                if (HeapSettings.threaded) {

                    HeapThreaded.before(debugAuditing,
                                        traceAuditing,
                                        mv,
                                        lvs,
                                        signature);

                }

            }

            break;

        case Opcodes.INVOKESTATIC:

            if (owner.equals("java/lang/reflect/Array") &&
                name.equals("newInstance")) {

                HeapNEWINSTANCE.beforeX(debugAuditing,
                                        traceAuditing,
                                        mv);

            }

            break;

        case Opcodes.INVOKEVIRTUAL:

            if (name.equals("newInstance")) {

                if (owner.equals("java/lang/Class") &&
                    signature.equals("()Ljava/lang/Object;")) {

                    HeapNEWINSTANCE.before(debugAuditing,
                                           traceAuditing,
                                           mv);

                }

            }

            break;

        default:

        }

        mv.visitMethodInsn(opcode,
                           owner,
                           name,
                           signature);

        switch (opcode) {

        case Opcodes.INVOKESPECIAL:

            if (name.equals("<init>")) {

                if (allocating > 0) {

                    --allocating;

                    HeapNEW.after(debugAuditing,
                                  traceAuditing,
                                  mv,
                                  owner);

                }

            }
            else if (owner.equals("java/lang/Object") &&
                     name.equals("clone")) {

                HeapCLONEOBJECT.after(debugAuditing,
                                      traceAuditing,
                                      mv);

            }
            else if (owner.equals("java/lang/Thread") &&
                     name.equals("init")) {

                if (HeapSettings.threaded) {

                    HeapThreaded.after(debugAuditing,
                                       traceAuditing,
                                       mv);

                }

            }

            break;

        case Opcodes.INVOKESTATIC:

            if (owner.equals("java/lang/reflect/Array") &&
                name.equals("newInstance")) {

                if (signature.equals("(Ljava/lang/Class;I)Ljava/lang/Object;")) {

                    HeapNEWINSTANCE.after(debugAuditing,
                                          traceAuditing,
                                          mv);

                }
                else if (signature.equals("(Ljava/lang/Class;[I)Ljava/lang/Object;")) {

                    HeapNEWINSTANCE.afterY(debugAuditing,
                                           traceAuditing,
                                           mv);

                }

            }

            break;

        case Opcodes.INVOKEVIRTUAL:

            if (name.equals("newInstance")) {

                if (owner.equals("java/lang/Class") &&
                    signature.equals("()Ljava/lang/Object;")) {

                    HeapNEWINSTANCE.after(debugAuditing,
                                          traceAuditing,
                                          mv);

                }
                else if (owner.equals("java/lang/reflect/Constructor") &&
                         signature.equals("([Ljava/lang/Object;)Ljava/lang/Object;")) {

                    HeapCLONEOBJECT.after(debugAuditing,
                                          traceAuditing,
                                          mv);

                }

            }
            else if (owner.startsWith("[") &&
                     name.equals("clone")) {

                HeapCLONEARRAY.after(debugAuditing,
                                     traceAuditing,
                                     mv,
                                     owner);

            }

            break;

        default:

        }

    }

    public void visitMultiANewArrayInsn(String desc,
                                        int dims) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitMultiANewArrayInsn(" + desc + ", " + dims + ")");

        mv.visitMultiANewArrayInsn(desc,
                                   dims);

        HeapMULTIARRAY.after(debugAuditing,
                             traceAuditing,
                             mv,
                             desc);

    }

    public void visitJumpInsn(int opcode,
                              Label label) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitJumpInsn(" + opcode + ", " + label + ")");

        mv.visitJumpInsn(opcode,
                         label);

    }

    public void visitLookupSwitchInsn(Label dlft,
                                      int[] keys,
                                      Label[] labels) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitLookupSwitchInsn()");

        mv.visitLookupSwitchInsn(dlft,
                                 keys,
                                 labels);

    }

    public void visitTableSwitchInsn(int min,
                                     int max,
                                     Label dlft,
                                     Label[] labels) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitTableSwitchInsn()");

        mv.visitTableSwitchInsn(min,
                                max,
                                dlft,
                                labels);

    }

    public void visitLabel(Label label) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitLabel() " + source + ":" + label);

        mv.visitLabel(label);

    }

    public void visitTryCatchBlock(Label start,
                                   Label end,
                                   Label handler,
                                   String type) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitTryCatchBlock()");

        mv.visitTryCatchBlock(start,
                              end,
                              handler,
                              type);

    }


    public void visitLocalVariable(String name,
                                   String desc,
                                   String signature,
                                   Label start,
                                   Label end,
                                   int index) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitLocalVariable(" + name + ")");

        mv.visitLocalVariable(name,
                              desc,
                              signature,
                              start,
                              end,
                              index);

    }

    public void visitLineNumber(int line,
                                Label start) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitLineNumber() " + source + ":" + start + "#" + line);

        mv.visitLineNumber(line,
                           start);

    }

    public void visitMaxs(int maxStack,
                          int maxLocals) {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitMaxs(" + maxStack + ", " + maxLocals + ")");

        lvs.declare();

        mv.visitMaxs(maxStack,
                     maxLocals);

    }

    public void visitEnd() {

        log(debugAuditing,
            traceAuditing,
            mv,
            "\t\tvisitEnd()\n\t}");

        mv.visitEnd();

    }

    private void visitEnter() {

        if (suppressAuditing) {

            // STACK: [...]
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                               "com/foursquare/heapaudit/HeapRecorder",
                               "suppress",
                               "()Ljava/lang/Object;");
            // STACK: [...|context]
            mv.visitInsn(Opcodes.POP);
            // STACK: [...]

        }
        else if (injectRecorder) {

            // STACK: [...]
            mv.visitLdcInsn(id);
            // STACK: [...|id]
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                               "com/foursquare/heapaudit/HeapUtil",
                               "register",
                               "(Ljava/lang/String;)V");
            // STACK: [...]

        }

    }

    private void visitReturn() {

        if (suppressAuditing) {

            // STACK: [...]
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                               "com/foursquare/heapaudit/HeapRecorder",
                               "unwind",
                               "()Ljava/lang/Object;");
            // STACK: [...|context]
            mv.visitInsn(Opcodes.POP);
            // STACK: [...]

        }
        else if (injectRecorder) {

            // STACK: [...]
            mv.visitLdcInsn(id);
            // STACK: [...|id]
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                               "com/foursquare/heapaudit/HeapUtil",
                               "unregister",
                               "(Ljava/lang/String;)V");
            // STACK: [...]

        }

    }

}
