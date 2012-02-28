package com.foursquare.heapaudit;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class HeapClass extends HeapUtil implements ClassVisitor {

    public HeapClass(ClassVisitor cv,
                     String classId,
                     boolean suppressAuditing,
                     boolean debugAuditing) {

        this.cv = new ClassAdapter(cv);

        this.id = classId;

        this.suppressClass = suppressAuditing;

        this.debugClass = debugAuditing;

        log(debugClass,
            "\tCLASS " + id);

    }

    private final ClassAdapter cv;

    private String source;

    private final String id;

    private boolean suppressClass;

    private final boolean debugClass;

    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {

        log(debugClass,
            "visit()");

        cv.visit(version,
                 access,
                 name,
                 signature,
                 superName,
                 interfaces);

    }

    public void visitSource(String source,
                            String debug) {

        log(debugClass,
            "visitSource(" + source + ", " + debug + ")");

        this.source = source;

        cv.visitSource(source,
                       debug);

    }

    public void visitOuterClass(String owner,
                                String name,
                                String desc) {

        log(debugClass,
            "visitOuterClass()");

        cv.visitOuterClass(owner,
                           name,
                           desc);

    }

    public AnnotationVisitor visitAnnotation(String desc,
                                             boolean visible) {

        log(debugClass,
            "visitAnnotation(" + desc + ", " + visible + ")");

        if (desc.equals("Lcom/foursquare/heapaudit/HeapRecorder$Suppress;")) {

            suppressClass = true;

        }

        return cv.visitAnnotation(desc,
                                  visible);

    }

    public void visitAttribute(Attribute attr) {

        log(debugClass,
            "visitAttribute()");

        cv.visitAttribute(attr);

    }

    public void visitInnerClass(String name,
                                String outerName,
                                String innerName,
                                int access) {

        log(debugClass,
            "visitInnerClass()");

        cv.visitInnerClass(name,
                           outerName,
                           innerName,
                           access);

    }

    public FieldVisitor visitField(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   Object value) {

        log(debugClass,
            "visitField()");

        return cv.visitField(access,
                             name,
                             desc,
                             signature,
                             value);

    }

    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {

        log(debugClass,
            "visitMethod()");

        String method = name + desc;

        boolean suppressAuditing = suppressClass ||
                                   HeapSettings.shouldSuppressAuditing(id,
                                                                       method);

        boolean debugAuditing = HeapSettings.shouldDebugAuditing(id,
                                                                 method);

        boolean traceAuditing = HeapSettings.shouldTraceAuditing(id,
                                                                 method);

        boolean injectRecorder = HeapSettings.shouldInjectRecorder(id,
                                                                   method);

        if (!suppressAuditing &&
            HeapSettings.shouldAvoidAuditing(id, method) &&
            !injectRecorder) {

            return cv.visitMethod(access,
                                  name,
                                  desc,
                                  signature,
                                  exceptions);

        }

        HeapMethod mv = new HeapMethod(cv.visitMethod(access,
                                                      name,
                                                      desc,
                                                      signature,
                                                      exceptions),
                                       source,
                                       id + '@' + method,
                                       suppressAuditing,
                                       debugAuditing,
                                       traceAuditing,
                                       injectRecorder);

        // The following sets up the weird cyclic dependency whereby the
        // HeapMethod implementation uses the HeapVariables class for injecting
        // new local variables but the HeapVariables wraps the HeapMethod from
        // the outside.

        mv.lvs = new HeapVariables(access,
                                   desc,
                                   debugAuditing,
                                   traceAuditing,
                                   mv);

        return mv.lvs.lvs;

    }

    public void visitEnd() {

        log(debugClass,
            "visitEnd()");

        cv.visitEnd();

    }

}
