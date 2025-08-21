package com.backend.model.response;

import com.google.gson.annotations.SerializedName;

public class SocialAddressData {
    @SerializedName("sdenumire_Strada") private String denumireStrada;
    @SerializedName("snumar_Strada") private String numarStrada;
    @SerializedName("sdenumire_Localitate") private String denumireLocalitate;
    @SerializedName("scod_Localitate") private String codLocalitate;
    @SerializedName("sdenumire_Judet") private String denumireJudet;
    @SerializedName("scod_Judet") private String codJudet;
    @SerializedName("scod_JudetAuto") private String codJudetAuto;
    @SerializedName("stara") private String tara;
    @SerializedName("sdetalii_Adresa") private String detaliiAdresa;
    @SerializedName("scod_Postal") private String codPostal;

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

}