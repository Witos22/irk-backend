package com.dom.irk_Backend.controller;

import com.dom.irk_Backend.model.Administrator;
import com.dom.irk_Backend.model.LoginRequest;
import com.dom.irk_Backend.service.AdministratorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/api/admins")
public class AdministratorController {

    private final AdministratorService administratorService;

    public AdministratorController(AdministratorService administratorService){
        this.administratorService = administratorService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){
        System.out.println("Próba logowania do Panelu Admina dla: " + loginRequest.getEmail());

        Administrator loggedInAdmin = administratorService.authenticate(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        if (loggedInAdmin != null){
            System.out.println("Sukces! Administrator zalogowany pomyślnie.");
            return ResponseEntity.ok(loggedInAdmin);
        } else {
            System.out.println("Błąd logowania do panelu administratora (złe dane).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Błędny email lub hasło administratora");
        }
    }
}