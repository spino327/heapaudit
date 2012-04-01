This directory consists of the main implementation of HeapAudit. The files are
roughly organized as follow:

##### Entry point
- [HeapAudit.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapAudit.java)
  contains all entry points

##### Helper functions
- [HeapCollection.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapCollection.java)
  defines a basic collection class for internal use to avoid cyclic dependency
  for instrumentation
- [HeapSettings.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapSettings.java)
  parses and keeps track of the internal runtime settings
- [HeapThreaded.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapThreaded.java)
  implements the instrumentation byte code for extending threaded recorders
- [HeapUtil.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapUtil.java)
  provides a set of helper utilities for internal use
- [HeapVariables.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapVariables.java)
  supports introducing new local variables for instrumentation purposes

##### Internal recorders
- [HeapRecorder.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapRecorder.java)
  defines the base class for all recorder
- [HeapActivity.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapActivity.java)
  implements the most base form of a recorder by printing out each allocation
- [HeapSummary.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapSummary.java)
  defines the base class for all dynamically injectable recorders
- [HeapQuantile.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapQuantile.java)
  implements a quantile-based recorder that displays heap allocations broken
  down in buckets

##### ASM instrumentation hooks
- [HeapClass.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapClass.java)
  implements the instrumentation hook for classes
- [HeapMethod.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapMethod.java)
  implements the instrumentation hook for methods

##### ASM instrumentation byte codes
- [HeapANEWARRAY.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapANEWARRAY.java)
  implements the instrumentation byte code for array of object references
- [HeapCLONEARRAY.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapCLONEARRAY.java)
  implements the instrumentation byte code for cloning arrays
- [HeapCLONEOBJECT.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapCLONEOBJECT.java)
  implements the instrumentation byte code for cloning objects
- [HeapMULTIARRAY.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapMULTIARRAY.java)
  implements the instrumentation byte code for multi-dimensional arrays
- [HeapNEW.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapNEW.java)
  implements the instrumentation byte code for newing objects
- [HeapNEWARRAY.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapNEWARRAY.java)
  implements the instrumentation byte code for newing arrays
- [HeapNEWINSTANCE.java](https://github.com/foursquare/heapaudit/blob/master/src/main/java/com/foursquare/heapaudit/HeapNEWINSTANCE.java)
  implements the instrumentation byte code for dynamically instantiating objects
  or arrays
