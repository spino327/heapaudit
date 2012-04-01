## Threaded use case

This example illustrates HeapAudit statically instrumenting the target code at
run time and recording allocations across threads.

After building the package, execute the following from the commandline under
/target/test-classes/.

	$ java -javaagent:heapaudit.jar="-Icom.foursquare.heapaudit.tutorials.example5.Threaded@run.+ -Xthreaded"
	       com/foursquare/heapaudit/tutorials/example5/Threaded

NOTE: You need to point to the file path for heapaudit.jar with the correct
version number under /target/.
