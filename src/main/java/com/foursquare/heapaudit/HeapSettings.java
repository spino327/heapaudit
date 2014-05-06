package com.foursquare.heapaudit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class HeapSettings {

    // The synchronized keyword is used on the parse method mostly to enforce a
    // memory barrier across all of the global variables being set in the parse
    // logic.

    static synchronized void parse(String args) throws ClassNotFoundException, FileNotFoundException, IllegalAccessException, InstantiationException, MalformedURLException {

        timeout = -1;

        threaded = false;

        conditional = false;

        enabled = true;

        lock = null;

        output = System.out;

        recorderClass = HeapQuantile.class;

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

                final String value = (arg.length() > 2) ? arg.substring(2) : null;

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
              	    else if (value.startsWith("recorder=")) {

                        String[] recorder = value.substring(9).split("@");

                        ClassLoader loader = new URLClassLoader(new URL[] { new URL("file:" + recorder[1]) });

                        recorderClass = loader.loadClass(recorder[0]).asSubclass(HeapSummary.class);

                    }
                    else if (value.equals("threaded")) {

                        threaded = true;

                    }
                    else if (value.equals("conditional")) {

                        conditional = true;

                    }
                    else if (value.startsWith("delay=")) {

                        // NOTE: The enabled variable is NOT declared volatile.
                        // The synchronized keyword surrounding setting the
                        // enabled variable causes the compiler to insert memory
                        // barriers before and after modifying the value, thus
                        // flushing the change across threads on all processors.

                        enabled = false;

                        Thread thread = new Thread() {

                            public void run() {

                                try {

                                    sleep(Long.parseLong(value.substring(6)));

                                } catch (java.lang.InterruptedException e) {

                                    output.println(e);

                                }

                                synchronized (this) {

                                    HeapSettings.enabled = true;

                                }

                            }

                        };

                        thread.start();

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

    static int timeout = -1;

    // The threaded setting determines whether to extend all local recorders
    // from the parent thread to the child thread.

    static boolean threaded = false;

    // The conditional setting determines whether to optimize for tradeoffs by
    // adding extra bytecode instructions to check and potentially skip the code
    // paths for executing the recording logic. If HeapAudit is expected to
    // always have at least one recorder present, then setting conditional to
    // false can avoid the checks.

    static boolean conditional = false;

    // The enabled setting determines whether to send the allocations to the
    // recorders.

    static boolean enabled = true;

    static FileChannel lock = null;

    public static PrintStream output = System.out;

    // The following specifies the class of the dynamically injected recorder.

    static Class<? extends HeapSummary> recorderClass = null;

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

    static boolean shouldSuppressAuditing(String classPath,
                                          String methodName) {

        return should(toSuppressAuditing,
                      classPath,
                      methodName);

    }

    static boolean shouldAvoidAuditing(String classPath,
                                       String methodName) {

        return should(toAvoidAuditing,
                      classPath,
                      methodName) &&
            (! should(toIncludeAuditing,
                      classPath,
                      methodName));

    }

    static boolean shouldDebugAuditing(String classPath,
                                       String methodName) {

        return should(toDebugAuditing,
                      classPath,
                      methodName);

    }

    static boolean shouldTraceAuditing(String classPath,
                                       String methodName) {

        return should(toTraceAuditing,
                      classPath,
                      methodName);

    }

    static boolean shouldInjectRecorder(String classPath,
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
