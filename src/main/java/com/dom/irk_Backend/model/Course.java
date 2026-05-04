package com.dom.irk_Backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer placesLimit;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "recruitment_id", nullable = true)
    private Recruitment recruitment;

    @Transient
    @JsonProperty("status")
    public String getStatus() {
        LocalDate now = LocalDate.now();

        if (placesLimit != null && placesLimit <= 0) {
            return "BRAK_MIEJSC";
        }

        if (startDate != null && now.isBefore(startDate)) {
            return "PLANOWANY";
        }

        if (endDate != null && now.isAfter(endDate)) {
            return "ZAKOŃCZONY";
        }

        if (startDate != null && endDate != null) {
            return "AKTYWNY";
        }

        return "NIEKOMPLETNY";
    }
}