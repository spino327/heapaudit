package com.foursquare.heapaudit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

class HeapSettings {

    public static void parse(String args) throws FileNotFoundException {

        // The following describes how to specify the args string.
        // 
        //   Syntax for args: [ -Xconditional ]
        //                    [ -Xtimeout=<milliseconds> ]
        //                    [ -Xoutput=<file> ]
        //                    [ -S<path> |
        //                      -A<path> |
        //                      -D<path> |
        //                      -T<path> |
        //                      -I<path> ]*
        //
        //   Syntax for path: <class_regex>[@<method_regex>]
        //
        //   * Use -Xconditional if most of the time zero recorders are
        //     registered to actively record heap allocations. It includes
        //     extra if-statements to short-circuit the recording logic.
        //     However, if recorders are expected to be mostly present, then
        //     including extra if-statements adds extra execution instructions.
        //
        //   * Use -Xtimeout for dynamic use case to automatically exit after
        //     the specified amount of milliseconds.
        //
        //   * Use -Xoutput to redirect the output to the designated file.
        //
        //   * Use -S to suppress auditing a particular path and its sub calls.
        //   * Use -A to avoid auditing a particular path.
        //   * Use -D to debug instrumentation of a particular path.
        //   * Use -T to trace execution of auditing a particular path.
        //   * Use -I to dynamically inject recorders for a particular path.
        //
        //   * Paths are specified as a one or two part regular expressions
        //     where if the second part if omitted, it is treated as a catch all
        //     wild card. The class_regex matches with the full namespace path
        //     of the class where '/' is used as the separator. The method_regex
        //     matches with the method name and method signature where the
        //     signature follows the JNI method descriptor convention. See
        //     http://java.sun.com/docs/books/jni/html/types.html
        //
        //   For instance:
        //
        //     The following avoids auditing all methods under the class
        //     com/foursquare/MyUtil
        //       -Acom/foursquare/MyUtil
        //
        //     The following injects recorders for all toString methods under
        //     the class com/foursquare/MyTest
        //       -Icom/foursquare/MyTest@toString.+
        //
        //   The -S option is more applicable to general scenarios over the -A
        //   option. The former suppresses the entire sub call tree as oppose to
        //   only skipping the designated class or method. The latter will still
        //   include the indirect allocations down the callstack.
        //
        //   The -D and -T options are normally used for HeapAudit development
        //   purposes only.
        //
        //   The -I option dynamically injects recorders to capture all heap
        //   allocations that occur within the designated method, including
        //   sub-method calls.

        toSuppressAuditing.clear();

        toAvoidAuditing.clear();
        
        toIncludeAuditing.clear();

        toDebugAuditing.clear();

        toTraceAuditing.clear();

        toInjectRecorder.clear();

        toAvoidAuditing.addAll(Arrays.asList(new Pattern("java/lang/ThreadLocal"),
                                             new Pattern("org/objectweb/asm/.+"),
                                             new Pattern("com/foursquare/heapaudit/HeapCollection.*"),
                                             new Pattern("com/foursquare/heapaudit/HeapRecorder.*"),
                                             new Pattern("com/foursquare/heapaudit/HeapUtil.*"),
                                             new Pattern("[$].*"),
                                             new Pattern("java/.+"),
                                             new Pattern("javax/.+"),
                                             new Pattern("org/jcp/.+"),
                                             new Pattern("org/xml/.+"),
                                             new Pattern("com/apple/.+"),
                                             new Pattern("apple/.+"),
                                             new Pattern("com/sun/.+"),
                                             new Pattern("sun/.+"),
                                             new Pattern("oracle/.+"),
                                             new Pattern("jrockit/.+")));

        // actually do transform these classes, despite the matches in toAvoidAuditing

        toIncludeAuditing.addAll(Arrays.asList(new Pattern("java/util/.+")
                                               // what can safely go here? It's unknown, but
                                               // java/.+ causes all kinds of trouble. It's not
                                               // actually known that java/util/.+ is safe
                                               ));

        if (args != null) {

            for (String arg: args.split("[ #]")) {

                if ((arg.length() < 2) ||
                    (arg.charAt(0) != '-')) {

                    throw new IllegalArgumentException(arg);

                }

                String value = (arg.length() > 2) ? arg.substring(2) : null;

                switch (arg.charAt(1)) {

                case 'X':

                    if (value.startsWith("timeout=")) {

                        timeout = Integer.parseInt(value.substring(8));

                    }
                    else if (value.startsWith("output=")) {

                        FileOutputStream stream = new FileOutputStream(value.substring(7));

                        lock = stream.getChannel();

                        output = value.length() > 0 ? new PrintStream(stream) : System.out;

                    }
                    else if (value.equals("conditional")) {

                        conditional = true;

                    }

                    break;

                case 'S':

                    toSuppressAuditing.add(new Pattern(value));

                    break;

                case 'A':

                    toAvoidAuditing.add(new Pattern(value));

                    break;

                case 'D':

                    toDebugAuditing.add(new Pattern(value));

                    break;

                case 'I':

                    toInjectRecorder.add(new Pattern(value));

                    break;

                case 'T':

                    toTraceAuditing.add(new Pattern(value));

                    break;

                default:

                    throw new IllegalArgumentException(arg);

                }

            }

        }

    }

