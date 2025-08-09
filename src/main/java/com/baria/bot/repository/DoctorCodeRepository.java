package com.baria.bot.repository;

import com.baria.bot.model.DoctorCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorCodeRepository extends JpaRepository<DoctorCode, String> {
}
