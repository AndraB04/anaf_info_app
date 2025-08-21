package com.backend.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Indicator {
    @SerializedName("indicator")
    private String codIndicator;

    @SerializedName("val_indicator")
    private long valoareIndicator;

    @SerializedName("val_den_indicator")
    private String denumireIndicator;

    public String getCodIndicator() {
        return codIndicator;
    }

    public long getValoareIndicator() {
        return valoareIndicator;
    }

    public String getDenumireIndicator() {
        return denumireIndicator;
    }

    // Metode convenience pentru compatibilitate
    public String getCod() {
        return codIndicator;
    }

    public long getVal_indicator() {
        return valoareIndicator;
    }

    public long getVal_anul_raportare() {
        return valoareIndicator; // sau poate o altă logică
    }
}