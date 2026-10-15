package com.aerofleet.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "depots")
public class Depot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double latitude;
    private double longitude;
    private double elevationMeters;
    private int chargingPadsTotal;
    private int chargingPadsOccupied;

    public Depot() {
    }

    public Depot(Long id, String name, double latitude, double longitude, double elevationMeters, int chargingPadsTotal, int chargingPadsOccupied) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevationMeters = elevationMeters;
        this.chargingPadsTotal = chargingPadsTotal;
        this.chargingPadsOccupied = chargingPadsOccupied;
    }

    public GeoCoordinate toCoordinate() {
        return new GeoCoordinate(latitude, longitude, elevationMeters);
    }

    public boolean hasAvailablePad() {
        return chargingPadsOccupied < chargingPadsTotal;
    }

    public synchronized void occupyPad() {
        if (chargingPadsOccupied < chargingPadsTotal) {
            chargingPadsOccupied++;
        }
    }

    public synchronized void releasePad() {
        if (chargingPadsOccupied > 0) {
            chargingPadsOccupied--;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getElevationMeters() {
        return elevationMeters;
    }

    public void setElevationMeters(double elevationMeters) {
        this.elevationMeters = elevationMeters;
    }

    public int getChargingPadsTotal() {
        return chargingPadsTotal;
    }

    public void setChargingPadsTotal(int chargingPadsTotal) {
        this.chargingPadsTotal = chargingPadsTotal;
    }

    public int getChargingPadsOccupied() {
        return chargingPadsOccupied;
    }

    public void setChargingPadsOccupied(int chargingPadsOccupied) {
        this.chargingPadsOccupied = chargingPadsOccupied;
    }
}
