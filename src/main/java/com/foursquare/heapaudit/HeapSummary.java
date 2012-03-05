package com.foursquare.heapaudit;

public abstract class HeapSummary extends HeapRecorder {

    abstract public String summarize();

    public void setId(String id) {

        this.id = id;

    }

    public String getId() {

        return (id == null) ?
            Integer.toHexString(System.identityHashCode(this)) :
            id;

    }

    private String id = null;

}
