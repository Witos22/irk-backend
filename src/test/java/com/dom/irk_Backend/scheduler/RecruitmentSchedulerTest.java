package com.dom.irk_Backend.scheduler;

import com.dom.irk_Backend.model.Recruitment;
import com.dom.irk_Backend.repository.RecruitmentRepository;
import com.dom.irk_Backend.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe automatycznego zamykania rekrutacji (scheduler).
 * SCRUM-17 - testy automatycznego zamykania rekrutacji i tworzenia rankingu.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecruitmentSchedulerTest {

    @Mock
    private RecruitmentRepository recruitmentRepository;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private RecruitmentScheduler scheduler;

    private Recruitment recruitment(int id) {
        Recruitment r = new Recruitment();
        r.setId(id);
        return r;
    }

    @Test
    @DisplayName("SCRUM-17: Scheduler: przelicza wyniki wszystkich zakonczonych rekrutacji")
    void shouldProcessAllEndedRecruitments() {
        when(recruitmentRepository.findByEndDateBeforeAndIsActiveTrue(any(LocalDate.class)))
                .thenReturn(List.of(recruitment(1), recruitment(2)));

        scheduler.processEndedRecruitmentsAutomatically();

        verify(applicationService).processRecruitmentResults(1);
        verify(applicationService).processRecruitmentResults(2);
    }

    @Test
    @DisplayName("SCRUM-17: Scheduler: szuka rekrutacji z data zakonczenia przed dzisiejsza")
    void shouldQueryWithTodaysDate() {
        when(recruitmentRepository.findByEndDateBeforeAndIsActiveTrue(any(LocalDate.class)))
                .thenReturn(List.of());

        scheduler.processEndedRecruitmentsAutomatically();

        verify(recruitmentRepository).findByEndDateBeforeAndIsActiveTrue(LocalDate.now());
    }

    @Test
    @DisplayName("SCRUM-17: Scheduler: brak zakonczonych rekrutacji -> nic nie jest przeliczane")
    void shouldDoNothingWhenNoEndedRecruitments() {
        when(recruitmentRepository.findByEndDateBeforeAndIsActiveTrue(any(LocalDate.class)))
                .thenReturn(List.of());

        scheduler.processEndedRecruitmentsAutomatically();

        verify(applicationService, never()).processRecruitmentResults(anyInt());
    }

    @Test
    @DisplayName("SCRUM-17: Scheduler: blad przy jednej rekrutacji nie blokuje kolejnych")
    void shouldContinueAfterFailure() {
        when(recruitmentRepository.findByEndDateBeforeAndIsActiveTrue(any(LocalDate.class)))
                .thenReturn(List.of(recruitment(1), recruitment(2)));
        doThrow(new IllegalStateException("blad")).when(applicationService).processRecruitmentResults(1);

        scheduler.processEndedRecruitmentsAutomatically();

        verify(applicationService).processRecruitmentResults(1);
        verify(applicationService).processRecruitmentResults(2);
    }
}
