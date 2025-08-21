package com.backend.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class BilantResponse {
    private int an;
    private int cui;
    private String deni;
    private int caen;

    @SerializedName("den_caen")
    private String denumireCaen;

    @SerializedName("i")
    private List<Indicator> indicatori;

    public int getAn() {
        return an;
    }

    public long getCui() {
        return cui;
    }

    public String getDeni() {
        return deni;
    }

    public int getCaen() {
        return caen;
    }

    public String getDenumireCaen() {
        return denumireCaen;
    }

    public List<Indicator> getIndicatori() {
        return indicatori;
    }
}
