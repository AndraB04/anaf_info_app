package com.backend.model.response;

import com.google.gson.annotations.SerializedName;

public class VatOnCashData {

    @SerializedName("dataInceputTvaInc")
    private String dataInceputTvaInc;

    @SerializedName("dataSfarsitTvaInc")
    private String dataSfarsitTvaInc;

    @SerializedName("dataActualizareTvaInc")
    private String dataActualizareTvaInc;

    @SerializedName("dataPublicareTvaInc")
    private String dataPublicareTvaInc;

    @SerializedName("tipActTvaInc")
    private String tipActTvaInc;

    @SerializedName("statusTvaIncasare")
    private boolean platitorTvaIncasare;

    // --- Gettere ---
    public String getDataInceputTvaInc() {
        return dataInceputTvaInc;
    }

    public String getDataSfarsitTvaInc() {
        return dataSfarsitTvaInc;
    }

    public String getDataActualizareTvaInc() {
        return dataActualizareTvaInc;
    }

    public String getDataPublicareTvaInc() {
        return dataPublicareTvaInc;
    }

    public String getTipActTvaInc() {
        return tipActTvaInc;
    }

    public boolean isPlatitorTvaIncasare() {
        return platitorTvaIncasare;
    }
}