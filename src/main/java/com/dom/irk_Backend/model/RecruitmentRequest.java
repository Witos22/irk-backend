package com.dom.irk_Backend.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RecruitmentRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Integer courseId; // ID kierunku do przypisania
}