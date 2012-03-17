package com.foursquare.heapaudit;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class HeapUtil {

    public static void log(String text) {

        HeapSettings.output.println(text);

    }

    public static void log(boolean debug,
                           String text) {

        if (debug) {

            log("\t" + text);

        }

    }

    public static void log(boolean debug,
                           boolean trace,
                           MethodAdapter mv,
                           String text) {

        log(debug,
            text);

        if (trace) {

            // STACK [...]
            mv.visitLdcInsn(text);
            // STACK [...|text]
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                               "com/foursquare/heapaudit/HeapUtil",
                               "log",
                               "(Ljava/lang/String;)V");
            // STACK [...]

        }

    }

    protected static void visitCheck(MethodVisitor mv,
                                     Label cleanup) {

        // STACK: [...]
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           "com/foursquare/heapaudit/HeapRecorder",
                           "hasRecorders",
                           "()Z");
        // STACK: [...|status]
        mv.visitJumpInsn(Opcodes.IFEQ,
                         cleanup);
        // STACK: [...]

    }

    protected static void visitCleanup(MethodVisitor mv,
                                       Label cleanup,
                                       Label finish) {

        // STACK: [...]
        mv.visitJumpInsn(Opcodes.GOTO,
                         finish);
        // STACK: [...]
        mv.visitLabel(cleanup);
        // STACK: [...]

    }

    protected static void visitFinish(MethodVisitor mv,
                                      Label finish) {

        // STACK: [...]
        mv.visitLabel(finish);
        // STACK: [...]

    }

    // The following holds a cache of type to size mappings.

    private static ConcurrentHashMap<String, Long> sizes = new ConcurrentHashMap<String, Long>();

    private static long sizeOf(Object obj,
                               String type) {

        Long size = sizes.get(type);

        if (size == null) {

            size = HeapRecorder.instrumentation.getObjectSize(obj);

            sizes.put(type,
                      size);

        }

        return size;

    }

    public static void record(Object obj,
                              int count,
                              String type,
                              long size) {

        if (size < 0) {

            if (HeapRecorder.suppress() != null) {

                size = sizeOf(obj,
                              "" + count + type);

            }

            HeapRecorder.unwind();

        }

        record(count,
               type,
               size);

    }

    public static void record(Object obj,
                              String type) {

        if (type.charAt(0) != '[') {

            record(obj,
                   -1,
                   type,
                   -1);

        }
        else if (HeapRecorder.suppress() != null) {

            long overhead = 0;

            Object[] o = (Object[])obj;

            int length = o.length;

            int count = length;

            for (int i = 1; i < type.length(); ++i) {

                // note that "o" might be null if this was a multidimensional
                // array with empty dims

                if (type.charAt(i) == '[' && o != null) {

                    // The following assumes the size of array of array,
                    // including the overhead of the array bookkeeping itself
                    // is only affected by the number of elements, not the
                    // actual element type.

                    overhead += sizeOf(o,
                                       "" + length + "[[L");

                    // o[0] might be null if this was a multidimensional
                    // array with empty dims - if so, set length to 0

                    switch (type.charAt(i + 1)) {

                    case 'Z':

                        length = (o[0] != null ? ((boolean[])o[0]).length : 0 );

                        break;

                    case 'B':

                        length = (o[0] != null ? ((byte[])o[0]).length : 0 );

                        break;

                    case 'C':

                        length = (o[0] != null ? ((char[])o[0]).length : 0 );

                        break;

                    case 'S':

                        length = (o[0] != null ? ((short[])o[0]).length : 0 );

                        break;

                    case 'I':

                        length = (o[0] != null ? ((int[])o[0]).length : 0 );

                        break;

                    case 'J':

                        length = (o[0] != null ? ((long[])o[0]).length : 0 );

                        break;

                    case 'F':

                        length = (o[0] != null ? ((float[])o[0]).length : 0 );

                        break;

                    case 'D':

                        length = (o[0] != null ? ((double[])o[0]).length : 0 );

                        break;

                    case 'L':

                        length = (o[0] != null ? ((Object[])o[0]).length : 0 );

                        break;

                    default:

                        o = (Object[])(o[0]);

                        // make sure this is not a null array due to a
                        // multidimensional array with empty dims

                        if (o != null) {

                            length = o.length;

                            count *= length;

                        }

                    }

                }
                else {

                    final String name;
                    final long size;

                    if (o != null && o[0] != null) {

                        name = type.substring(i);

                        size = overhead + count * sizeOf(o[0],
                                                         "" + length + type.substring(i - 1));

                    }
                    else {

                        // patch things up so we record the right length, name
                        // and size when this was a multidimensional array with
                        // empty dims

                        length = 1;

                        name = type.substring(i-1);

                        size = overhead;

                    }

                    HeapRecorder.unwind();

                    record(obj,
                           count * length,
                           name,
                           size);

                    break;

                }

            }

        }
        else {

            HeapRecorder.unwind();

        }

    }

    public static void record(Object obj,
                              int count,
                              String type) {

        if (HeapRecorder.suppress() != null) {

            long size = sizeOf(obj,
                               "" + count + "[" + type);

            HeapRecorder.unwind();

            record(obj,
                   count,
                   type,
                   size);

        }
        else {

            HeapRecorder.unwind();

        }

    }

    public static void record(Object obj,
                              int[] dimensions,
                              String type) {

        if (HeapRecorder.suppress() != null) {

            long overhead = 0;

            Object o[] = (Object[])obj;

            int count = 1;

            for (int i = 0; i < dimensions.length - 1 && count > 0; ++i) {

                int length = dimensions[i];

                if (length >= 0) {

                    // The following assumes the size of array of array, including
                    // the overhead of the array bookkeeping itself is only affected
                    // by the number of elements, not the actual element type.

                    overhead += sizeOf(o,
                                       "" + length + "[[L");

                    o = (Object[])(o[0]);

                }

                count *= length;

            }

            if (count > 0) {

                int length = dimensions[dimensions.length - 1];

                long size = overhead + count * sizeOf(o,
                                                      "" + length + "[" + type);

                HeapRecorder.unwind();

                record(obj,
                       count * length,
                       type,
                       size);

            }
            else {

                HeapRecorder.unwind();

            }

        }
        else {

            HeapRecorder.unwind();

        }

    }

    public static void record(int count,
                              String type,
                              long size) {

        Object context = HeapRecorder.suppress();

        if (context != null) {

            for (HeapRecorder recorder: HeapRecorder.getRecorders(context)) {

                try {

                    recorder.record(type,
                                    count,
                                    size);

                } catch (Exception e) {

                    System.err.println(e);

                }
            }

        }

        HeapRecorder.unwind();

    }

    private final static HashMap<String, HeapSummary> recorders = new HashMap<String, HeapSummary>();

    public static boolean inject(String id) {

        HeapRecorder.suppress();

        try {

            HeapSummary recorder = (HeapSettings.recorderClass == null) ?
                new HeapQuantile() :
                (HeapSummary)HeapSettings.recorderClass.newInstance();

            recorder.setId(id);

            recorders.put(id,
                          recorder);

        }
        catch (IllegalAccessException e) {

            return false;

        }
        catch (InstantiationException e) {

            return false;

        }

        HeapRecorder.unwind();

        return true;

    }

    public static void dump() {

        for (HeapSummary recorder: recorders.values()) {

            HeapSettings.output.println(recorder.summarize());

        }

        recorders.clear();

    }

    public static void register(String id) {

        HeapRecorder.suppress();

        HeapSummary recorder = recorders.get(id);

        if (recorder != null) {

            HeapRecorder.register(recorder);

        }

        HeapRecorder.unwind();

    }

    public static void unregister(String id) {

        HeapRecorder.suppress();

        HeapSummary recorder = recorders.get(id);

        if (recorder != null) {

            HeapRecorder.unregister(recorder);

        }

        HeapRecorder.unwind();

    }

}
