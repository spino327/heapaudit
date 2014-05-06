package com.foursquare.heapaudit.tutorials.example4;

import com.foursquare.heapaudit.tutorials.Example;

// The following illustrates using HeapAudit to record allocations by static
// instrumentation using a custom recorder. See https://github.com/foursquare/heapaudit/blob/master/src/test/java/com/foursquare/heapaudit/tutorials/example4/README.md
// for more information.

public class Custom extends Example {

    public static void main(String[] args) {

        allocateFoo();

        allocateBar();

        allocatedC();
    }

}
