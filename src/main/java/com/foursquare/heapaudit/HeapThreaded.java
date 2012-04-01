package com.foursquare.heapaudit;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class HeapThreaded extends HeapUtil {

    // Before the init method of java/lang/Thread is called, duplicate the
    // thread object reference on the stack. Immediately after the init method
    // has been called, extend the recorders on the local thread such that the
    // child thread can continue with the same set of recorders. The top of the
    // stack when calling visitMethodInsn(INVOKESPECIAL) contains the reference
    // to the newly allocated object and all the parameter values for the method
    // arguments.

    static void before(boolean debug,
                       boolean trace,
                       MethodAdapter mv,
                       HeapVariables lvs,
                       String signature) {

        log(debug,
            trace,
            mv,
            "\tThreaded.before");

        Type[] args = Type.getArgumentTypes(signature);

        int[] vars = new int[args.length];

        Label start = new Label();

        Label end = new Label();

        mv.visitLabel(start);

        for (int i = args.length - 1; i >= 0; --i) {

            vars[i] = lvs.define(args[i],
                                 start,
                                 end);

            // STACK [...|obj|...|arg]
            mv.visitVarInsn(args[i].getOpcode(Opcodes.ISTORE),
                            vars[i]);
            // STACK [...|obj|...]

        }

        // STACK [...|obj]
        mv.visitInsn(Opcodes.DUP);
        // STACK [...|obj|obj]

        for (int i = 0; i < args.length; ++i) {

            // STACK [...|obj|obj|...]
            mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD),
                            vars[i]);
            // STACK [...|obj|obj|...|arg]

        }

        mv.visitLabel(end);

    }

    static void after(boolean debug,
                      boolean trace,
                      MethodAdapter mv) {

        log(debug,
            trace,
            mv,
            "\tThreaded.after");

        // STACK: [...|obj]
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                           "java/lang/Thread",
                           "getId",
                           "()J");
        // STACK: [...|id]
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           "com/foursquare/heapaudit/HeapUtil",
                           "extend",
                           "(J)V");
        // STACK: [...]

    }

}
