package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Course;
import com.dom.irk_Backend.model.CourseRequirement;
import com.dom.irk_Backend.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe zarzadzania kierunkami studiow (tworzenie, edycja, walidacja).
 * SCRUM-19 - uzupelnienie pokrycia kodu kluczowych procesow biznesowych.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course course(String name, Integer places) {
        Course c = new Course();
        c.setName(name);
        c.setPlacesLimit(places);
        return c;
    }

    private CourseRequirement requirement(String subject, double multiplier) {
        CourseRequirement r = new CourseRequirement();
        r.setSubjectName(subject);
        r.setMultiplier(multiplier);
        return r;
    }

    private void assertCreateRejected(Course course) {
        assertThatThrownBy(() -> courseService.createCourse(course)).isInstanceOf(IllegalArgumentException.class);
        verify(courseRepository, never()).save(any(Course.class));
    }

    // ---------- tworzenie ----------

    @Test
    @DisplayName("SCRUM-19: Kierunek: poprawne dane -> zapis, wymagania powiazane z kierunkiem")
    void shouldCreateValidCourse() {
        Course c = course("Informatyka", 30);
        CourseRequirement req = requirement("Matematyka", 1.5);
        c.setRequirements(new ArrayList<>(List.of(req)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course saved = courseService.createCourse(c);

        assertThat(saved.getName()).isEqualTo("Informatyka");
        assertThat(req.getCourse()).isSameAs(c);
        verify(courseRepository).save(c);
    }

    @Test
    @DisplayName("SCRUM-19: Kierunek: tworzenie bez listy wymagan dziala")
    void shouldCreateCourseWithoutRequirements() {
        Course c = course("Prawo", 20);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(courseService.createCourse(c)).isSameAs(c);
    }

    @Test
    @DisplayName("SCRUM-19: Kierunek: pusta nazwa jest odrzucana")
    void shouldRejectBlankName() {
        assertCreateRejected(course("  ", 10));
    }

    @Test
    @DisplayName("SCRUM-19: Kierunek: brak nazwy (null) jest odrzucany")
    void shouldRejectNullName() {
        assertCreateRejected(course(null, 10));
    }

    @Test
    @DisplayName("SCRUM-19: Kierunek: nazwa z cyframi jest odrzucana")
    void shouldRejectNameWithDigits() {
        assertCreateRejected(course("Informatyka 2", 10));
    }

    @Test
    @DisplayName("SCRUM-19: Kierunek: limit miejsc rowny 0 jest odrzucany")
    void shouldRejectZeroPlacesLimit() {
        assertCreateRejected(course("Informatyka", 0));
    }

    @Test
    @DisplayName("SCRUM-19: Kierunek: brak limitu miejsc (null) jest odrzucany")
    void shouldRejectNullPlacesLimit() {
        assertCreateRejected(course("Informatyka", null));
    }

    @Test
    @DisplayName("SCRUM-19: Kierunek: duplikat nazwy (bez wzgledu na wielkosc liter) jest odrzucany")
    void shouldRejectDuplicateName() {
        Course existing = course("Informatyka", 10);
        existing.setId(1);
        when(courseRepository.findByNameIgnoreCase("informatyka")).thenReturn(Optional.of(existing));

        assertCreateRejected(course("informatyka", 15));
    }

    // ---------- edycja ----------

    @Test
    @DisplayName("SCRUM-19: Edycja: zmienia nazwe, limit i podmienia wymagania")
    void shouldUpdateCourse() {
        Course existing = course("Informatyka", 10);
        existing.setId(1);
        existing.setRequirements(new ArrayList<>(List.of(requirement("Fizyka", 1.0))));
        when(courseRepository.findById(1)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course updated = course("Informatyka stosowana", 25);
        CourseRequirement newReq = requirement("Matematyka", 2.0);
        newReq.setId(77);
        updated.setRequirements(new ArrayList<>(List.of(newReq)));

        Course result = courseService.updateCourse(1, updated);

        assertThat(result.getName()).isEqualTo("Informatyka stosowana");
        assertThat(result.getPlacesLimit()).isEqualTo(25);
        assertThat(result.getRequirements()).hasSize(1);
        assertThat(result.getRequirements().get(0).getSubjectName()).isEqualTo("Matematyka");
        assertThat(result.getRequirements().get(0).getId()).isNull();
        assertThat(result.getRequirements().get(0).getCourse()).isSameAs(existing);
    }

    @Test
    @DisplayName("SCRUM-19: Edycja: ta sama nazwa tego samego kierunku nie jest duplikatem")
    void shouldAllowKeepingOwnName() {
        Course existing = course("Informatyka", 10);
        existing.setId(1);
        existing.setRequirements(new ArrayList<>());
        when(courseRepository.findByNameIgnoreCase("Informatyka")).thenReturn(Optional.of(existing));
        when(courseRepository.findById(1)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseService.updateCourse(1, course("Informatyka", 40));

        assertThat(result.getPlacesLimit()).isEqualTo(40);
    }

    @Test
    @DisplayName("SCRUM-19: Edycja: nazwa zajeta przez inny kierunek jest odrzucana")
    void shouldRejectNameUsedByAnotherCourse() {
        Course other = course("Informatyka", 10);
        other.setId(2);
        when(courseRepository.findByNameIgnoreCase("Informatyka")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> courseService.updateCourse(1, course("Informatyka", 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Edycja: nieistniejacy kierunek -> RuntimeException")
    void shouldFailUpdatingMissingCourse() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse(99, course("Chemia", 10)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Edycja: brak wymagan w danych i w kierunku nie powoduje bledu")
    void shouldUpdateCourseWithoutRequirements() {
        Course existing = course("Chemia", 10);
        existing.setId(3);
        when(courseRepository.findById(3)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseService.updateCourse(3, course("Biologia", 12));

        assertThat(result.getName()).isEqualTo("Biologia");
    }

    // ---------- odczyt / usuwanie ----------

    @Test
    @DisplayName("SCRUM-19: getAllCourses zwraca liste z repozytorium")
    void shouldReturnAllCourses() {
        when(courseRepository.findAll()).thenReturn(List.of(course("A", 1), course("B", 2)));

        assertThat(courseService.getAllCourses()).hasSize(2);
    }

    @Test
    @DisplayName("SCRUM-19: deleteCourse usuwa kierunek po ID")
    void shouldDeleteCourse() {
        courseService.deleteCourse(7);

        verify(courseRepository).deleteById(7);
    }
}
