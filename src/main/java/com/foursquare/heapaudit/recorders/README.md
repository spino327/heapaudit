The recorders sub-directory contains sample recorders which are bundled into the
heaprecorders.jar library and can serve as alternative dynamically injected
recorders.

## Injecting custom recorders

For instance, the following injects the sample [HeapTotal](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/recorders/HeapTotal.java)
recorder (bundled in heaprecorders.jar) into MyTest.test().

	$ java -jar heapaudit.jar 999 -Icom/foursquare/test/MyTest@test.+ -Xrecorder=com.foursquare.heapaudit.recorders.HeapTotal@heaprecorders.jar

## Implementing custom recorders

Simply define a new class that extends [HeapSummary](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapSummary.java).
