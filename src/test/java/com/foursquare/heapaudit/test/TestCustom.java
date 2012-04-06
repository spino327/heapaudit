package com.foursquare.heapaudit.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestCustom {

    // Test custom scenario.

    @Test public void Custom() throws InterruptedException, IOException {

        Process process = Runtime.getRuntime().exec("java -javaagent:" + System.getProperty("heapaudit") + "=-Icom.foursquare.heapaudit.tutorials.Example@allocateBar.+#-Xrecorder=com.foursquare.heapaudit.recorders.HeapTotal@" + System.getProperty("heaprecorders") + " -classpath test-classes com/foursquare/heapaudit/tutorials/example4/Custom");

        process.waitFor();

        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String result = "";

        for (String s = input.readLine(); s != null; s = input.readLine()) {

            result += s + "\n";

        }

        assertTrue(result.matches("(?m)^com/foursquare/heapaudit/tutorials/Example@allocateBar\\(\\)V x\\d+: \\d+ bytes / \\d+ allocs$[\\s\\S]*"));

    }

}