    // The timeout specifies how many milliseconds to wait before exiting from
    // the dynamic use case.

    public static int timeout = -1;

    // The conditional setting determines whether to optimize for tradeoffs by
    // adding extra bytecode instructions to check and potentially skip the code
    // paths for executing the recording logic. If HeapAudit is expected to
    // always have at least one recorder present, then setting conditional to
    // false can avoid the checks.

    public static boolean conditional = false;

    public static FileChannel lock = null;

    public static PrintStream output = System.out;

    private final static ArrayList<Pattern> toSuppressAuditing = new ArrayList<Pattern>();

    private final static ArrayList<Pattern> toAvoidAuditing = new ArrayList<Pattern>();

    // override avoidance
    private final static ArrayList<Pattern> toIncludeAuditing = new ArrayList<Pattern>();

    private final static ArrayList<Pattern> toDebugAuditing = new ArrayList<Pattern>();

    private final static ArrayList<Pattern> toTraceAuditing = new ArrayList<Pattern>();

    private final static ArrayList<Pattern> toInjectRecorder = new ArrayList<Pattern>();

    private static boolean should(ArrayList<Pattern> patterns,
                                  String classPath,
                                  String methodName) {

        for (Pattern pattern: patterns) {

            if (classPath.matches(pattern.classPattern)) {

                if ((methodName == null) ||
                    (pattern.methodPattern == null) ||
                    methodName.matches(pattern.methodPattern)) {

                    return true;

                }

            }

        }

        return false;

    }

    public static boolean shouldSuppressAuditing(String classPath,
                                                 String methodName) {

        return should(toSuppressAuditing,
                      classPath,
                      methodName);

    }

    public static boolean shouldAvoidAuditing(String classPath,
                                              String methodName) {

        return should(toAvoidAuditing,
                      classPath,
                      methodName) &&
            (! should(toIncludeAuditing,
                      classPath,
                      methodName));

    }

    public static boolean shouldDebugAuditing(String classPath,
                                            String methodName) {

        return should(toDebugAuditing,
                      classPath,
                      methodName);

    }

    public static boolean shouldTraceAuditing(String classPath,
                                              String methodName) {

        return should(toTraceAuditing,
                      classPath,
                      methodName);

    }

    public static boolean shouldInjectRecorder(String classPath,
                                               String methodName) {

        return should(toInjectRecorder,
                      classPath,
                      methodName);

    }

    private static class Pattern {

        public Pattern(String pattern) {

            String[] parts = pattern.split("@");

            classPattern = parts[0];

            methodPattern = (parts.length > 1) ? parts[1] : null;

        }

        public final String classPattern;

        public final String methodPattern;

    }

}
