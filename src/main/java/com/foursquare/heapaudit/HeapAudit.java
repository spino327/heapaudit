package com.foursquare.heapaudit;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.MalformedURLException;
import java.nio.channels.FileLock;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

// When loaded statically via the java command line as a java agent, the main
// entry point is HeapAudit.premain(). When loaded dynamically, the java command
// line first invokes HeapAudit.main() which then causes the JVM to load the
// java agent (the same jar file) into the target process.

public class HeapAudit extends HeapUtil implements ClassFileTransformer {

    private static void help() {

        System.out.println(
            "The following describes how to specify the options.                             \n" +
            "                                                                                \n" +
            "Syntax for options: [ -Xconditional ]                                           \n" +
            "                    [ -Xtimeout=<milliseconds> ]                                \n" +
            "                    [ -Xoutput=<file> ]                                         \n" +
            "                    [ -Xrecorder=<class>@<jar> ]                                \n" +
            "                    [ -S<path> |                                                \n" +
            "                      -A<path> |                                                \n" +
            "                      -D<path> |                                                \n" +
            "                      -T<path> |                                                \n" +
            "                      -I<path> ]*                                               \n" +
            "                                                                                \n" +
            "Syntax for path: <class_regex>[@<method_regex>]                                 \n" +
            "                                                                                \n" +
            "* Use -Xconditional if most of the time zero recorders are registered to        \n" +
            "  actively record heap allocations. It includes extra if-statements to short    \n" +
            "  circuit the recording logic. However, if recorders are expected to be mostly  \n" +
            "  present, then including extra if-statements will add extra execution          \n" +
            "  instructions.                                                                 \n" +
            "                                                                                \n" +
            "* Use -Xtimeout for dynamic use case to automatically exit after the specified  \n" +
            "  amount of milliseconds.                                                       \n" +
            "                                                                                \n" +
            "* Use -Xoutput to redirect the output to the designated file.                   \n" +
            "                                                                                \n" +
            "* Use -Xrecorder to override the default dynamic recorder with the specified    \n" +
            "  recorder class in the designated jar file.                                    \n" +
            "                                                                                \n" +
            "* Use -S to suppress auditing a particular path and its sub calls.              \n" +
            "* Use -A to avoid auditing a particular path.                                   \n" +
            "* Use -D to debug instrumentation of a particular path.                         \n" +
            "* Use -T to trace execution of auditing a particular path.                      \n" +
            "* Use -I to dynamically inject recorders for a particular path.                 \n" +
            "                                                                                \n" +
            "* Paths are specified as a one or two part regular expressions where if the     \n" +
            "  second part if omitted, it is treated as a catch all wild card. The           \n" +
            "  class_regex matches with the full namespace path of the class where '/' is    \n" +
            "  used as the separator. The method_regex matches with the method name and      \n" +
            "  method signature where the signature follows the JNI method descriptor        \n" +
            "  convention. See http://java.sun.com/docs/books/jni/html/types.html            \n" +
            "                                                                                \n" +
            "For instance:                                                                   \n" +
            "                                                                                \n" +
            "  The following avoids auditing all methods under the class                     \n" +
            "  com/foursquare/MyUtil                                                         \n" +
            "    -Acom/foursquare/MyUtil                                                     \n" +
            "                                                                                \n" +
            "  The following injects recorders for all toString methods under the class      \n" +
            "  com/foursquare/MyTest                                                         \n" +
            "    -Icom/foursquare/MyTest@toString.+                                          \n" +
            "                                                                                \n" +
            "The -S option is more applicable to general scenarios over the -A option. The   \n" +
            "former suppresses the entire sub call tree as oppose to only skipping the       \n" +
            "designated class or method. The latter will still include the indirect          \n" +
            "allocations down the callstack.                                                 \n" +
            "                                                                                \n" +
            "The -D and -T options are normally used for HeapAudit development purposes only.\n" +
            "                                                                                \n" +
            "The -I option dynamically injects recorders to capture all heap allocations     \n" +
            "that occur within the designated method, including sub-method calls.            \n"
        );

    }

    // The following is the wrapper that instructs JVM to load the java agent
    // into the designated target process.

