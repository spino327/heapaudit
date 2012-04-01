package com.foursquare.heapaudit.tutorials.example5;

import com.foursquare.heapaudit.tutorials.Example;

// The following illustrates using HeapAudit to record allocations by static
// instrumentation across threads. See https://github.com/foursquare/heapaudit/blob/master/src/test/java/com/foursquare/heapaudit/tutorials/example5/README.md
// for more information.

public class Threaded extends Example {

    public static void main(String[] args) throws InterruptedException {

        allocateFoo();

        run();

    }

    static void run() throws InterruptedException {

        Background thread = new Background();

        thread.start();

        thread.join();

    }

    static class Background extends Thread {

        public void run() {

            allocateBar();

        }

    }

}
