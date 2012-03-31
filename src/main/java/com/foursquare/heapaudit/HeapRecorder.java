package com.foursquare.heapaudit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class HeapRecorder {

    // Use the following annotation to suppress HeapAudit from recording
    // allocations caused by the annotated method directly and indirectly.

    public @Retention(RetentionPolicy.RUNTIME) @interface Suppress { }

    abstract public void record(String type,
                                int count,
                                long size);

    // The following keeps track of how many times each recorder has been
    // registered locally. When registered globally, this is not incremented.

    protected AtomicInteger registrations = new AtomicInteger();

    protected static String friendly(String type) {

        String t = type.replaceAll("^\\[*", "");

        switch (t.charAt(0)) {

        case 'Z':

            return "boolean";

        case 'B':

            return "byte";

        case 'C':

            return "char";

        case 'S':

            return "short";

        case 'I':

            return "int";

        case 'J':

            return "long";

        case 'F':

            return "float";

        case 'D':

            return "double";

        default:

            return t.replaceAll("^L", "").replaceAll(";$", "").replaceAll("/", ".");

        }

    }

    // TODO (norberthu): If possible, declare isAuditing as final such that JVM
    // can optimize out the status checks during JIT.

    public static boolean isAuditing = false;

    static Instrumentation instrumentation = null;

    private static HeapCollection<HeapRecorder> globalRecorders = new HeapCollection<HeapRecorder>();

    private static ThreadLocal<NestedRecorders> localRecorders = new ThreadLocal<NestedRecorders>() {

        @Override protected NestedRecorders initialValue() {

            return new NestedRecorders();

        }

    };

    // The following suppresses recording of allocations due to the
    // HeapAudit library itself to avoid being caught in an infinite loop.
    // Returns non-null context if caller is the first in the nested sequence.

    public static Object suppress() {

        NestedRecorders context = localRecorders.get();

        return (context.level++ == 0) ? context : null;

    }

    // The following unwinds the nested calls that suppressed of recordings.
    // Returns true if caller is the first in the nested sequence.

    public static Object unwind() {

        NestedRecorders context = localRecorders.get();

        return (--context.level == 0) ? context : null;

    }

    public static boolean hasRecorders() {

        return (localRecorders.get().recorders.size() > 0) || (globalRecorders.size() > 0);

    }

    static HeapRecorder[] getRecorders(Object context) {

        HeapCollection<HeapRecorder> localRecorders = ((NestedRecorders)context).recorders;

        HeapRecorder[] recorders = new HeapRecorder[globalRecorders.size() + localRecorders.size()];

        int index = 0;

        for (HeapRecorder recorder: globalRecorders) {

            recorders[index++] = recorder;

        }

        for (HeapRecorder recorder: localRecorders) {

            recorders[index++] = recorder;

        }

        return recorders;

    }

    private static synchronized void register(HeapRecorder recorder) {

        // The following round about way of inserting the recorder into
        // globalRecorders is because the consuming end of this collection is
        // not synchronized.

        HeapCollection<HeapRecorder> recorders = new HeapCollection<HeapRecorder>();

        for (HeapRecorder r: globalRecorders) {

            recorders.add(r);

        }

        recorders.add(recorder);

        globalRecorders = recorders;

    }

    private static synchronized void unregister(HeapRecorder recorder) {

        // The following round about way of removing the recorder from
        // globalRecorders is because the consuming end of this collection is
        // not synchronized.

        HeapCollection<HeapRecorder> recorders = new HeapCollection<HeapRecorder>();

        for (HeapRecorder r: globalRecorders) {

            recorders.add(r);

        }

        recorders.remove(recorder);

        globalRecorders = recorders;

    }

    // The following describes how the recorder should be registered.

    public enum Threading {

        // Registered across all threads in the process.

        Global,

        // Registered on the local thread only.

	Local,

    }

    public static void register(HeapRecorder recorder,
                                Threading threading) {

        if (threading == Threading.Global) {

            register(recorder);

        }
        else {

            localRecorders.get().recorders.add(recorder);

            recorder.registrations.incrementAndGet();

        }

    }

    public static void unregister(HeapRecorder recorder,
                                  Threading threading) {

        if (threading == Threading.Global) {

            unregister(recorder);

        }
        else {

            localRecorders.get().recorders.remove(recorder);

        }

    }

}

class NestedRecorders {

    public int level = 0;

    public HeapCollection<HeapRecorder> recorders = new HeapCollection<HeapRecorder>();

}