    public static void main(String[] args) throws Exception {

        String id = null;

        StringBuffer s = new StringBuffer("-Xconditional");

        File file = null;

        boolean hasOutput = false;

        boolean hasTimeout = false;

        boolean hasInject = false;

        if (args.length > 0) {

            id = args[0];

        }
        else {

            // Show interactive menu for selecting which virtual machine to
            // attach to.

            for (VirtualMachineDescriptor vm: VirtualMachine.list()) {

                System.out.println(vm.id() + '\t' + vm.displayName());

            }

            id = System.console().readLine("PID: ");

        }

        VirtualMachine vm = null;

        try {

            vm = VirtualMachine.attach(id);

            String[] options = null;

            int start = 0;

            if (args.length > 1) {

                options = args;

                start = 1;

            }
            else {

                // Show interactive menu for specifying instrumentation options.

                do {

                    if (options != null) {

                        help();

                    }

                    options = System.console().readLine("OPTIONS[?]: ").split(" ");

                } while (options[0].equals(""));

            }

            for (int i = start; i < options.length; ++i) {

                s.append(' ');

                s.append(options[i]);

                if (options[i].startsWith("-Xoutput=")) {

                    file = new File(options[i].substring(9));

                    hasOutput = true;

                }
                else if (options[i].startsWith("-Xtimeout=")) {

                    hasTimeout = true;

                }
                else if (options[i].startsWith("-I")) {

                    hasInject = true;

                }

            }

            if (!hasInject) {

                help();

                throw new IllegalArgumentException("Missing -I option");

            }

            // The following instructs the java agent to write to a temporary
            // file if an output file has not already been specified. This is
            // necessary because the java agent runs in the injectee's JVM while
            // the injector runs in a separate JVM. The injectee will not be
            // able to directly write the output to the injector's console.

            if (!hasOutput) {

                file = File.createTempFile("heapaudit",
                                           ".out");

                s.append(" -Xoutput=" + file.getAbsolutePath());

            }

            // The following attaches to the specified process and dynamically
            // injects recorders to collect heap allocation activities. The
            // collection continues until the user presses enter at the command
            // line. Because we are dealing with two separate JVM instances, the
            // following logic relies on a file lock to signal when the
            // collection should terminate.

            if (!hasTimeout) {

                final FileLock lock = (new FileOutputStream(file)).getChannel().lock();

                (new Thread(new Runnable() {

                    public void run() {

                        try {

                            System.console().readLine("Press <enter> to exit HeapAudit...");

                            // Unblock agentmain barrier.

                            lock.release();

                        }
                        catch (Exception e) {

                        }

                    }

                })).start();

            }

            try {

                // Locate the current jar file path and inject itself into the
                // target JVM process. NOTE: The heapaudit.jar file is intended
                // to be multi-purposed. It acts as the java agent for the
                // static use case, the java agent for the dynamic use case and
                // also the command line utility to perform injecting the agent
                // for the dynamic use case.

                vm.loadAgent(HeapAudit.class.getProtectionDomain().getCodeSource().getLocation().getPath(),
                             s.toString());

            }
            catch (IOException e) {

                // There is nothing wrong here. If the targeting app exits
                // before agentmain exits, then an IOException will be thrown.
                // The cleanup logic in agentmain is also registered as a
                // shutdown hook. No need to worry about the non-terminated
                // agentmain behavior.

                System.out.println(" terminated");

            }

            // If the output file was not explicitly specified, display content
            // of the temporary file generated by the injectee to the injector's
            // console.

            if (!hasOutput) {

                BufferedReader input = new BufferedReader(new FileReader(file.getAbsolutePath()));

                char[] buffer = new char[4096];

                int length = 0;

                while ((length = input.read(buffer)) != -1) {

                    System.out.println(String.valueOf(buffer,
                                                      0,
                                                      length));

                }

                file.delete();

            }

        }
        catch (AttachNotSupportedException e) {

            help();

            throw e;

        }
        finally {

            if (vm != null) {

                vm.detach();

            }

        }

    }

    private static void initialize(String args,
                                   Instrumentation instrumentation) throws ClassNotFoundException, FileNotFoundException, IllegalAccessException, InstantiationException, MalformedURLException {

        HeapSettings.parse(args);

        HeapRecorder.isAuditing = true;

        HeapRecorder.instrumentation = instrumentation;

    }

    // The following is the entry point when loaded dynamically to inject
    // recorders from the target process. 

