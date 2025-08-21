package com.backend.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class InactiveStatusData {

    @SerializedName("dataInactivare")
    private String dataInactivare;

    @SerializedName("dataReactivare")
    private String dataReactivare;

    @SerializedName("dataPublicare")
    private String dataPublicare;

    @SerializedName("dataRadiere")
    private String dataRadiere;

    @SerializedName("statusInactivi")
    private boolean isInactive;

    // --- Gettere ---
    public String getDataInactivare() {
        return dataInactivare;
    }

    public String getDataReactivare() {
        return dataReactivare;
    }

    public String getDataPublicare() {
        return dataPublicare;
    }

    public String getDataRadiere() {
        return dataRadiere;
    }

    public boolean isInactive() {
        return isInactive;
    }

    // Metoda convenience pentru compatibilitate
    public boolean getStareInactiva() {
        return isInactive;
    }
}