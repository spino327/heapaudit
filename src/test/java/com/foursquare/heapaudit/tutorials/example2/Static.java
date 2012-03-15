package com.foursquare.heapaudit.tutorials.example2;

import com.foursquare.heapaudit.HeapQuantile;
import com.foursquare.heapaudit.HeapRecorder;
import com.foursquare.heapaudit.tutorials.Example;

// The following illustrates using HeapAudit to record allocations by static
// instrumentation. See https://github.com/foursquare/heapaudit/blob/master/src/test/java/com/foursquare/heapaudit/tutorials/example2/README.md
// for more information.

public class Static extends Example {

    public static void main(String[] args) {

        allocateFoo();

        HeapQuantile recorder = new HeapQuantile();
        HeapRecorder.register(recorder, false);

        allocateBar();

        HeapRecorder.unregister(recorder, false);
        System.out.println(recorder.summarize(false));

    }

}
