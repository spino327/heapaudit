package com.foursquare.heapaudit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.Instrumentation;

public abstract class HeapRecorder {

    // Use the following annotation to suppress HeapAudit from recording
    // allocations caused by the annotated method directly and indirectly.

    public @Retention(RetentionPolicy.RUNTIME) @interface Suppress { }

    abstract public void record(String type,
                                int count,
                                long size);

    protected static String friendly(String type) {

        switch (type.charAt(0)) {

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

            return type.replaceAll("^\\[*L", "").replaceAll(";$", "").replaceAll("/", ".");

        }

    }

    // TODO (norberthu): If possible, declare isAuditing as final such that JVM
    // can optimize out the status checks during JIT.

    public static boolean isAuditing = false;

    public static Instrumentation instrumentation = null;

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

    public static HeapCollection<HeapRecorder> getRecorders(Object context) {

        HeapCollection<HeapRecorder> recorders = new HeapCollection<HeapRecorder>();

        recorders.addAll(globalRecorders);

        recorders.addAll(((NestedRecorders)context).recorders);

        return recorders;

    }

    public static synchronized void register(HeapRecorder recorder) {

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

    public static synchronized void unregister(HeapRecorder recorder) {

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

    public static void register(HeapRecorder recorder,
                                boolean global) {

        if (global) {

            register(recorder);

        }
        else {

            localRecorders.get().recorders.add(recorder);

        }

    }

    public static void unregister(HeapRecorder recorder,
                                  boolean global) {

        if (global) {

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
