package com.baria.bot.repository;

import com.baria.bot.model.SymptomLog;
import com.baria.bot.model.SymptomLog.SymptomStatus;
import com.baria.bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SymptomLogRepository extends JpaRepository<SymptomLog, Long> {
    
    List<SymptomLog> findByUserAndStatus(User user, SymptomStatus status);
    
    Optional<SymptomLog> findFirstByUserAndSymptomAndStatusOrderByStartedAtDesc(
            User user, String symptom, SymptomStatus status);
    
    List<SymptomLog> findByUserOrderByStartedAtDesc(User user);
}
