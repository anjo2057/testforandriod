package com.example.testingand;

import com.google.gson.annotations.SerializedName;

public class Planet {

    @SerializedName("name")
    private String name;

    @SerializedName("distance")
    private int distance;

    @SerializedName("gravity")
    private int gravity;

    @SerializedName("diameter")
    private int diameter;

    @SerializedName("image")
    private String image;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getGravity() {
        return gravity;
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
    }

    public int getDiameter() {
        return diameter;
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}