package com.backend.model.response;

import com.google.gson.annotations.SerializedName;

public class TvaPeriod {

    @SerializedName("data_inceput_ScpTVA")
    private String dataInceputScpTVA;

    @SerializedName("data_sfarsit_ScpTVA")
    private String dataSfarsitScpTVA;

    @SerializedName("data_anul_imp_ScpTVA")
    private String dataAnulareImpunereScpTVA;

    @SerializedName("mesaj_ScpTVA")
    private String mesajScpTVA;

    public String getDataInceputScpTVA() {
        return dataInceputScpTVA;
    }

    public String getDataSfarsitScpTVA() {
        return dataSfarsitScpTVA;
    }

    public String getDataAnulareImpunereScpTVA() {
        return dataAnulareImpunereScpTVA;
    }

    public String getMesajScpTVA() {
        return mesajScpTVA;
    }
}