package com.backend.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class FiscalAddressData {
    @SerializedName("ddenumire_Strada") private String denumireStrada;
    @SerializedName("dnumar_Strada") private String numarStrada;
    @SerializedName("ddenumire_Localitate") private String denumireLocalitate;
    @SerializedName("dcod_Localitate") private String codLocalitate;
    @SerializedName("ddenumire_Judet") private String denumireJudet;
    @SerializedName("dcod_Judet") private String codJudet;
    @SerializedName("dcod_JudetAuto") private String codJudetAuto;
    @SerializedName("dtara") private String tara;
    @SerializedName("ddetalii_Adresa") private String detaliiAdresa;
    @SerializedName("dcod_Postal") private String codPostal;

    public String getDenumireStrada() { return denumireStrada; }
    public String getNumarStrada() { return numarStrada; }
    public String getDenumireLocalitate() { return denumireLocalitate; }
    public String getCodLocalitate() { return codLocalitate; }
    public String getDenumireJudet() { return denumireJudet; }
    public String getCodJudet() { return codJudet; }
    public String getCodJudetAuto() { return codJudetAuto; }
    public String getTara() { return tara; }
    public String getDetaliiAdresa() { return detaliiAdresa; }
    public String getCodPostal() { return codPostal; }

    // Metoda convenience pentru a returna adresa completă
    public String getAdresa() {
        return detaliiAdresa != null ? detaliiAdresa : "";
    }
}