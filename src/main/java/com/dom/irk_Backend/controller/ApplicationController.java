package com.dom.irk_Backend.controller;

import com.dom.irk_Backend.model.Application;
import com.dom.irk_Backend.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // Składanie nowej aplikacji
    @PostMapping("/{recruitmentId}")
    public ResponseEntity<?> apply(@PathVariable Integer recruitmentId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Application app = applicationService.applyForRecruitment(auth.getName(), recruitmentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(app);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Pobieranie własnych aplikacji kandydata
    @GetMapping("/my")
    public ResponseEntity<List<Application>> getMyApplications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(applicationService.getMyApplications(auth.getName()));
    }

    // Endpoint dla administratora, pobierający wszystkich kandydatów dla danego naboru
    @GetMapping("/recruitment/{recruitmentId}")
    public ResponseEntity<List<Application>> getApplicationsByRecruitment(@PathVariable Integer recruitmentId) {
        // Uwaga: Tutaj warto dodać zabezpieczenie (np. sprawdzić rolę), by tylko admin miał do tego dostęp
        return ResponseEntity.ok(applicationService.getApplicationsForRecruitment(recruitmentId));
    }
}