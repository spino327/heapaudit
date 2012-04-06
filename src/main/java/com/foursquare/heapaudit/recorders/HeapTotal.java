package com.foursquare.heapaudit.recorders;

import com.foursquare.heapaudit.HeapSummary;
import java.util.concurrent.atomic.AtomicLong;

public class HeapTotal extends HeapSummary {

    private final AtomicLong occurrences = new AtomicLong();

    private final AtomicLong bytes = new AtomicLong();

    @Override public void record(String type,
                                 int count,
                                 long size) {

        occurrences.incrementAndGet();

        bytes.addAndGet(size);

    }

    @Override public String summarize() {

        return getId() + " x" + registrations.get() + ": " + bytes + " bytes / " + occurrences + " allocs";

    }

}
