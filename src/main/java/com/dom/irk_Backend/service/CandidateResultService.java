package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.model.CandidateResult;
import com.dom.irk_Backend.model.FullProfileRequest;
import com.dom.irk_Backend.repository.CandidateRepository;
import com.dom.irk_Backend.repository.CandidateResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateResultService {

    private final CandidateResultRepository resultRepository;
    private final CandidateRepository candidateRepository;

    public CandidateResultService(CandidateResultRepository resultRepository, CandidateRepository candidateRepository) {
        this.resultRepository = resultRepository;
        this.candidateRepository = candidateRepository;
    }

    public CandidateResult saveResultForCandidate(String candidateEmail, CandidateResult newResult) {
        // 1. Szukamy kandydata na podstawie maila z sesji
        Candidate candidate = candidateRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono kandydata o emailu: " + candidateEmail));

        // 2. Wiążemy wynik z konkretnym kandydatem
        newResult.setCandidate(candidate);

        // 3. Zapisujemy w bazie
        return resultRepository.save(newResult);
    }

    @Transactional
    public void saveFullProfile(String email, FullProfileRequest request) {
        Candidate candidate = candidateRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono kandydata"));

        resultRepository.deleteByCandidate(candidate);
        // 1. Zapisujemy matury podstawowe
        if (request.getMandatory() != null) {
            request.getMandatory().forEach((subject, score) -> {
                if (score != null) {
                    saveSingleResult(candidate, subject + " (Podstawa)", score);
                }
            });
        }

        // 2. Zapisujemy matury rozszerzone
        if (request.getExtended() != null) {
            request.getExtended().forEach(item -> {
                if (item.getSubject() != null && !item.getSubject().isEmpty() && item.getValue() != null) {
                    saveSingleResult(candidate, item.getSubject() + " (Rozszerzenie)", item.getValue());
                }
            });
        }

        // 3. Zapisujemy oceny ze świadectwa
        if (request.getGrades() != null) {
            request.getGrades().forEach(item -> {
                if (item.getSubject() != null && item.getValue() != null) {
                    saveSingleResult(candidate, "Ocena końcowa: " + item.getSubject(), item.getValue());
                }
            });
        }
    }

    private void saveSingleResult(Candidate candidate, String subject, Integer score) {
        CandidateResult result = new CandidateResult();
        result.setCandidate(candidate);
        result.setSubjectName(subject);
        result.setScore(score);
        resultRepository.save(result);
    }
}