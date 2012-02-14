# HeapAudit

HeapAudit is a java agent which audits heap allocations for JVM processes.

HeapAudit runs in two modes:

- STATIC: This requires a simple integration hook to be implemented by the java
process of interest. The callback hook defines how the allocations are recorded
and the callback code is only executed when the java agent is loaded.
- DYNAMIC: This injects HeapQuantile recorders to all matching methods and dumps
heap allocations to stdout when removed. Be aware, a lot of recorders, including
nested ones, may be injected if the supplied matching pattern is not restrictive
enough.

## Building and testing the HeapAudit java agent

Build project with Maven:

	$ mvn clean package

The built jar will be in 'target/'.

Because the included tests must be executed with the java agent attached, they
must run in the verify phase instead of in the test phase as unit tests:

	$ mvn verify

## Implementing the HeapAudit hook

Currently, two recorders are provided with HeapAudit:

- [HeapActivity](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapActivity.java)
prints each heap allocation to stdout as they occur
- [HeapQuantile](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapQuantile.java)
accumulates allocations and dumps out summary at the end

Both of the above inherit from the base class HeapRecorder. Additional recording
behavior can be extended by implementing the record method in [HeapRecorder](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapRecorder.java).

	class MyRecorder extends HeapRecorder {

	    @Override public void record(String name,
	                                 int count,
	                                 long size) {

	        System.out.println("Allocated " + name +
	                           "[" + count + "] " + size + " bytes");

	    }

	}

## Registering the HeapAudit recorder

Recording starts when it is registered and stops when it is unregistered. Each
recorder can be registered globally across all threads or local to the current.
The following example shows how to register the HeapActivity recorder across all
threads. The output will display as allocations occur.

	HeapActivity r = new HeapActivity();

	HeapRecorder.register(r, true);

	MyObject o = new MyObject();

	HeapRecorder.unregister(r, true);

The HeapQuantile recorder requires an extra step at the end to tally up the
results. The following example shows how to register the HeapQuantile recorder
only on the current thread and displays the summary at the end.

	HeapQuantile r = new HeapQuantile();

	HeapRecorder.register(r, false);

	MyObject o = new MyObject();

	HeapRecorder.unregister(r, false);

	for (HeapQuantile.Stats s: r.tally(false, true)) System.out.println(s);

## Launching the HeapAudit java agent

Launch HeapAudit statically along with the process of interest (requires MyTest
to implement the integration hook to register heap recorders).

	$ java -javaagent:heapaudit.jar MyTest

Launch HeapAudit dynamically by injecting to the process of interest (does not
require MyTest to have any prior intrumentations). The recorder data is dumped
to the console upon exiting.

	$ java -jar heapaudit.jar 999 -Icom/foursquare/test/MyTest@test.+

The JDK's tools.jar library is required to launch HeapAudit dynamically. If
launching within JRE, specify the -Xbootclasspath command line arg to point to
the tools.jar file.

	$ java -Xbootclasspath/a:/usr/local/lib/tools.jar -jar heapaudit.jar 999 -Icom/foursquare/test/MyTest@test.+

Additional options can be passed to HeapAudit to customize which classes and/or
methods are not to be instrumented for recording allocations. For additional
information on how to specify the options, see [HeapSettings.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapSettings.java).

	$ java -javaagent:heapaudit.jar="-Acom/foursquare/test/.+" MyTest

## Troubleshooting

Some libraries may not be instrumentable. This often manifests in some useless
error while instrumenting the target code, i.e. some generic error in the form
of a java.lang.NoClassDefFoundError exception. While some of the errors may be
attributed to bugs in HeapAudit, this should not block you from auditing the
rest of your code.

To identify the offending library, run with "-D.+" and search for the last line
of the debug output that starts with the word CLASS. You can subsequently avoid
instrumenting the identified class via the -A flag.

For instance, we recently switched to use jrockit JVM and it would not run with
HeapAudit attached. Upon troubleshooting with "-D.+", we noticed that many of
the classes under the jrockit/ namespace causes exception to be thrown during
startup. We subsequently ran HeapAudit with "-Ajrockit/.+" and everything
returned back to normal. See [HeapSettings.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapSettings.java)
for list of namespaces avoided by default.

## Dependencies

- [ASM](http://asm.ow2.org/)

## Maintainers

- Norbert Y. Hu norberthu@foursquare.com
