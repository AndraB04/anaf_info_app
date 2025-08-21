package com.backend.service;

import com.backend.model.DataRequest;
import com.google.gson.Gson;
import com.backend.model.response.AnafResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class AnafService {
    private final String Anaf_Url_Api="https://webservicesp.anaf.ro/api/PlatitorTvaRest/v9/tva";
    private final HttpClient client;
    private final Gson gson;
    public AnafService(){
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public AnafResponse checkAnaf(Integer cui) throws IOException, InterruptedException {
        String dataResponse = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        DataRequest requestDate = new DataRequest(cui, dataResponse);

        String jsonPayload = "[" + gson.toJson(requestDate) + "]";
        log.info("Sending request to ANAF API for CUI: {} with payload: {}", cui, jsonPayload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Anaf_Url_Api))
                .header("Content-Type", "application/json")
                .header("User-Agent", "AplicatieVerificareJava/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        HttpResponse<String> response= client.send(request, HttpResponse.BodyHandlers.ofString());
        String json_response= response.body();
        return gson.fromJson(json_response, AnafResponse.class);
    }
}
