package com.foursquare.heapaudit.test;

import java.util.HashMap;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestHashMap extends TestUtil {

    // Test allocation of HashMap.

    @Test public void HashMap() {

	clear();

	HashMap<Integer, Integer> hashmap = new HashMap<Integer, Integer>();

	for (int i = 0; i < 100; ++i) {

	    hashmap.put(i, i);

	    assertTrue(expect("java.util.HashMap$Entry",
			      -1,
			      32));

	}

	assertTrue(expect("java.util.HashMap",
			  -1,
			  48));

	assertTrue(expect("java.util.HashMap$Entry",
			  16,
			  80));

	assertTrue(expect("java.util.HashMap$Entry",
			  32,
			  144));

	assertTrue(expect("java.util.HashMap$Entry",
			  64,
			  272));

	assertTrue(expect("java.util.HashMap$Entry",
			  128,
			  528));

	assertTrue(expect("java.util.HashMap$Entry",
			  256,
			  1040));

	assertTrue(empty());

    }

}
