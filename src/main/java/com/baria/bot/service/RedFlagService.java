package com.baria.bot.service;

import com.baria.bot.model.RedFlag;
import com.baria.bot.model.User;
import com.baria.bot.repository.RedFlagRepository;
import com.baria.bot.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RedFlagService {
    private final RedFlagRepository redFlagRepository;
    private final UserRepository userRepository;

    public RedFlagService(RedFlagRepository redFlagRepository, UserRepository userRepository) {
        this.redFlagRepository = redFlagRepository;
        this.userRepository = userRepository;
    }

    public void registerFlag(Long tgId, String message, int severity) {
        User user = userRepository.findByTgId(tgId).orElseThrow();
        RedFlag flag = new RedFlag();
        flag.setUser(user);
        flag.setMessage(message);
        flag.setSeverity(severity);
        redFlagRepository.save(flag);
    }
}

