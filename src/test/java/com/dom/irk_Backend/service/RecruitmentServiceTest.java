package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Course;
import com.dom.irk_Backend.model.Recruitment;
import com.dom.irk_Backend.model.RecruitmentRequest;
import com.dom.irk_Backend.repository.CourseRepository;
import com.dom.irk_Backend.repository.RecruitmentRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe zarzadzania rekrutacjami (tworzenie, edycja, daty, kierunek).
 * SCRUM-19 - uzupelnienie pokrycia kodu kluczowych procesow biznesowych.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecruitmentServiceTest {

    @Mock
    private RecruitmentRepository recruitmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private RecruitmentService recruitmentService;

    private RecruitmentRequest request(LocalDate start, LocalDate end, Boolean active, Integer courseId) {
        RecruitmentRequest r = new RecruitmentRequest();
        r.setName("Rekrutacja 2026");
        r.setStartDate(start);
        r.setEndDate(end);
        r.setIsActive(active);
        r.setCourseId(courseId);
        return r;
    }

    private void stubSaveReturnsArgument() {
        when(recruitmentRepository.save(any(Recruitment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("SCRUM-19: Nowa rekrutacja z kierunkiem: pola i kierunek zostaja ustawione")
    void shouldCreateRecruitmentWithCourse() {
        Course course = new Course();
        course.setId(5);
        when(courseRepository.findById(5)).thenReturn(Optional.of(course));
        stubSaveReturnsArgument();
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 7, 1);

        Recruitment saved = recruitmentService.saveRecruitment(null, request(start, end, true, 5));

        assertThat(saved.getName()).isEqualTo("Rekrutacja 2026");
        assertThat(saved.getStartDate()).isEqualTo(start);
        assertThat(saved.getEndDate()).isEqualTo(end);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getCourse()).isSameAs(course);
    }

    @Test
    @DisplayName("SCRUM-19: Nowa rekrutacja bez isActive i bez kierunku: nieaktywna, kierunek null")
    void shouldDefaultToInactiveWithoutCourse() {
        stubSaveReturnsArgument();

        Recruitment saved = recruitmentService.saveRecruitment(null,
                request(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), null, null));

        assertThat(saved.getIsActive()).isFalse();
        assertThat(saved.getCourse()).isNull();
    }

    @Test
    @DisplayName("SCRUM-19: Rekrutacja: data zakonczenia przed data rozpoczecia jest odrzucana")
    void shouldRejectEndBeforeStart() {
        RecruitmentRequest bad = request(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 1), true, null);

        assertThatThrownBy(() -> recruitmentService.saveRecruitment(null, bad))
                .isInstanceOf(IllegalArgumentException.class);
        verify(recruitmentRepository, never()).save(any(Recruitment.class));
    }

    @Test
    @DisplayName("SCRUM-19: Edycja istniejacej rekrutacji aktualizuje jej pola")
    void shouldUpdateExistingRecruitment() {
        Recruitment existing = new Recruitment();
        existing.setId(3);
        existing.setName("Stara");
        when(recruitmentRepository.findById(3)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();

        Recruitment saved = recruitmentService.saveRecruitment(3,
                request(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true, null));

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getName()).isEqualTo("Rekrutacja 2026");
    }

    @Test
    @DisplayName("SCRUM-19: Edycja nieistniejacej rekrutacji -> RuntimeException")
    void shouldFailForMissingRecruitment() {
        when(recruitmentRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recruitmentService.saveRecruitment(404,
                request(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true, null)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Rekrutacja z nieistniejacym kierunkiem -> RuntimeException")
    void shouldFailForMissingCourse() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recruitmentService.saveRecruitment(null,
                request(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), true, 99)))
                .isInstanceOf(RuntimeException.class);
        verify(recruitmentRepository, never()).save(any(Recruitment.class));
    }

    @Test
    @DisplayName("SCRUM-19: getAllRecruitments zwraca liste z repozytorium")
    void shouldReturnAllRecruitments() {
        when(recruitmentRepository.findAll()).thenReturn(List.of(new Recruitment(), new Recruitment()));

        assertThat(recruitmentService.getAllRecruitments()).hasSize(2);
    }

    @Test
    @DisplayName("SCRUM-19: getActiveRecruitments pyta repozytorium o aktywne rekrutacje do dzisiaj")
    void shouldReturnActiveRecruitments() {
        Recruitment r = new Recruitment();
        when(recruitmentRepository.findByIsActiveTrueAndEndDateGreaterThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(r));

        assertThat(recruitmentService.getActiveRecruitments()).containsExactly(r);
        verify(recruitmentRepository).findByIsActiveTrueAndEndDateGreaterThanEqual(LocalDate.now());
    }

    @Test
    @DisplayName("SCRUM-19: deleteRecruitment usuwa rekrutacje po ID")
    void shouldDeleteRecruitment() {
        recruitmentService.deleteRecruitment(8);

        verify(recruitmentRepository).deleteById(8);
    }
}
