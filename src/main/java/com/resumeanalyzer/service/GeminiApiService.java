package com.resumeanalyzer.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class GeminiApiService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final Gson gson = new Gson();
    private static final MediaType JSON_TYPE =
            MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public String generateContent(String prompt) throws IOException {

        // ── Sanitize prompt before putting it in JSON ─────────
        String cleanPrompt = sanitizeText(prompt);

        // ── Build JSON using Gson objects (never raw strings) ──
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", cleanPrompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "llama-3.3-70b-versatile"); // ✅ updated model
        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", 2048);
        requestBody.addProperty("temperature", 0.3);
        requestBody.addProperty("stream", false);

        // ── Gson serializes everything safely ─────────────────
        String jsonBody = gson.toJson(requestBody);

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(RequestBody.create(jsonBody, JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            String responseBody = response.body() != null
                    ? response.body().string() : "empty";

            if (response.isSuccessful()) {
                return extractText(responseBody);
            }

            // ── Detailed error messages for each error code ───
            switch (response.code()) {
                case 400:
                    throw new IOException(
                        "Bad request sent to Groq (400). Response: " + responseBody);
                case 401:
                    throw new IOException(
                        "Invalid Groq API key (401). Check groq.api.key in application.properties.");
                case 429:
                    throw new IOException(
                        "Groq rate limit hit (429). Please wait a moment and try again.");
                case 500:
                    throw new IOException(
                        "Groq server error (500). Please try again in a few seconds.");
                default:
                    throw new IOException(
                        "Groq API error: " + response.code() + ". Body: " + responseBody);
            }
        }
    }

    private String extractText(String responseBody) throws IOException {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            return json
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
        } catch (Exception e) {
            throw new IOException(
                "Failed to parse Groq response: " + responseBody);
        }
    }

    // ── Remove characters that break JSON ─────────────────────
    private String sanitizeText(String text) {
        if (text == null) return "";
        return text
            .substring(0, Math.min(text.length(), 12000))
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
            .replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E\\u00A0-\\uFFFF]", " ")
            .trim();
    }
}