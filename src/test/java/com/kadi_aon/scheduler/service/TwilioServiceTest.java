package com.kadi_aon.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kadi_aon.scheduler.enums.CanalMessage;

class TwilioServiceTest {

    private TwilioService twilioService;

    @BeforeEach
    void setUp() {
        twilioService = new TwilioService();
        // Mode simulation par défaut (pas d'identifiants renseignés)
        twilioService.init();
    }

    @Test
    void envoyerMessage_simulationSms_retourneSidSimule() {
        String sid = twilioService.envoyerMessage("+22370000000", "Rappel de votre RDV", CanalMessage.SMS);

        assertNotNull(sid);
        assertTrue(sid.startsWith("SM_MOCK_"));
    }

    @Test
    void envoyerMessage_simulationWhatsApp_retourneSidSimule() {
        String sid = twilioService.envoyerMessage("+22370000000", "Rappel WhatsApp", CanalMessage.WHATSAPP);

        assertNotNull(sid);
        assertTrue(sid.startsWith("SM_MOCK_"));
    }

    @Test
    void envoyerMessage_destinataireVide_leveException() {
        assertThrows(IllegalArgumentException.class, () ->
                twilioService.envoyerMessage("", "Message test", CanalMessage.SMS)
        );
    }

    @Test
    void envoyerMessage_messageVide_leveException() {
        assertThrows(IllegalArgumentException.class, () ->
                twilioService.envoyerMessage("+22370000000", "  ", CanalMessage.SMS)
        );
    }
}
