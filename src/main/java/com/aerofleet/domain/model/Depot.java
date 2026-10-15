package com.aerofleet.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Base Station / Charging Hub on the 2D grid.
 */
@Entity
@Table(name = "depots")
public class Depot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double x;
    private double y;
    private int chargingPadsTotal;
    private int chargingPadsOccupied;

    public Depot() {
    }

    public Depot(Long id, String name, double x, double y, int chargingPadsTotal, int chargingPadsOccupied) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.chargingPadsTotal = chargingPadsTotal;
        this.chargingPadsOccupied = chargingPadsOccupied;
    }

    public Location getLocation() {
        return new Location(x, y);
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

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
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
