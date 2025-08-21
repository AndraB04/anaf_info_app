package com.backend.service;

import com.backend.model.response.BilantResponse;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class BilantService {
    private final String Anaf_Url_Api="https://webservicesp.anaf.ro/bilant";
    private final HttpClient client;
    private final Gson gson;
    public BilantService() {
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public BilantResponse checkBilant(int cui,int an) throws IOException, InterruptedException {
        Thread.sleep(1000);

        String fullUrl = String.format("%s?an=%d&cui=%d", Anaf_Url_Api, an, cui);
        log.info("Requesting Bilant data from ANAF API for CUI: {}, Year: {} - URL: {}", cui, an, fullUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String json_response = response.body();

        if (json_response != null && !json_response.trim().isEmpty()) {
            if (json_response.trim().startsWith("{")) {
                try {
                    return gson.fromJson(json_response, BilantResponse.class);
                } catch (Exception e) {
                    System.err.println("Error parsing JSON response: " + e.getMessage());
                    System.err.println("Response was: " + json_response);
                    return null;
                }
            } else {
                System.err.println("API returned non-JSON response: " + json_response);
                return null;
            }
        } else {
            System.err.println("No valid bilant data found for year " + an + ".");
            return null;
        }
    }
}
