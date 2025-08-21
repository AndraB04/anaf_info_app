package com.backend.model.response;

import com.google.gson.annotations.SerializedName;

public class SplitVatData {

    @SerializedName("dataInceputSplitTVA")
    private String dataInceputSplitTVA;

    @SerializedName("dataAnulareSplitTVA")
    private String dataAnulareSplitTVA;

    @SerializedName("statusSplitTVA")
    private boolean isSplitTVA;

    public String getDataInceputSplitTVA() {
        return dataInceputSplitTVA;
    }

    public String getDataAnulareSplitTVA() {
        return dataAnulareSplitTVA;
    }

    public boolean isSplitTVA() {
        return isSplitTVA;
    }
}