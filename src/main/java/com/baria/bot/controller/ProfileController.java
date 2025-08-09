package com.baria.bot.controller;

import com.baria.bot.model.User;
import com.baria.bot.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/{tgId}")
    public ResponseEntity<Void> updateProfile(@PathVariable Long tgId, @RequestBody Map<String, String> body) {
        String name = body.get("fullName");
        String date = body.get("surgeryDate");
        profileService.updateBasicInfo(tgId, name, date != null ? java.time.LocalDate.parse(date) : null);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{tgId}")
    public ResponseEntity<User> getProfile(@PathVariable Long tgId) {
        return profileService.getByTgId(tgId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
