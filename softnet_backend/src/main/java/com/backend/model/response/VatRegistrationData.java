package com.backend.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class VatRegistrationData {

    @SerializedName("scpTVA")
    private boolean platitorTVA;

    @SerializedName("perioade_TVA")
    private List<TvaPeriod> perioadeTVA;

    public boolean isPlatitorTVA() {
        return platitorTVA;
    }

    public List<TvaPeriod> getPerioadeTVA() {
        return perioadeTVA;
    }

    // Metoda convenience pentru compatibilitate
    public boolean getScpTVA() {
        return platitorTVA;
    }
}