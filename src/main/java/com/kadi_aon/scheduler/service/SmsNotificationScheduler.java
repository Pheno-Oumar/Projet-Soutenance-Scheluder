package com.kadi_aon.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kadi_aon.scheduler.entity.NotificationSms;
import com.kadi_aon.scheduler.enums.StatutNotification;
import com.kadi_aon.scheduler.repository.NotificationSmsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsNotificationScheduler {

    private static final int BATCH_SIZE = 10;
    private static final int STALE_MINUTES = 15;

    private final NotificationSmsRepository notificationSmsRepository;
    private final SmsWorkerService smsWorkerService;

    @Scheduled(fixedDelay = 5000)
    public void dispatcherSmsEnAttente() {
        List<NotificationSms> snapshot = capturerEtVerrouillerSnapshot();

        if (snapshot.isEmpty()) {
            return;
        }

        log.info("[SMS SCHEDULER] Snapshot capturé : {} message(s) SMS/WhatsApp transféré(s) aux workers.", snapshot.size());

        for (NotificationSms sms : snapshot) {
            smsWorkerService.traiterSmsAsync(
                    sms.getId(),
                    sms.getDestinataire(),
                    sms.getMessage(),
                    sms.getCanal()
            );
        }
    }

    @Transactional
    public List<NotificationSms> capturerEtVerrouillerSnapshot() {
        // 1. Récupération fail-safe des tâches EN_COURS abandonnées (ex: crash serveur antérieur)
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        List<NotificationSms> staleList = notificationSmsRepository.findStaleEnCoursSms(staleThreshold);
        if (!staleList.isEmpty()) {
            log.warn("[SMS SCHEDULER] Récupération de {} notification(s) SMS bloquée(s) en EN_COURS depuis > {} min.",
                    staleList.size(), STALE_MINUTES);
            for (NotificationSms stale : staleList) {
                stale.setStatut(StatutNotification.EN_ATTENTE);
            }
            notificationSmsRepository.saveAll(staleList);
        }

        // 2. Capture d'un lot limité
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        List<NotificationSms> snapshot = notificationSmsRepository
                .findByStatutOrderByDateCreationAsc(StatutNotification.EN_ATTENTE, pageable);

        if (snapshot.isEmpty()) {
            return List.of();
        }

        // 3. Bascule instantanée en EN_COURS pour isoler le snapshot
        for (NotificationSms sms : snapshot) {
            sms.setStatut(StatutNotification.EN_COURS);
        }

        return notificationSmsRepository.saveAll(snapshot);
    }
}
