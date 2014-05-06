package com.foursquare.heapaudit.recorders;

import java.util.concurrent.atomic.AtomicLong;

import com.foursquare.heapaudit.HeapSettings;
import com.foursquare.heapaudit.HeapSummary;

public class HeapSites extends HeapSummary {

    private final AtomicLong occurrences = new AtomicLong();
    private final AtomicLong bytes = new AtomicLong();
    private final AtomicLong max_bytes = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong min_bytes = new AtomicLong(Long.MAX_VALUE);
    
    @Override public void record(String type,
                                 int count,
                                 long size) {

        occurrences.incrementAndGet();
        bytes.addAndGet(size);
        
        // updating the max
        if (size > max_bytes.get())
        	max_bytes.set(size);
        // updating the min
        if (size < min_bytes.get())
        	min_bytes.set(size);
    }

    @Override public String summarize() {

        return getId() + " x" + registrations.get() + 
        		": " + bytes + " bytes / " + occurrences + " allocs, " +
        		(registrations.get() > 0 && bytes.get() > 0? "avg: " + bytes.get()/registrations.floatValue() + " bytes, " +
        		"max: " + max_bytes + " bytes, min: " + min_bytes + " bytes" : "");

    }

}
