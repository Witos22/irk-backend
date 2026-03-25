package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.repository.CandidateRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateService(CandidateRepository candidateRepository){
        this.candidateRepository = candidateRepository;
    }

    public Candidate registerCandidate(Candidate newCandidate){
        return candidateRepository.save(newCandidate);
    }

    public Candidate authenticate(String email, String password){
        Optional<Candidate> candidateOptional = candidateRepository.findByEmail(email);

        if(candidateOptional.isPresent()){
            Candidate candidate = candidateOptional.get();

            if(candidate.getPasswordHash().equals(password)){
                return candidate;
            }
        }
        return null;
    }
}
