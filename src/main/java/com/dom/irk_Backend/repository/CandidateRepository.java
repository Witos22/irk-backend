package com.dom.irk_Backend.repository;

import com.dom.irk_Backend.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Integer> {
    Optional<Candidate> findByEmail(String email);
}
