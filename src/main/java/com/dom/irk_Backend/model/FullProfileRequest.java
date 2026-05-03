package com.dom.irk_Backend.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class FullProfileRequest {
    private Map<String, Integer> mandatory; // polski, matematyka, angielski
    private List<SubjectScore> extended;    // lista rozszerzeń
    private List<SubjectScore> grades;      // lista ocen

    @Data
    public static class SubjectScore {
        private String subject;
        private Integer value;
    }
}