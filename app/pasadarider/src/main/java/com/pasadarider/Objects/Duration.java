package com.pasadarider.Objects;

public class Duration {
    String humanReadable;
    long inSeconds;

    public Duration() {
    }

    public Duration(String humanReadable, long inSeconds) {
        this.humanReadable = humanReadable;
        this.inSeconds = inSeconds;
    }

    public String getHumanReadable() {
        return humanReadable;
    }

    public void setHumanReadable(String humanReadable) {
        this.humanReadable = humanReadable;
    }

    public long getInSeconds() {
        return inSeconds;
    }

    public void setInSeconds(long inSeconds) {
        this.inSeconds = inSeconds;
    }
}
