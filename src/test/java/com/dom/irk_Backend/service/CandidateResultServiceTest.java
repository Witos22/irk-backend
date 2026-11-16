package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.model.CandidateResult;
import com.dom.irk_Backend.model.FullProfileRequest;
import com.dom.irk_Backend.repository.CandidateRepository;
import com.dom.irk_Backend.repository.CandidateResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe profilu kandydata: zapis i walidacja matur oraz ocen, odczyt profilu.
 * Dane z profilu sa podstawa punktacji w rankingu (patrz SCRUM-29).
 * SCRUM-19 - uzupelnienie pokrycia kodu kluczowych procesow biznesowych.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CandidateResultServiceTest {

    private static final String EMAIL = "jan.kowalski@example.com";
    private static final String GRADE_PREFIX = "Ocena końcowa: ";

    @Mock
    private CandidateResultRepository resultRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private CandidateResultService candidateResultService;

    private Candidate candidate;

    private void givenCandidateExists() {
        candidate = new Candidate();
        candidate.setId(1);
        candidate.setEmail(EMAIL);
        when(candidateRepository.findByEmail(EMAIL)).thenReturn(Optional.of(candidate));
    }

    private FullProfileRequest.SubjectScore score(String subject, Integer value) {
        FullProfileRequest.SubjectScore s = new FullProfileRequest.SubjectScore();
        s.setSubject(subject);
        s.setValue(value);
        return s;
    }

    private CandidateResult result(String subject, int score) {
        CandidateResult r = new CandidateResult();
        r.setSubjectName(subject);
        r.setScore(score);
        return r;
    }

    // ---------- saveResultForCandidate ----------

    @Test
    @DisplayName("SCRUM-19: Pojedynczy wynik: zostaje przypisany do kandydata i zapisany")
    void shouldSaveSingleResultForCandidate() {
        givenCandidateExists();
        when(resultRepository.save(any(CandidateResult.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateResult saved = candidateResultService.saveResultForCandidate(EMAIL, result("Matematyka (Podstawa)", 80));

        assertThat(saved.getCandidate()).isSameAs(candidate);
    }

    @Test
    @DisplayName("SCRUM-19: Pojedynczy wynik: nieznany kandydat -> RuntimeException")
    void shouldFailSavingResultForUnknownCandidate() {
        when(candidateRepository.findByEmail("brak@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateResultService.saveResultForCandidate("brak@example.com", new CandidateResult()))
                .isInstanceOf(RuntimeException.class);
        verify(resultRepository, never()).save(any(CandidateResult.class));
    }

    // ---------- saveFullProfile ----------

    @Test
    @DisplayName("SCRUM-19: Profil: czysci stare wyniki i zapisuje matury podstawowe, rozszerzone oraz oceny")
    void shouldSaveFullProfile() {
        givenCandidateExists();
        FullProfileRequest request = new FullProfileRequest();
        Map<String, Integer> mandatory = new LinkedHashMap<>();
        mandatory.put("Matematyka", 80);
        mandatory.put("Polski", 70);
        mandatory.put("Angielski", null); // pominiety
        request.setMandatory(mandatory);
        request.setExtended(new ArrayList<>(List.of(score("Informatyka", 90))));
        request.setGrades(new ArrayList<>(List.of(score("Fizyka", 5))));

        candidateResultService.saveFullProfile(EMAIL, request);

        verify(resultRepository).deleteByCandidate(candidate);
        ArgumentCaptor<CandidateResult> captor = ArgumentCaptor.forClass(CandidateResult.class);
        verify(resultRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(CandidateResult::getSubjectName)
                .containsExactly("Matematyka (Podstawa)", "Polski (Podstawa)",
                        "Informatyka (Rozszerzenie)", GRADE_PREFIX + "Fizyka");
        assertThat(captor.getAllValues()).allMatch(r -> r.getCandidate() == candidate);
    }

    @Test
    @DisplayName("SCRUM-19: Profil: puste listy (null) tylko czyszcza stare wyniki")
    void shouldHandleEmptyProfile() {
        givenCandidateExists();

        candidateResultService.saveFullProfile(EMAIL, new FullProfileRequest());

        verify(resultRepository).deleteByCandidate(candidate);
        verify(resultRepository, never()).save(any(CandidateResult.class));
    }

    @Test
    @DisplayName("SCRUM-19: Profil: wpisy rozszerzen i ocen z pustym przedmiotem lub bez wartosci sa pomijane")
    void shouldSkipIncompleteEntries() {
        givenCandidateExists();
        FullProfileRequest request = new FullProfileRequest();
        request.setExtended(new ArrayList<>(List.of(
                score("", 50), score(null, 50), score("Chemia", null))));
        request.setGrades(new ArrayList<>(List.of(
                score(null, 4), score("Historia", null))));

        candidateResultService.saveFullProfile(EMAIL, request);

        verify(resultRepository, never()).save(any(CandidateResult.class));
    }

    @Test
    @DisplayName("SCRUM-19: Profil: nieznany kandydat -> RuntimeException")
    void shouldFailSavingProfileForUnknownCandidate() {
        when(candidateRepository.findByEmail("brak@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateResultService.saveFullProfile("brak@example.com", new FullProfileRequest()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Walidacja: matura podstawowa powyzej 100 jest odrzucana")
    void shouldRejectMandatoryScoreAbove100() {
        givenCandidateExists();
        FullProfileRequest request = new FullProfileRequest();
        request.setMandatory(new LinkedHashMap<>(Map.of("Matematyka", 101)));

        assertThatThrownBy(() -> candidateResultService.saveFullProfile(EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Walidacja: matura podstawowa ponizej 0 jest odrzucana")
    void shouldRejectNegativeMandatoryScore() {
        givenCandidateExists();
        FullProfileRequest request = new FullProfileRequest();
        request.setMandatory(new LinkedHashMap<>(Map.of("Matematyka", -1)));

        assertThatThrownBy(() -> candidateResultService.saveFullProfile(EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Walidacja: wartosci graniczne 0 i 100 sa akceptowane")
    void shouldAcceptBoundaryExamScores() {
        givenCandidateExists();
        FullProfileRequest request = new FullProfileRequest();
        Map<String, Integer> mandatory = new LinkedHashMap<>();
        mandatory.put("Matematyka", 0);
        mandatory.put("Polski", 100);
        request.setMandatory(mandatory);

        candidateResultService.saveFullProfile(EMAIL, request);

        verify(resultRepository, times(2)).save(any(CandidateResult.class));
    }

    @Test
    @DisplayName("SCRUM-19: Walidacja: matura rozszerzona poza zakresem 0-100 jest odrzucana")
    void shouldRejectExtendedScoreOutOfRange() {
        givenCandidateExists();
        FullProfileRequest request = new FullProfileRequest();
        request.setExtended(new ArrayList<>(List.of(score("Informatyka", 150))));

        assertThatThrownBy(() -> candidateResultService.saveFullProfile(EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Walidacja: ocena ze swiadectwa 1 jest odrzucana")
    void shouldRejectGradeBelowTwo() {
        givenCandidateExists();
        FullProfileRequest request = new FullProfileRequest();
        request.setGrades(new ArrayList<>(List.of(score("Fizyka", 1))));

        assertThatThrownBy(() -> candidateResultService.saveFullProfile(EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Walidacja: ocena ze swiadectwa 7 jest odrzucana")
    void shouldRejectGradeAboveSix() {
        givenCandidateExists();
        FullProfileRequest request = new FullProfileRequest();
        request.setGrades(new ArrayList<>(List.of(score("Fizyka", 7))));

        assertThatThrownBy(() -> candidateResultService.saveFullProfile(EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- getMyProfile ----------

    @Test
    @DisplayName("SCRUM-19: Odczyt profilu: wyniki trafiaja do wlasciwych sekcji, nieznane sa pomijane")
    void shouldBuildProfileFromResults() {
        givenCandidateExists();
        when(resultRepository.findByCandidateId(1)).thenReturn(List.of(
                result("Matematyka (Podstawa)", 70),
                result("Informatyka (Rozszerzenie)", 90),
                result(GRADE_PREFIX + "Historia", 5),
                result("Inny wpis", 1)));

        FullProfileRequest profile = candidateResultService.getMyProfile(EMAIL);

        assertThat(profile.getMandatory()).containsEntry("Matematyka", 70).hasSize(1);
        assertThat(profile.getExtended()).hasSize(1);
        assertThat(profile.getExtended().get(0).getSubject()).isEqualTo("Informatyka");
        assertThat(profile.getExtended().get(0).getValue()).isEqualTo(90);
        assertThat(profile.getGrades()).hasSize(1);
        assertThat(profile.getGrades().get(0).getSubject()).isEqualTo("Historia");
        assertThat(profile.getGrades().get(0).getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("SCRUM-19: Odczyt profilu: kandydat bez wynikow dostaje puste sekcje")
    void shouldReturnEmptyProfileWithoutResults() {
        givenCandidateExists();
        when(resultRepository.findByCandidateId(1)).thenReturn(List.of());

        FullProfileRequest profile = candidateResultService.getMyProfile(EMAIL);

        assertThat(profile.getMandatory()).isEmpty();
        assertThat(profile.getExtended()).isEmpty();
        assertThat(profile.getGrades()).isEmpty();
    }

    @Test
    @DisplayName("SCRUM-19: Odczyt profilu: nieznany kandydat -> RuntimeException")
    void shouldFailReadingProfileOfUnknownCandidate() {
        when(candidateRepository.findByEmail("brak@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateResultService.getMyProfile("brak@example.com"))
                .isInstanceOf(RuntimeException.class);
    }
}
