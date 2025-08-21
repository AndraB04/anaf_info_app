package com.backend.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class FoundCompany {

    @SerializedName("date_generale")
    private GeneralData dateGenerale;

    @SerializedName("inregistrare_scop_Tva")
    private VatRegistrationData inregistrareScopTva;

    @SerializedName("inregistrare_RTVAI")
    private VatOnCashData inregistrareTvaIncasare;

    @SerializedName("stare_inactiv")
    private InactiveStatusData stareInactiv;

    @SerializedName("inregistrare_SplitTVA")
    private SplitVatData inregistrareSplitTVA;

    @SerializedName("adresa_sediu_social")
    private SocialAddressData adresaSediuSocial;

    @SerializedName("adresa_domiciliu_fiscal")
    private FiscalAddressData adresaDomiciliuFiscal;

    // Convenience methods pentru DatabaseService
    public long getCui() {
        return dateGenerale != null ? dateGenerale.getCui() : 0;
    }

    public String getDenumire() {
        return dateGenerale != null ? dateGenerale.getDenumire() : null;
    }

    public String getAdresa() {
        return adresaDomiciliuFiscal != null ? adresaDomiciliuFiscal.getAdresa() : null;
    }

    public String getNrRegCom() {
        return dateGenerale != null ? dateGenerale.getNrRegCom() : null;
    }

    public String getTelefon() {
        return dateGenerale != null ? dateGenerale.getTelefon() : null;
    }

    public String getFax() {
        return dateGenerale != null ? dateGenerale.getFax() : null;
    }

    public String getCodPostal() {
        return dateGenerale != null ? dateGenerale.getCodPostal() : null;
    }

    public String getDataInregistrare() {
        return dateGenerale != null ? dateGenerale.getDataInregistrare() : null;
    }

    public int getCaen() {
        return dateGenerale != null ? dateGenerale.getCaen() : 0;
    }

    public String getDenumireCaen() {
        return dateGenerale != null ? dateGenerale.getDenumireCaen() : null;
    }

    public Boolean getScpTVA() {
        return inregistrareScopTva != null ? inregistrareScopTva.getScpTVA() : false;
    }
}