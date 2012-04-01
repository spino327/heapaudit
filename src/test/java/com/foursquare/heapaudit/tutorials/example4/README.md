## Hybrid use case

This example illustrates HeapAudit statically instrumenting the target code at
run time.

After building the package, execute the following from the command line under
/target/test-classes/.

	$ java -javaagent:heapaudit.jar=-Icom.foursquare.heapaudit.tutorials.Example@allocateBar.+
	       com/foursquare/heapaudit/tutorials/example4/Hybrid

NOTE: You need to point to the file path for heapaudit.jar with the correct
version number under /target/.