    public static void agentmain(final String args,
                                 final Instrumentation instrumentation) throws ClassNotFoundException, FileNotFoundException, IllegalAccessException, InstantiationException, InterruptedException, IOException, MalformedURLException, UnmodifiableClassException {

        initialize(args,
                   instrumentation);

        final HeapAudit agent = new HeapAudit();

        instrument(agent,
                   args,
                   instrumentation);

        Thread cleanup = new Thread() {

            @Override public void run() {

                try {

                    reset(agent,
                          args,
                          instrumentation);

                }
                catch (Exception e) {

                    // Swallow exception but surface the exception information
                    // in the log output.

                    HeapSettings.output.println(e);

                }

            }

        };

        // Add shutdown hook to handle the case where the targeting app exited
        // before the user pressed enter at the command line or before the
        // timeout expired.

        Runtime.getRuntime().addShutdownHook(cleanup);

        if (HeapSettings.timeout < 0) {

            // Block on barrier until user hits enter from command line to exit.

            HeapSettings.lock.lock().release();

        }
        else {

            // Sleep for specified amount of milliseconds and exit.

            Thread.sleep(HeapSettings.timeout);

        }

        // The following logic will not be executed if the targeting app exited
        // on its own via ctrl-C. If the app exited before the user pressed
        // enter at the command line or before the timeout expired, then the
        // reset logic is handled by the shutdown hook.

        Runtime.getRuntime().removeShutdownHook(cleanup);

        reset(agent,
              args,
              instrumentation);

    }

    // The following is the entry point when loaded as a java agent along with
    // the target process on the java command line.

    public static void premain(String args,
                               Instrumentation instrumentation) throws ClassNotFoundException, FileNotFoundException, IllegalAccessException, InstantiationException, MalformedURLException, UnmodifiableClassException {

        initialize(args,
                   instrumentation);

        instrument(new HeapAudit(),
                   args,
                   instrumentation);

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override public void run() {

                HeapUtil.dump();

            }

        });

    }

    // The following is the implementation for instrumenting the target code.

    private static void instrument(HeapAudit agent,
                                   String args,
                                   Instrumentation instrumentation) throws FileNotFoundException, UnmodifiableClassException {

        if (!instrumentation.isRetransformClassesSupported()) {

            throw new UnmodifiableClassException();

        }

        instrumentation.addTransformer(agent,
                                       true);

        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

        for (Class<?> c: instrumentation.getAllLoadedClasses()) {

            if (instrumentation.isModifiableClass(c)) {

                classes.add(c);

            }

        }

        instrumentation.retransformClasses(classes.toArray(new Class<?>[classes.size()]));

    }

    // The following is the implementation for resetting the instrumentation.

    private static void reset(HeapAudit agent,
                              String args,
                              Instrumentation instrumentation) throws FileNotFoundException, UnmodifiableClassException {

        instrumentation.removeTransformer(agent);

        HeapAudit cleanup = new HeapAudit() {

            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {

                // Returning null causes the class definition to be restored
                // back to the original byte codes.

                return null;

            }

        };

        instrument(cleanup,
                   args,
                   instrumentation);

        instrumentation.removeTransformer(cleanup);

        HeapUtil.dump();

    }

    // The following is the main entry point for transforming the bytecode.

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {

        byte[] buffer = null;

        boolean shouldSuppressAuditing = HeapSettings.shouldSuppressAuditing(className,
                                                                             null);

        boolean shouldAvoidAuditing = HeapSettings.shouldAvoidAuditing(className,
                                                                       null);

        boolean shouldDebugAuditing = HeapSettings.shouldDebugAuditing(className,
                                                                       null);

        boolean shouldInjectRecorder = HeapSettings.shouldInjectRecorder(className,
                                                                         null);

        if (shouldSuppressAuditing ||
            !shouldAvoidAuditing ||
            shouldInjectRecorder) {

            ClassReader cr = new ClassReader(classfileBuffer);

            ClassWriter cw = new ClassWriter(cr,
                                             ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

            cr.accept(new HeapClass(cw,
                                    className,
                                    shouldSuppressAuditing,
                                    shouldDebugAuditing),
                      ClassReader.SKIP_FRAMES);

            buffer = cw.toByteArray();

        }

        return buffer;

    }

}
