package com.example.fishfinder.data;

import android.graphics.Bitmap;

import java.io.Serializable;

public class FishInfo implements Serializable {

    /* Variables - (Variable names are same as JSON keys) */
    private String FBname;      // From Fish Base API
    private String Species;
    private String Genus;
    private String BodyShapeI;
    private Double Length;
    private Double Weight;
    private String image;
    private String Dangerous;
    private String Comments;
    private boolean Fresh;
    private boolean Saltwater;

    private byte[] imageBytes;

    /* Constructor - TODO: Possibly make default values null */
    public FishInfo() {
        FBname = null;
        Species = null;
        Genus = null;
        BodyShapeI = null;
        Length = null;
        Weight = null;
        image = null;
        Dangerous = null;
        Comments = null;
        imageBytes = null;
    }

    /* Accessor Methods */

    public String getSpecies() {
        return Species;
    }

    public String getGenus() {
        return Genus;
    }

    public String getFBname() {
        return FBname;
    }

    public Double getLength() {
        return Length;
    }

    public String getBodyShapeI() {
        return BodyShapeI;
    }

    public Double getWeight() {
        return Weight;
    }

    public String getImage() {
        return image;
    }

    public String getComments() {
        return Comments;
    }

    public String getDangerous() {
        return Dangerous;
    }

    public boolean isFresh() {
        return Fresh;
    }

    public boolean isSaltwater() {
        return Saltwater;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }



    /* Mutator Methods */

    public void setSpecies(String species) {
        this.Species = species;
    }

    public void setGenus(String genus) {
        Genus = genus;
    }

    public void setFBname(String FBname) {
        this.FBname = FBname;
    }

    public void setBodyShapeI(String bodyShapeI) {
        BodyShapeI = bodyShapeI;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setLength(Double length) {
        Length = length;
    }

    public void setWeight(Double weight) {
        Weight = weight;
    }

    public void setComments(String comments) {
        Comments = comments;
    }

    public void setDangerous(String dangerous) {
        Dangerous = dangerous;
    }

    public void setFresh(boolean fresh) {
        Fresh = fresh;
    }

    public void setSaltwater(boolean saltwater) {
        Saltwater = saltwater;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    /* Logic Methods */

    @Override
    public String toString() {
        return String.format("<Fish: %s,\nSpecies: %s,\nBodyShape: %s,\nLength: %.2f,\nWeight: %.2f,\nComments: %s>",this.FBname, this.Species, this.BodyShapeI, this.Length, this.Weight, this.Comments);
    }
}
