package com.foursquare.heapaudit.recorders;

import com.foursquare.heapaudit.HeapSummary;

public class HeapActivity extends HeapSummary {

    @Override public void record(String type,
                                 int count,
                                 long size) {

        String length = "";

        if (count >= 0) {

            length = "[" + count + "]";

        }

        System.out.println("new " + type + length + " (" + size + " bytes)");

    }

	@Override
	public String summarize() {
		// TODO Auto-generated method stub
		return null;
	}

}
