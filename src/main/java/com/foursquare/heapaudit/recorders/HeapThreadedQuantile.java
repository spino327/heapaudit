package com.foursquare.heapaudit.recorders;

import com.foursquare.heapaudit.HeapQuantile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HeapThreadedQuantile extends HeapQuantile {

    private HashMap<Long, AtomicInteger> allRegistrations = new HashMap<Long, AtomicInteger>();

    private ThreadLocal<AtomicInteger> threadedRegistrations = new ThreadLocal<AtomicInteger>() {

        @Override protected AtomicInteger initialValue() {

            AtomicInteger counter = new AtomicInteger();

            allRegistrations.put(Thread.currentThread().getId(),
                                 counter);

            return counter;

        }

    };

    @Override public void onRegister() {

        super.onRegister();

        threadedRegistrations.get().incrementAndGet();

    }

    @Override public String summarize() {

        String summary = "";

        synchronized (registrations) {

            synchronized (threadedRecords) {

                for (Records records: threadedRecords) {

                    summary += "HEAP: " + getId() + " x" + allRegistrations.get(records.id) + " @" + records.id;

                    ArrayList<Stats> sQuantiles = new ArrayList<Stats>();

                    flatten(sQuantiles,
                            records.quantilesType);

                    flatten(sQuantiles,
                            records.quantilesArray);

                    Collections.sort(sQuantiles);

                    for (Stats s: sQuantiles) {

                        summary += "\n      - " + s.toString();

                    }

                    summary += "\n";

                }

            }

        }

        return summary;

    }

}
