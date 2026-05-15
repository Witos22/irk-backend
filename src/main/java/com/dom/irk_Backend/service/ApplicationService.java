package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Application;
import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.model.Recruitment;
import com.dom.irk_Backend.repository.ApplicationRepository;
import com.dom.irk_Backend.repository.CandidateRepository;
import com.dom.irk_Backend.repository.RecruitmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final RecruitmentRepository recruitmentRepository;

    public ApplicationService(ApplicationRepository applicationRepository, CandidateRepository candidateRepository, RecruitmentRepository recruitmentRepository) {
        this.applicationRepository = applicationRepository;
        this.candidateRepository = candidateRepository;
        this.recruitmentRepository = recruitmentRepository;
    }

    public Application applyForRecruitment(String candidateEmail, Integer recruitmentId) {
        Candidate candidate = candidateRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono kandydata."));

        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono rekrutacji."));

        // 1. Walidacja: Czy rekrutacja jest w ogóle aktywna
        if (!recruitment.getIsActive()) {
            throw new IllegalStateException("Ta rekrutacja jest obecnie zamknięta.");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(recruitment.getStartDate()) || today.isAfter(recruitment.getEndDate())) {
            throw new IllegalStateException("Rekrutacja nie odbywa się w obecnym terminie.");
        }

        // 2. Walidacja: Czy kandydat już tu aplikował
        if (applicationRepository.existsByCandidateAndRecruitment(candidate, recruitment)) {
            throw new IllegalStateException("Już złożyłeś aplikację w tej rekrutacji!");
        }

        // 3. Tworzymy aplikację
        Application application = new Application();
        application.setCandidate(candidate);
        application.setRecruitment(recruitment);
        application.setStatus("ZŁOŻONA"); // Domyślny status
        // application.setCreatedAt(...) - ustawi się samo

        return applicationRepository.save(application);
    }

    public List<Application> getApplicationsForRecruitment(Integer recruitmentId) {
        return applicationRepository.findByRecruitmentId(recruitmentId);
    }

    public List<Application> getMyApplications(String candidateEmail) {
        Candidate candidate = candidateRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono kandydata."));
        return applicationRepository.findByCandidate(candidate);
    }
}