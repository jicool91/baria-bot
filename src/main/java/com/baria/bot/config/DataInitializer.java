package com.baria.bot.config;

import com.baria.bot.model.DoctorCode;
import com.baria.bot.repository.DoctorCodeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final DoctorCodeRepository doctorCodeRepository;

    public DataInitializer(DoctorCodeRepository doctorCodeRepository) {
        this.doctorCodeRepository = doctorCodeRepository;
    }

    @Override
    public void run(String... args) {
        if (!doctorCodeRepository.existsById("DOC001")) {
            DoctorCode code = new DoctorCode();
            code.setCode("DOC001");
            code.setDoctorName("Тестовый врач");
            doctorCodeRepository.save(code);
        }
    }
}

