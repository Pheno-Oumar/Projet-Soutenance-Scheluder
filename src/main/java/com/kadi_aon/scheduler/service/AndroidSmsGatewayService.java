package com.kadi_aon.scheduler.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AndroidSmsGatewayService {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    @Value("${android.sms.gateway.url:}")
    private String gatewayUrl;

    @Value("${android.sms.gateway.username:}")
    private String username;

    @Value("${android.sms.gateway.password:}")
    private String password;

    @Value("${android.sms.gateway.enabled:false}")
    private boolean enabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public boolean isEnabled() {
        return enabled && gatewayUrl != null && !gatewayUrl.isBlank();
    }

    /**
     * Envoie un SMS réel via la passerelle Android SMS Gateway hébergée sur le téléphone mobile.
     *
     * @param destinataire Numéro de téléphone au format E.164 (+223...)
     * @param texte Message SMS à envoyer
     * @return Identifiant unique du message retourné par la passerelle Android
     */
    public String envoyerSms(String destinataire, String texte) {
        if (!isEnabled()) {
            throw new IllegalStateException("La passerelle Android SMS Gateway n'est pas activée.");
        }
        if (destinataire == null || destinataire.isBlank()) {
            throw new IllegalArgumentException("Le numéro du destinataire ne peut pas être vide.");
        }
        if (texte == null || texte.isBlank()) {
            throw new IllegalArgumentException("Le texte du SMS ne peut pas être vide.");
        }

        String targetUrl = gatewayUrl.endsWith("/") ? gatewayUrl + "message" : gatewayUrl + "/message";

        // Nettoyage / échappement JSON simple pour le payload
        String escapedTexte = texte
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        String payload = String.format("{\"message\":\"%s\",\"phoneNumbers\":[\"%s\"]}", escapedTexte, destinataire.trim());

        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        log.info("[ANDROID GATEWAY] Envoi SMS vers {} via passerelle {}...", destinataire, gatewayUrl);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                String messageId = "ASG_" + System.currentTimeMillis();
                Matcher matcher = ID_PATTERN.matcher(responseBody);
                if (matcher.find()) {
                    messageId = matcher.group(1);
                }
                log.info("[ANDROID GATEWAY] SMS expédié avec succès vers {} ! (Statut HTTP: {}, ID: {})",
                        destinataire, statusCode, messageId);
                return messageId;
            } else {
                log.error("[ANDROID GATEWAY] Échec de la passerelle (HTTP {}) : {}", statusCode, responseBody);
                throw new RuntimeException("Échec envoi SMS via passerelle Android (HTTP " + statusCode + ") : " + responseBody);
            }
        } catch (Exception e) {
            log.error("[ANDROID GATEWAY] Erreur réseau lors de l'envoi du SMS vers {} : {}", destinataire, e.getMessage());
            throw new RuntimeException("Erreur de connexion à la passerelle Android SMS Gateway : " + e.getMessage(), e);
        }
    }
}
