package com.foursquare.heapaudit.tutorials.example1;

import com.foursquare.heapaudit.tutorials.Example;

// The following illustrates using HeapAudit to record allocations by dynamic
// instrumentation. See https://github.com/foursquare/heapaudit/blob/master/src/test/java/com/foursquare/heapaudit/tutorials/example1/README.md
// for more information.

public class Dynamic extends Example {

    public static void main(String[] args) {

        System.console().readLine("After injecting HeapAudit, press <enter> to continue...");

        Example.allocateFoo();

        Example.allocateBar();

    }

}
