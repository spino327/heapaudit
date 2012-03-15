## Dynamic use case

This example illustrates HeapAudit dynamically instrumenting the target code at
run time.

After building the package, execute the following from the commandline under
/target/test-classes/.

	$ java com/foursquare/heapaudit/tutorials/example1/Dynamic

From a different commandline, run the following with the matching process id.

	$ java -jar heapaudit.jar 999 -Icom.foursquare.heapaudit.tutorials.Example@allocateBar.+

In the first commandline, press enter to continue with the allocation.

NOTE: You need to point to the file path for heapaudit.jar with the correct
version number under /target/.
