package com.dom.irk_Backend.controller;

import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.service.CandidateService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService){
        this.candidateService = candidateService;
    }

    @PostMapping("/register")
    public Candidate register(@RequestBody Candidate candidate){
        return candidateService.registerCandidate(candidate);
    }
}
