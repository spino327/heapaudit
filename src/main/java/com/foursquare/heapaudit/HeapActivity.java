package com.foursquare.heapaudit;

public class HeapActivity extends HeapRecorder {

    @Override public void record(String type,
                                 int count,
                                 long size) {

        String length = "";

        if (count >= 0) {

            length = "[" + count + "]";

        }

        HeapSettings.output.println("new " + type + length + " (" + size + " bytes)");

    }

}
