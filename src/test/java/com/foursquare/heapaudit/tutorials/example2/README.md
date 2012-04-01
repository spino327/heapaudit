## Static use case

This example illustrates HeapAudit statically instrumenting the target code at
load time.

After building the package, execute the following from the command line under
/target/test-classes/.

	$ java -javaagent:heapaudit.jar com/foursquare/heapaudit/tutorials/example2/Static

NOTE: You need to point to the file path for heapaudit.jar with the correct
version number under /target/.
