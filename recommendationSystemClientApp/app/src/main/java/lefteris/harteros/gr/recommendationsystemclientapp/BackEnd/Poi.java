/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

import android.graphics.Bitmap;

import java.io.Serializable;

public class Poi implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String poi;
    private double latitude;
    private double longitude;
    private String photo;
    private String category;
    private String name;
    public Bitmap p;

    public Poi() {
    }

    public Poi(int id, String poi, double latitude, double longitude, String photo, String category, String name) {
        this.id = id;
        this.poi = poi;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photo = photo;
        this.category = category;
        this.name = name;
    }

    public int getID() {
        return this.id;
    }

    public String getPoi() {
        return poi;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public String getPhoto() {
        return photo;
    }

    public String getCategory() {
        return this.category;
    }

    public String getName() {
        return this.name;
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setPoi(String poi) {
        this.poi = poi;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return id + "  " + poi + "  " + latitude + "  " + longitude + "  " + photo + "  " + category + "  " + name;
    }
}
