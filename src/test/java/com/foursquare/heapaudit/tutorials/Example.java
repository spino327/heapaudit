package com.foursquare.heapaudit.tutorials;

public abstract class Example {

    private static class Foo {

        public float value;

    }

    private static class Bar {

        public boolean value;

    }

    protected static void allocateFoo() {

        Foo f = new Foo();

    }

    protected static void allocateBar() {

        Bar b = new Bar();

    }

}
