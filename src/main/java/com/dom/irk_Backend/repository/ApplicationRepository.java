package com.dom.irk_Backend.repository;

import com.dom.irk_Backend.model.Application;
import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.model.Recruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    // Sprawdza, czy kandydat już aplikował na tę rekrutację
    boolean existsByCandidateAndRecruitment(Candidate candidate, Recruitment recruitment);

    // Pobiera wszystkie aplikacje danego kandydata
    List<Application> findByCandidate(Candidate candidate);
}