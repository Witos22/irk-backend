package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.repository.CandidateRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    public CandidateService(CandidateRepository candidateRepository, PasswordEncoder passwordEncoder){
        this.candidateRepository = candidateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Candidate registerCandidate(Candidate newCandidate){
        String rawPassword = newCandidate.getPasswordHash();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        newCandidate.setPasswordHash(encodedPassword);
        return candidateRepository.save(newCandidate);
    }

    public Candidate authenticate(String email, String password){
        Optional<Candidate> candidateOptional = candidateRepository.findByEmail(email);

        if(candidateOptional.isPresent()){
            Candidate candidate = candidateOptional.get();

            if(passwordEncoder.matches(password, candidate.getPasswordHash())){
                return candidate;
            }
        }
        return null;
    }

    public boolean emailExists(String email) {
        return candidateRepository.findByEmail(email).isPresent();
    }
}
