package com.example.mortarcalculator;

public class GeoPoint {
    private final double latitude;
    private final double longitude;
    private double elevation;
    private MortarType mortarType; // только для точек минометов

    public GeoPoint(double latitude, double longitude) {
        this(latitude, longitude, 0.0);
    }

    public GeoPoint(double latitude, double longitude, double elevation) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.mortarType = MortarType.PREDEFINED_MORTARS[0]; // По умолчанию первый тип
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public MortarType getMortarType() {
        return mortarType;
    }

    public void setMortarType(MortarType mortarType) {
        this.mortarType = mortarType;
    }
} 