## Custom use case

This example illustrates HeapAudit statically instrumenting the target code at
run time with a custom recorder.

After building the package, execute the following from the command line under
/target/test-classes/.

	$ java -javaagent:heapaudit.jar="-Icom.foursquare.heapaudit.tutorials.Example@allocateBar.+
	                                 -Xrecorder=com.foursquare.heapaudit.recorders.HeapTotal@heaprecorders.jar"
	       com/foursquare/heapaudit/tutorials/example4/Custom

NOTE: You need to point to the file path for heapaudit.jar and heaprecorders.jar
with the correct version number under /target/.
