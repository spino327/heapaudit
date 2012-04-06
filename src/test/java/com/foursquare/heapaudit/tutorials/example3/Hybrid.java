package com.foursquare.heapaudit.tutorials.example3;

import com.foursquare.heapaudit.tutorials.Example;

// The following illustrates using HeapAudit to record allocations by static
// instrumentation. See https://github.com/foursquare/heapaudit/blob/master/src/test/java/com/foursquare/heapaudit/tutorials/example3/README.md
// for more information.

public class Hybrid extends Example {

    public static void main(String[] args) {

        allocateFoo();

        allocateBar();

    }

}
