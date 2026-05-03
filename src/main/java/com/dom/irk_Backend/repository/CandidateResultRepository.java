package com.dom.irk_Backend.repository;

import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.model.CandidateResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateResultRepository extends JpaRepository<CandidateResult, Integer> {
    //pobieranie wszystkich wyników dla konkretnego kandydata
    List<CandidateResult> findByCandidateId(Integer candidateId);

    void deleteByCandidate(Candidate candidate);
}