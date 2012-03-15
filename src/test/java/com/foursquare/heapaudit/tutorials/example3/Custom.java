package com.foursquare.heapaudit.tutorials.example3;

import com.foursquare.heapaudit.tutorials.Example;

// The following illustrates using HeapAudit to record allocations by dynamic
// instrumentation with a custom recorder. See https://github.com/foursquare/heapaudit/blob/master/src/test/java/com/foursquare/heapaudit/tutorials/example3/README.md
// for more information.

public class Custom extends Example {

    public static void main(String[] args) {

        System.console().readLine("After injecting HeapAudit, press <enter> to continue...");

        allocateFoo();

        allocateBar();

    }

}
