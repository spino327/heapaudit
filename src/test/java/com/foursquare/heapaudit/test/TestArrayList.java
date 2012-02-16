package com.foursquare.heapaudit.test;

import java.util.ArrayList;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestArrayList extends TestUtil {

    // Test allocation of ArrayList.

    @Test public void ArrayList() {

	clear();

        ArrayList<Integer> array = new ArrayList<Integer>();

	for (int i = 0; i < 100; ++i) {

	    array.add(i);

	}

	assertTrue(expect("java.util.ArrayList",
			  -1,
			  24));

	assertTrue(expect("java.lang.Object",
			  10,
			  56));

	assertTrue(expect("java.lang.Object",
			  16,
			  80));

	assertTrue(expect("java.lang.Object",
			  25,
			  120));

	assertTrue(expect("java.lang.Object",
			  38,
			  168));

	assertTrue(expect("java.lang.Object",
			  58,
			  248));

	assertTrue(expect("java.lang.Object",
			  88,
			  368));

	assertTrue(expect("java.lang.Object",
			  133,
			  552));

	assertTrue(empty());

    }

}
