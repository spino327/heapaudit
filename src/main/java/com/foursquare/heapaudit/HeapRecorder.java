package com.foursquare.heapaudit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ConcurrentHashMap;
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

    private static ConcurrentHashMap<Long, NestedRecorders> threadedRecorders = new ConcurrentHashMap<Long, NestedRecorders>();

    private static ThreadLocal<NestedRecorders> localRecorders = new ThreadLocal<NestedRecorders>() {

        @Override protected NestedRecorders initialValue() {

            // In the event the parent thread wishes to extend all of its local
            // recorders down to the child thread, the recorders will have been
            // stashed in threadedRecorders keyed on the child thread id.

            NestedRecorders recorders = threadedRecorders.get(Thread.currentThread().getId());

            return (recorders == null) ? new NestedRecorders() : recorders;

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
    // Returns non-null if caller is the first in the nested sequence.

    public static Object unwind() {

        NestedRecorders context = localRecorders.get();

        return (--context.level == 0) ? context : null;

    }

    public static boolean hasRecorders() {

        return (localRecorders.get().recorders.size() > 0) || (globalRecorders.size() > 0);

    }

    // The following retrieves all recorders. The context is obtained by calling
    // suppress. Caller should call unwind afterwards.

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

	Local

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

    // The following is to be called by the parent thread to extend all of its
    // local recorders down to the child thread.

    static void extend(long id) {

        Object context = suppress();

        NestedRecorders recorders = new NestedRecorders();

        recorders.recorders.addAll(((NestedRecorders)context).recorders);

        threadedRecorders.put(id,
                              recorders);

        unwind();

    }

}

class NestedRecorders {

    public int level = 0;

    public final HeapCollection<HeapRecorder> recorders = new HeapCollection<HeapRecorder>();

}
