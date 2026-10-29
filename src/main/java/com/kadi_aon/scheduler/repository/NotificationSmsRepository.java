package com.kadi_aon.scheduler.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kadi_aon.scheduler.entity.NotificationSms;
import com.kadi_aon.scheduler.enums.StatutNotification;

@Repository
public interface NotificationSmsRepository extends JpaRepository<NotificationSms, Long> {

    List<NotificationSms> findByStatutOrderByDateCreationAsc(StatutNotification statut, Pageable pageable);

    @Query("SELECT n FROM NotificationSms n WHERE n.statut = com.kadi_aon.scheduler.enums.StatutNotification.EN_COURS AND n.dateCreation < :staleTime")
    List<NotificationSms> findStaleEnCoursSms(@Param("staleTime") LocalDateTime staleTime);
}
