package com.pasadarider.Objects;

public class Distance {
    String humanReadable;
    long inMeters;

    public Distance() {
    }

    public Distance(String humanReadable, long inMeters) {
        this.humanReadable = humanReadable;
        this.inMeters = inMeters;
    }

    public String getHumanReadable() {
        return humanReadable;
    }

    public void setHumanReadable(String humanReadable) {
        this.humanReadable = humanReadable;
    }

    public long getInMeters() {
        return inMeters;
    }

    public void setInMeters(long inMeters) {
        this.inMeters = inMeters;
    }
}
