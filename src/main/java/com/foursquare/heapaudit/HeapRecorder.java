package com.foursquare.heapaudit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;

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

    private static ArrayList<HeapRecorder> globalRecorders = new ArrayList<HeapRecorder>();

    private static ThreadLocal<NestedRecorders> localRecorders = new ThreadLocal<NestedRecorders>() {

        @Override protected NestedRecorders initialValue() {

            return new NestedRecorders();

        }

    };

    // The following suppresses recording of allocations due to the
    // HeapAudit library itself to avoid being caught in an infinite loop.
    // Returns non-null context if caller is the first in the nested sequence.

    public static Object suppress() {

        NestedRecorders recorders = localRecorders.get();

        return (recorders.level++ == 0) ? recorders.context : null;

    }

    // The following unwinds the nested calls that suppressed of recordings.
    // Returns true if caller is the first in the nested sequence.

    public static Object unwind() {

        NestedRecorders recorders = localRecorders.get();

        return (--recorders.level == 0) ? recorders.context : null;

    }

    public static boolean hasRecorders() {

        return hasRecorders(localRecorders.get().context);

    }

    public static boolean hasRecorders(Object context) {

        return (((ArrayList<HeapRecorder>)context).size() > 0) || (globalRecorders.size() > 0);

    }

    public static ArrayList<HeapRecorder> getRecorders(Object context) {

        ArrayList<HeapRecorder> recorders = new ArrayList<HeapRecorder>(globalRecorders);

        recorders.addAll((ArrayList<HeapRecorder>)context);

        return recorders;

    }

    public static synchronized void register(HeapRecorder recorder) {

        ArrayList<HeapRecorder> recorders = new ArrayList<HeapRecorder>(globalRecorders);

        recorders.add(recorder);

        globalRecorders = recorders;

    }

    public static synchronized void unregister(HeapRecorder recorder) {

        ArrayList<HeapRecorder> recorders = new ArrayList<HeapRecorder>(globalRecorders);

        recorders.remove(recorder);

        globalRecorders = recorders;

    }

    public static void register(HeapRecorder recorder,
                                boolean global) {

        if (global) {

            register(recorder);

        }
        else {

            localRecorders.get().context.add(recorder);

        }

    }

    public static void unregister(HeapRecorder recorder,
                                  boolean global) {

        if (global) {

            unregister(recorder);

        }
        else {

            localRecorders.get().context.remove(recorder);

        }

    }

}

class NestedRecorders {

    public int level = 0;

    public ArrayList<HeapRecorder> context = new ArrayList<HeapRecorder>();

}
