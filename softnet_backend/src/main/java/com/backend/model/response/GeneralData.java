package com.backend.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class GeneralData {
    @SerializedName("cui")
    private long cui;

    @SerializedName("denumire")
    private String denumire;

    @SerializedName("adresa")
    private String adresa;

    @SerializedName("nrRegCom")
    private String nrRegCom;

    @SerializedName("telefon")
    private String telefon;

    @SerializedName("fax")
    private String fax;

    @SerializedName("codPostal")
    private String codPostal;

    @SerializedName("act")
    private String act;

    @SerializedName("stare_inregistrare")
    private String stareInregistrare;

    @SerializedName("data_inregistrare")
    private String dataInregistrare;

    @SerializedName("statusRO_e_Factura")
    private boolean statusROeFactura;

    @SerializedName("organFiscalCompetent")
    private String organFiscalCompetent;

    @SerializedName("forma_de_proprietate")
    private String formaDeProprietate;

    @SerializedName("forma_organizare")
    private String formaOrganizare;

    @SerializedName("forma_juridica")
    private String formaJuridica;

    @SerializedName("caen")
    private int caen;

    @SerializedName("den_caen")
    private String denumireCaen;
}