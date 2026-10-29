package com.kadi_aon.scheduler.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.scheduler.enums.CanalMessage;
import com.kadi_aon.scheduler.enums.StatutNotification;
import com.kadi_aon.scheduler.repository.NotificationSmsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsWorkerService {

    private final NotificationSmsRepository notificationSmsRepository;
    private final TwilioService twilioService;
    private final AndroidSmsGatewayService androidSmsGatewayService;

    @Async("smsTaskExecutor")
    public void traiterSmsAsync(Long notificationId, String destinataire, String texte, CanalMessage canal) {
        log.debug("[SMS WORKER] Début traitement asynchrone pour notification #{} [{}] vers {}", notificationId, canal, destinataire);
        try {
            String sid;
            if (canal == CanalMessage.SMS && androidSmsGatewayService.isEnabled()) {
                sid = androidSmsGatewayService.envoyerSms(destinataire, texte);
            } else {
                sid = twilioService.envoyerMessage(destinataire, texte, canal);
            }

            mettreAJourStatutSucces(notificationId, sid);
            log.info("[SMS WORKER] Notification #{} [{}] expédiée avec succès vers {} (SID: {})",
                    notificationId, canal, destinataire, sid);
        } catch (Exception e) {
            log.error("[SMS WORKER] Échec d'envoi notification #{} [{}] vers {} : {}",
                    notificationId, canal, destinataire, e.getMessage(), e);
            mettreAJourStatutEchec(notificationId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mettreAJourStatutSucces(Long notificationId, String sid) {
        notificationSmsRepository.findById(notificationId).ifPresent(sms -> {
            sms.setStatut(StatutNotification.ENVOYE);
            sms.setDateEnvoi(LocalDateTime.now());
            sms.setSidMessage(sid);
            sms.setMessageErreur(null);
            notificationSmsRepository.save(sms);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mettreAJourStatutEchec(Long notificationId, String messageErreur) {
        notificationSmsRepository.findById(notificationId).ifPresent(sms -> {
            sms.setStatut(StatutNotification.ECHEC);
            sms.setMessageErreur(messageErreur != null && messageErreur.length() > 1000
                    ? messageErreur.substring(0, 1000)
                    : messageErreur);
            notificationSmsRepository.save(sms);
        });
    }
}
