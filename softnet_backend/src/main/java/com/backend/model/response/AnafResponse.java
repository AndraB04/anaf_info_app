package com.backend.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class AnafResponse {
    private int cod;
    private String data;
    private List<FoundCompany> found;
    private List<Object> notFound;
}