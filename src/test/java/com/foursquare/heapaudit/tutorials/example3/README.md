## Dynamic use case with custom recorder

This example illustrates HeapAudit dynamically instrumenting the target code at
run time with a custom recorder.

After building the package, execute the following from the command line under
/target/test-classes/.

	$ java com/foursquare/heapaudit/tutorials/example3/Custom

From a different command line, run the following with the matching process id.

	$ java -jar heapaudit.jar 999 -Icom.foursquare.heapaudit.tutorials.Example@allocateBar.+
	       -Xrecorder=com.foursquare.heapaudit.recorders.HeapTotal@heaprecorders.jar
	Press <enter> to exit HeapAudit...
In the first command line, press enter to continue with the allocation.

NOTE: You need to point to the file path for heapaudit.jar and heaprecorders.jar
with the correct version number under /target/.
