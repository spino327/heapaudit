package com.foursquare.heapaudit;

import com.sun.tools.attach.VirtualMachine;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

// When loaded statically via the java command line as a java agent, the main
// entry point is HeapAudit.premain(). When loaded dynamically, the java command
// line first invokes HeapAudit.main() which then causes the JVM to load the
// java agent (the same jar file) into the target process.

public class HeapAudit extends HeapUtil implements ClassFileTransformer {

    static {
        /*
        Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override public void run() {

                    HeapRecorder.instrumentation = null;

                }

            });
        */
    }

    // The following is the wrapper that instructs JVM to load the java agent
    // into the designated target process.

    public static void main(String[] args) throws Exception {

        StringBuffer s = new StringBuffer("-Xconditional");

        for (int i = 1; i < args.length; ++i) {

            s.append(' ');

            s.append(args[i]);

        }

        load(args[0],
             s.toString());

    }

    // The following attaches to the specified process and loads the java agent,
    // which happens to be the same jar file itself.

    public static void load(String pid,
                            String args) throws Exception {

        VirtualMachine vm = VirtualMachine.attach(pid);

        vm.loadAgent(HeapAudit.class.getProtectionDomain().getCodeSource().getLocation().getPath(),
                     args);

        vm.detach();

    }

    // The following is the entry point when loaded dynamically to inject or
    // remove recorders from the target process. 

    public static void agentmain(String args,
                                 Instrumentation instrumentation) throws UnmodifiableClassException {

        instrument(args,
                   instrumentation,
                   true);

    }

    // The following is the entry point when loaded as a java agent along with
    // the target process on the java command line.

    public static void premain(String args,
                               Instrumentation instrumentation) throws UnmodifiableClassException {

        instrument(args,
                   instrumentation,
                   false);

    }

    // The following is the implementation for instrumenting the target code.

    private static void instrument(String args,
                                   Instrumentation instrumentation,
                                   boolean dynamic) throws UnmodifiableClassException {

        HeapSettings.parse(args,
                           dynamic);
        
        HeapRecorder.isAuditing = true;

        HeapRecorder.instrumentation = instrumentation;

        ClassFileTransformer transformer = new HeapAudit();

        instrumentation.addTransformer(transformer,
                                       true);

        if (instrumentation.isRetransformClassesSupported()) {

            ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

            for (Class<?> c: instrumentation.getAllLoadedClasses()) {

                if (instrumentation.isModifiableClass(c)) {

                    classes.add(c);

                }

            }

            instrumentation.retransformClasses(classes.toArray(new Class<?>[classes.size()]));

        }

        if (dynamic) {

            instrumentation.removeTransformer(transformer);

        }

    }

    // The following is the main entry point for transforming the bytecode.

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {

        if (HeapSettings.shouldAvoidAuditing(className, null) &&
            !HeapSettings.shouldInjectRecorder(className, null) &&
            !HeapSettings.shouldRemoveRecorder(className, null)) {

            return null;

        }

        ClassReader cr = new ClassReader(classfileBuffer);

        ClassWriter cw = new ClassWriter(cr,
                                         ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new HeapClass(cw,
                                className,
                                HeapSettings.shouldDebugAuditing(className, null)),
                  ClassReader.SKIP_FRAMES);

        return cw.toByteArray();

    }

}
