package com.pasadarider.Objects;

public class RideRequest {
    String uid;
    String userUid;
    String riderUid;
    Destination destination;
    Distance distance;
    Duration duration;
    UserLocation userLocation;
    String userFullName;
    ModesOfTransport modesOfTransport;
    long timestampStart;
    long timestampEnd;
    String status;

    public RideRequest() {
    }

    public RideRequest(String uid, String userUid, String riderUid, Destination destination, Distance distance, Duration duration, UserLocation userLocation, String userFullName, ModesOfTransport modesOfTransport, long timestampStart, long timestampEnd, String status) {
        this.uid = uid;
        this.userUid = userUid;
        this.riderUid = riderUid;
        this.destination = destination;
        this.distance = distance;
        this.duration = duration;
        this.userLocation = userLocation;
        this.userFullName = userFullName;
        this.modesOfTransport = modesOfTransport;
        this.timestampStart = timestampStart;
        this.timestampEnd = timestampEnd;
        this.status = status;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public String getRiderUid() {
        return riderUid;
    }

    public void setRiderUid(String riderUid) {
        this.riderUid = riderUid;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public Distance getDistance() {
        return distance;
    }

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public UserLocation getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(UserLocation userLocation) {
        this.userLocation = userLocation;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public ModesOfTransport getModesOfTransport() {
        return modesOfTransport;
    }

    public void setModesOfTransport(ModesOfTransport modesOfTransport) {
        this.modesOfTransport = modesOfTransport;
    }

    public long getTimestampStart() {
        return timestampStart;
    }

    public void setTimestampStart(long timestampStart) {
        this.timestampStart = timestampStart;
    }

    public long getTimestampEnd() {
        return timestampEnd;
    }

    public void setTimestampEnd(long timestampEnd) {
        this.timestampEnd = timestampEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
