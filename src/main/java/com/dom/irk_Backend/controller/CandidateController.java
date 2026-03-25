package com.dom.irk_Backend.controller;

import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.model.LoginRequest;
import com.dom.irk_Backend.service.CandidateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService){
        this.candidateService = candidateService;
    }

    @PostMapping("/register")
    public Candidate register(@RequestBody Candidate candidate){
        System.out.println("Połączenie z Reactem nawiązane! Dowód - Imię kandydata: " + candidate.getFirstName());

        return candidateService.registerCandidate(candidate);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){
        System.out.println("Próba logowania dla: " + loginRequest.getEmail());

        Candidate loggedInCandidate = candidateService.authenticate(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        if (loggedInCandidate != null){
            System.out.println("Sukces! Zalogowano pomyślnie.");
            return ResponseEntity.ok(loggedInCandidate);
        } else {
            System.out.println("Błąd logowania, złe dane.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Błędny email lub hasło");
        }
    }
}
