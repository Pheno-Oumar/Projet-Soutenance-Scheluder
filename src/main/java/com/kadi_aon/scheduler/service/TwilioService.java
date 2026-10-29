package com.kadi_aon.scheduler.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kadi_aon.scheduler.enums.CanalMessage;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TwilioService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.sms-from-number:+1234567890}")
    private String smsFromNumber;

    @Value("${twilio.whatsapp-from-number:whatsapp:+14155238886}")
    private String whatsappFromNumber;

    @Value("${twilio.enabled:false}")
    private boolean enabled;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (enabled && accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank()) {
            try {
                Twilio.init(accountSid, authToken);
                initialized = true;
                log.info("[TWILIO] Client Twilio initialisé avec succès pour Account SID: {}***",
                        accountSid.substring(0, Math.min(6, accountSid.length())));
            } catch (Exception e) {
                log.error("[TWILIO] Erreur lors de l'initialisation Twilio : {}", e.getMessage(), e);
            }
        } else {
            log.info("[TWILIO] Mode SIMULATION actif (identifiants non renseignés ou twilio.enabled=false). Les messages seront simulés et logués.");
        }
    }

    public String envoyerMessage(String destinataire, String texte, CanalMessage canal) {
        if (destinataire == null || destinataire.isBlank()) {
            throw new IllegalArgumentException("Le numéro de téléphone du destinataire ne peut pas être vide.");
        }
        if (texte == null || texte.isBlank()) {
            throw new IllegalArgumentException("Le texte du message ne peut pas être vide.");
        }

        // Si le mode réel n'est pas activé ou que l'initialisation a échoué -> mode simulation / console
        if (!enabled || !initialized) {
            String fakeSid = "SM_MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            log.info("[TWILIO SIMULATION] Canal: {} | Vers: {} | SID: {} | Message: \"{}\"",
                    canal, destinataire, fakeSid, texte);
            return fakeSid;
        }

        // Envoi réel via l'API Twilio
        if (canal == CanalMessage.WHATSAPP) {
            String toAddress = destinataire.startsWith("whatsapp:") ? destinataire : "whatsapp:" + destinataire;
            String fromAddress = whatsappFromNumber.startsWith("whatsapp:") ? whatsappFromNumber : "whatsapp:" + whatsappFromNumber;

            log.info("[TWILIO WHATSAPP] Envoi en cours de {} vers {}", fromAddress, toAddress);
            Message twilioMsg = Message.creator(
                    new PhoneNumber(toAddress),
                    new PhoneNumber(fromAddress),
                    texte
            ).create();

            log.info("[TWILIO WHATSAPP] Message envoyé avec succès. SID: {}, Statut: {}", twilioMsg.getSid(), twilioMsg.getStatus());
            return twilioMsg.getSid();
        } else {
            // SMS Standard
            String toAddress = destinataire.replace("whatsapp:", "").trim();
            String fromAddress = smsFromNumber.replace("whatsapp:", "").trim();

            log.info("[TWILIO SMS] Envoi en cours de {} vers {}", fromAddress, toAddress);
            Message twilioMsg = Message.creator(
                    new PhoneNumber(toAddress),
                    new PhoneNumber(fromAddress),
                    texte
            ).create();

            log.info("[TWILIO SMS] SMS envoyé avec succès. SID: {}, Statut: {}", twilioMsg.getSid(), twilioMsg.getStatus());
            return twilioMsg.getSid();
        }
    }
}
