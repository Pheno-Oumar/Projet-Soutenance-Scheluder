package com.kadi_aon.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.kadi_aon.scheduler.entity.NotificationSms;
import com.kadi_aon.scheduler.enums.CanalMessage;
import com.kadi_aon.scheduler.enums.StatutNotification;
import com.kadi_aon.scheduler.repository.NotificationSmsRepository;

@ExtendWith(MockitoExtension.class)
class SmsNotificationSchedulerTest {

    @Mock
    private NotificationSmsRepository notificationSmsRepository;

    @Mock
    private SmsWorkerService smsWorkerService;

    @InjectMocks
    private SmsNotificationScheduler smsNotificationScheduler;

    private NotificationSms sms;

    @BeforeEach
    void setUp() {
        sms = NotificationSms.builder()
                .id(1L)
                .destinataire("+22377000000")
                .canal(CanalMessage.SMS)
                .message("Rappel de votre RDV à 14h")
                .statut(StatutNotification.EN_ATTENTE)
                .build();
    }

    @Test
    void dispatcherSmsEnAttente_succes_captureEtDelegueAuWorker() {
        when(notificationSmsRepository.findStaleEnCoursSms(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(notificationSmsRepository.findByStatutOrderByDateCreationAsc(eq(StatutNotification.EN_ATTENTE), any(Pageable.class)))
                .thenReturn(List.of(sms));
        when(notificationSmsRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        smsNotificationScheduler.dispatcherSmsEnAttente();

        verify(smsWorkerService).traiterSmsAsync(1L, "+22377000000", "Rappel de votre RDV à 14h", CanalMessage.SMS);
        assertEquals(StatutNotification.EN_COURS, sms.getStatut());
    }

    @Test
    void dispatcherSmsEnAttente_aucunMessage_neFaitRien() {
        when(notificationSmsRepository.findStaleEnCoursSms(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(notificationSmsRepository.findByStatutOrderByDateCreationAsc(eq(StatutNotification.EN_ATTENTE), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        smsNotificationScheduler.dispatcherSmsEnAttente();

        verify(smsWorkerService, never()).traiterSmsAsync(any(), any(), any(), any());
    }
}
