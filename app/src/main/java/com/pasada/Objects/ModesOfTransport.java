package com.pasada.Objects;

public class ModesOfTransport {
    boolean habalHabal;
    boolean motorpot;
    boolean trisikad;

    public ModesOfTransport() {
    }

    public ModesOfTransport(boolean habalHabal, boolean motorpot, boolean trisikad) {
        this.habalHabal = habalHabal;
        this.motorpot = motorpot;
        this.trisikad = trisikad;
    }

    public boolean isHabalHabal() {
        return habalHabal;
    }

    public void setHabalHabal(boolean habalHabal) {
        this.habalHabal = habalHabal;
    }

    public boolean isMotorpot() {
        return motorpot;
    }

    public void setMotorpot(boolean motorpot) {
        this.motorpot = motorpot;
    }

    public boolean isTrisikad() {
        return trisikad;
    }

    public void setTrisikad(boolean trisikad) {
        this.trisikad = trisikad;
    }
}
