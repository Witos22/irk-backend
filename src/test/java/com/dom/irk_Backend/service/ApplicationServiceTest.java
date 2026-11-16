package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Application;
import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.model.CandidateResult;
import com.dom.irk_Backend.model.Course;
import com.dom.irk_Backend.model.CourseRequirement;
import com.dom.irk_Backend.model.Recruitment;
import com.dom.irk_Backend.repository.ApplicationRepository;
import com.dom.irk_Backend.repository.CandidateRepository;
import com.dom.irk_Backend.repository.CandidateResultRepository;
import com.dom.irk_Backend.repository.DocumentRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe ApplicationService (Mockito).
 *
 * SCRUM-17 - testy automatycznego zamykania rekrutacji i tworzenia rankingu:
 *   SCRUM-29 - tworzenie rankingu kandydatow (punktacja, kolejnosc, lista glowna i rezerwowa),
 *   SCRUM-30 - obsluga remisow w rankingu kandydatow (rowna liczba punktow),
 *   SCRUM-17 - zamkniecie rekrutacji po przeliczeniu (dezaktywacja, bledy danych wejsciowych).
 * SCRUM-19 - testy uzupelniajace pokrycie kodu (odczyt aplikacji, zmiana statusu, bledy skladania aplikacji).
 *
 * Testy walidacji skladania aplikacji (SCRUM-20 do SCRUM-23) sa w ApplicationProcessValidationTest.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceTest {

    private static final String EMAIL = "jan.kowalski@example.com";
    private static final String STATUS_QUALIFIED = "ZAKWALIFIKOWANY";
    private static final String STATUS_RESERVE = "LISTA REZERWOWA";
    private static final String STATUS_REJECTED = "ODRZUCONY";

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private RecruitmentRepository recruitmentRepository;
    @Mock
    private CandidateResultRepository resultRepository;
    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private ApplicationService applicationService;

    // ---------- pomocnicze ----------

    private Candidate candidate(int id) {
        Candidate c = new Candidate();
        c.setId(id);
        c.setEmail(EMAIL);
        return c;
    }

    private CandidateResult result(String subject, int score) {
        CandidateResult r = new CandidateResult();
        r.setSubjectName(subject);
        r.setScore(score);
        return r;
    }

    private CourseRequirement requirement(String subject, double multiplier) {
        CourseRequirement r = new CourseRequirement();
        r.setSubjectName(subject);
        r.setMultiplier(multiplier);
        return r;
    }

    private Course course(Integer placesLimit, CourseRequirement... requirements) {
        Course c = new Course();
        c.setName("Informatyka");
        c.setPlacesLimit(placesLimit);
        c.setRequirements(new ArrayList<>(List.of(requirements)));
        return c;
    }

    private Recruitment openRecruitment(Course course) {
        Recruitment r = new Recruitment();
        r.setId(10);
        r.setIsActive(true);
        r.setStartDate(LocalDate.now().minusDays(1));
        r.setEndDate(LocalDate.now().plusDays(1));
        r.setCourse(course);
        return r;
    }

    private Application applicationWithPoints(Integer points) {
        Application a = new Application();
        a.setPoints(points);
        a.setStatus("ZŁOŻONA");
        return a;
    }

    private void givenApplyScenario(Recruitment recruitment, List<CandidateResult> results) {
        when(candidateRepository.findByEmail(EMAIL)).thenReturn(Optional.of(candidate(1)));
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        when(resultRepository.findByCandidateId(1)).thenReturn(results);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- SCRUM-29: punktacja aplikacji (podstawa rankingu) oraz bledy skladania aplikacji ----------

    @Test
    @DisplayName("SCRUM-29: Punkty: 50% matur podstawowych + najlepsze rozszerzenie z mnoznikiem kierunku")
    void shouldCalculatePointsFromBaseAndBestExtended() {
        Course course = course(10, requirement("Matematyka", 1.0), requirement("Fizyka", 2.0));
        givenApplyScenario(openRecruitment(course), List.of(
                result("Matematyka (Podstawa)", 80),      // 40
                result("Polski (Podstawa)", 60),          // 30
                result("Matematyka (Rozszerzenie)", 90),  // 90 * 1.0 = 90
                result("Fizyka (Rozszerzenie)", 50)));    // 50 * 2.0 = 100 -> najlepsze

        Application app = applicationService.applyForRecruitment(EMAIL, 10);

        assertThat(app.getPoints()).isEqualTo(170); // 40 + 30 + 100
        assertThat(app.getStatus()).isEqualTo("ZŁOŻONA");
    }

    @Test
    @DisplayName("SCRUM-29: Punkty: rozszerzenie spoza wymagan kierunku nie jest liczone")
    void shouldIgnoreExtendedSubjectNotRequiredByCourse() {
        Course course = course(10, requirement("Matematyka", 1.0));
        givenApplyScenario(openRecruitment(course), List.of(
                result("Matematyka (Podstawa)", 80),
                result("Biologia (Rozszerzenie)", 100)));

        Application app = applicationService.applyForRecruitment(EMAIL, 10);

        assertThat(app.getPoints()).isEqualTo(40);
    }

    @Test
    @DisplayName("SCRUM-29: Punkty: rekrutacja bez kierunku liczy tylko matury podstawowe")
    void shouldCalculateOnlyBasePointsWhenNoCourse() {
        givenApplyScenario(openRecruitment(null), List.of(result("Matematyka (Podstawa)", 90)));

        Application app = applicationService.applyForRecruitment(EMAIL, 10);

        assertThat(app.getPoints()).isEqualTo(45);
    }

    @Test
    @DisplayName("SCRUM-19: Aplikacja: tworzy trzy dokumenty ze statusem 'Niedostarczone'")
    void shouldCreateThreeUndeliveredDocuments() {
        givenApplyScenario(openRecruitment(course(10)), List.of(result("Matematyka (Podstawa)", 50)));

        Application app = applicationService.applyForRecruitment(EMAIL, 10);

        assertThat(app.getDocuments()).hasSize(3);
        assertThat(app.getDocuments()).allMatch(d -> "Niedostarczone".equals(d.getStatus()));
        assertThat(app.getDocuments()).allMatch(d -> d.getApplication() == app);
    }

    @Test
    @DisplayName("SCRUM-19: Aplikacja: nieznany kandydat -> RuntimeException")
    void shouldFailForUnknownCandidate() {
        when(candidateRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.applyForRecruitment(EMAIL, 10))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Aplikacja: nieznana rekrutacja -> RuntimeException")
    void shouldFailForUnknownRecruitment() {
        when(candidateRepository.findByEmail(EMAIL)).thenReturn(Optional.of(candidate(1)));
        when(recruitmentRepository.findById(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.applyForRecruitment(EMAIL, 10))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Aplikacja: brak wynikow egzaminow (pusta lista) -> IllegalStateException")
    void shouldRejectWhenResultsEmpty() {
        givenApplyScenario(openRecruitment(course(10)), List.of());

        assertThatThrownBy(() -> applicationService.applyForRecruitment(EMAIL, 10))
                .isInstanceOf(IllegalStateException.class);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("SCRUM-19: Aplikacja: brak wynikow egzaminow (null) -> IllegalStateException")
    void shouldRejectWhenResultsNull() {
        givenApplyScenario(openRecruitment(course(10)), null);

        assertThatThrownBy(() -> applicationService.applyForRecruitment(EMAIL, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- SCRUM-29 / SCRUM-17: ranking i zamkniecie rekrutacji (processRecruitmentResults) ----------

    @Test
    @DisplayName("SCRUM-29: Ranking: limit 2 + 50% rezerwy -> 2 zakwalifikowanych, 1 rezerwowy, reszta odrzucona")
    void shouldAssignQualifiedReserveAndRejected() {
        Recruitment recruitment = openRecruitment(course(2));
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        Application a50 = applicationWithPoints(50);
        Application a90 = applicationWithPoints(90);
        Application a70 = applicationWithPoints(70);
        Application a10 = applicationWithPoints(10);
        Application a30 = applicationWithPoints(30);
        when(applicationRepository.findByRecruitmentId(10))
                .thenReturn(new ArrayList<>(List.of(a50, a90, a70, a10, a30)));

        applicationService.processRecruitmentResults(10);

        assertThat(a90.getStatus()).isEqualTo(STATUS_QUALIFIED);
        assertThat(a70.getStatus()).isEqualTo(STATUS_QUALIFIED);
        assertThat(a50.getStatus()).isEqualTo(STATUS_RESERVE);
        assertThat(a30.getStatus()).isEqualTo(STATUS_REJECTED);
        assertThat(a10.getStatus()).isEqualTo(STATUS_REJECTED);
        verify(applicationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("SCRUM-29: Ranking: limit 3 -> lista rezerwowa zaokraglona w gore (2 miejsca)")
    void shouldRoundReserveListUp() {
        Recruitment recruitment = openRecruitment(course(3));
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        List<Application> apps = new ArrayList<>();
        for (int points = 100; points >= 60; points -= 10) { // 100, 90, 80, 70, 60
            apps.add(applicationWithPoints(points));
        }
        when(applicationRepository.findByRecruitmentId(10)).thenReturn(apps);

        applicationService.processRecruitmentResults(10);

        assertThat(apps).extracting(Application::getStatus).containsExactly(
                STATUS_QUALIFIED, STATUS_QUALIFIED, STATUS_QUALIFIED, STATUS_RESERVE, STATUS_RESERVE);
    }

    @Test
    @DisplayName("SCRUM-29: Ranking: brak punktow (null) jest traktowany jak 0 i laduje na koncu")
    void shouldTreatNullPointsAsZero() {
        Recruitment recruitment = openRecruitment(course(1));
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        Application noPoints = applicationWithPoints(null);
        Application withPoints = applicationWithPoints(5);
        when(applicationRepository.findByRecruitmentId(10))
                .thenReturn(new ArrayList<>(List.of(noPoints, withPoints)));

        applicationService.processRecruitmentResults(10);

        assertThat(withPoints.getStatus()).isEqualTo(STATUS_QUALIFIED);
        assertThat(noPoints.getStatus()).isEqualTo(STATUS_RESERVE);
    }

    @Test
    @DisplayName("SCRUM-17: Zamkniecie: rekrutacja zostaje dezaktywowana po przeliczeniu")
    void shouldDeactivateRecruitmentAfterProcessing() {
        Recruitment recruitment = openRecruitment(course(2));
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        when(applicationRepository.findByRecruitmentId(10))
                .thenReturn(new ArrayList<>(List.of(applicationWithPoints(10))));

        applicationService.processRecruitmentResults(10);

        assertThat(recruitment.getIsActive()).isFalse();
        verify(recruitmentRepository).save(recruitment);
    }

    @Test
    @DisplayName("SCRUM-17: Zamkniecie: rekrutacja bez aplikacji jest tylko dezaktywowana")
    void shouldDeactivateRecruitmentWithoutApplications() {
        Recruitment recruitment = openRecruitment(course(2));
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        when(applicationRepository.findByRecruitmentId(10)).thenReturn(new ArrayList<>());

        applicationService.processRecruitmentResults(10);

        assertThat(recruitment.getIsActive()).isFalse();
        verify(applicationRepository, never()).saveAll(anyList());
        verify(recruitmentRepository).save(recruitment);
    }

    @Test
    @DisplayName("SCRUM-17: Zamkniecie: nieznana rekrutacja -> RuntimeException")
    void shouldFailProcessingUnknownRecruitment() {
        when(recruitmentRepository.findById(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.processRecruitmentResults(10))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("SCRUM-17: Zamkniecie: rekrutacja bez kierunku -> IllegalStateException")
    void shouldFailProcessingRecruitmentWithoutCourse() {
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(openRecruitment(null)));

        assertThatThrownBy(() -> applicationService.processRecruitmentResults(10))
                .isInstanceOf(IllegalStateException.class);
        verify(recruitmentRepository, never()).save(any(Recruitment.class));
    }

    @Test
    @DisplayName("SCRUM-17: Zamkniecie: kierunek bez limitu miejsc -> IllegalStateException")
    void shouldFailProcessingCourseWithoutPlacesLimit() {
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(openRecruitment(course(null))));

        assertThatThrownBy(() -> applicationService.processRecruitmentResults(10))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- SCRUM-30: obsluga remisow w rankingu kandydatow ----------
    //
    // Algorytm sortuje aplikacje malejaco po punktach (sortowanie stabilne), a dodatkowego kryterium
    // rozstrzygajacego remis nie ma - przy rownej liczbie punktow o miejscu decyduje kolejnosc,
    // w jakiej aplikacje zwrocilo repozytorium. Testy ponizej opisuja to zachowanie.

    @Test
    @DisplayName("SCRUM-30: Remis wsrod zakwalifikowanych - wszyscy z rownymi punktami zachowuja kolejnosc z repozytorium")
    void shouldKeepRepositoryOrderForEqualPoints() {
        Recruitment recruitment = openRecruitment(course(2)); // 2 miejsca + 1 rezerwowe
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        Application first = applicationWithPoints(80);
        Application second = applicationWithPoints(80);
        Application third = applicationWithPoints(80);
        Application last = applicationWithPoints(60);
        when(applicationRepository.findByRecruitmentId(10))
                .thenReturn(new ArrayList<>(List.of(first, second, third, last)));

        applicationService.processRecruitmentResults(10);

        assertThat(first.getStatus()).isEqualTo(STATUS_QUALIFIED);
        assertThat(second.getStatus()).isEqualTo(STATUS_QUALIFIED);
        assertThat(third.getStatus()).isEqualTo(STATUS_RESERVE);
        assertThat(last.getStatus()).isEqualTo(STATUS_REJECTED);
    }

    @Test
    @DisplayName("SCRUM-30: Remis na granicy limitu miejsc - zakwalifikowany jest ten, ktory byl wczesniej na liscie")
    void shouldResolveTieOnQualificationBoundaryByOrder() {
        Recruitment recruitment = openRecruitment(course(1)); // 1 miejsce + 1 rezerwowe
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        Application earlier = applicationWithPoints(90);
        Application later = applicationWithPoints(90);
        when(applicationRepository.findByRecruitmentId(10))
                .thenReturn(new ArrayList<>(List.of(earlier, later)));

        applicationService.processRecruitmentResults(10);

        assertThat(earlier.getStatus()).isEqualTo(STATUS_QUALIFIED);
        assertThat(later.getStatus()).isEqualTo(STATUS_RESERVE);
    }

    @Test
    @DisplayName("SCRUM-30: Wszyscy kandydaci z identyczna liczba punktow - podzial na liste glowna, rezerwowa i odrzuconych")
    void shouldSplitListWhenAllCandidatesHaveEqualPoints() {
        Recruitment recruitment = openRecruitment(course(3)); // 3 miejsca + 2 rezerwowe
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        List<Application> apps = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            apps.add(applicationWithPoints(70));
        }
        when(applicationRepository.findByRecruitmentId(10)).thenReturn(apps);

        applicationService.processRecruitmentResults(10);

        assertThat(apps).extracting(Application::getStatus).containsExactly(
                STATUS_QUALIFIED, STATUS_QUALIFIED, STATUS_QUALIFIED,
                STATUS_RESERVE, STATUS_RESERVE,
                STATUS_REJECTED);
    }

    @Test
    @DisplayName("SCRUM-30: Brak punktow (null) i 0 punktow to remis - decyduje kolejnosc z repozytorium")
    void shouldTreatNullAndZeroPointsAsTie() {
        Recruitment recruitment = openRecruitment(course(1)); // 1 miejsce + 1 rezerwowe
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        Application nullPoints = applicationWithPoints(null);
        Application zeroPoints = applicationWithPoints(0);
        when(applicationRepository.findByRecruitmentId(10))
                .thenReturn(new ArrayList<>(List.of(nullPoints, zeroPoints)));

        applicationService.processRecruitmentResults(10);

        assertThat(nullPoints.getStatus()).isEqualTo(STATUS_QUALIFIED);
        assertThat(zeroPoints.getStatus()).isEqualTo(STATUS_RESERVE);
    }

    @Test
    @DisplayName("SCRUM-30: Remisy w kilku grupach punktowych nie mieszaja kolejnosci miedzy grupami")
    void shouldHandleSeveralTieGroups() {
        Recruitment recruitment = openRecruitment(course(2)); // 2 miejsca + 1 rezerwowe
        when(recruitmentRepository.findById(10)).thenReturn(Optional.of(recruitment));
        Application low1 = applicationWithPoints(70);
        Application high1 = applicationWithPoints(90);
        Application high2 = applicationWithPoints(90);
        Application low2 = applicationWithPoints(70);
        when(applicationRepository.findByRecruitmentId(10))
                .thenReturn(new ArrayList<>(List.of(low1, high1, high2, low2)));

        applicationService.processRecruitmentResults(10);

        assertThat(high1.getStatus()).isEqualTo(STATUS_QUALIFIED);
        assertThat(high2.getStatus()).isEqualTo(STATUS_QUALIFIED);
        assertThat(low1.getStatus()).isEqualTo(STATUS_RESERVE);
        assertThat(low2.getStatus()).isEqualTo(STATUS_REJECTED);
    }

    // ---------- SCRUM-19: odczyt aplikacji ----------

    @Test
    @DisplayName("SCRUM-19: Aplikacje rekrutacji: zwraca liste z repozytorium")
    void shouldReturnApplicationsForRecruitment() {
        when(applicationRepository.findByRecruitmentId(10))
                .thenReturn(List.of(applicationWithPoints(1), applicationWithPoints(2)));

        assertThat(applicationService.getApplicationsForRecruitment(10)).hasSize(2);
    }

    @Test
    @DisplayName("SCRUM-19: Moje aplikacje: zwraca aplikacje kandydata")
    void shouldReturnMyApplications() {
        Candidate c = candidate(1);
        when(candidateRepository.findByEmail(EMAIL)).thenReturn(Optional.of(c));
        when(applicationRepository.findByCandidate(c)).thenReturn(List.of(applicationWithPoints(1)));

        assertThat(applicationService.getMyApplications(EMAIL)).hasSize(1);
    }

    @Test
    @DisplayName("SCRUM-19: Moje aplikacje: nieznany kandydat -> RuntimeException")
    void shouldFailMyApplicationsForUnknownCandidate() {
        when(candidateRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getMyApplications(EMAIL))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Aplikacje kierunku: dla kandydata z kilkoma aplikacjami zostaje tylko najlepsza")
    void shouldKeepBestApplicationPerCandidate() {
        Application best = applicationWithPoints(120);
        best.setCandidate(candidate(1));
        Application worse = applicationWithPoints(80);
        worse.setCandidate(candidate(1));
        Application other = applicationWithPoints(100);
        other.setCandidate(candidate(2));
        // repozytorium zwraca liste posortowana malejaco po punktach
        when(applicationRepository.findByCourseIdOrderByPointsDesc(7)).thenReturn(List.of(best, other, worse));

        List<Application> result = applicationService.getApplicationsByCourse(7);

        assertThat(result).containsExactly(best, other);
    }

    // ---------- SCRUM-19: zmiana statusu aplikacji ----------

    @Test
    @DisplayName("SCRUM-19: Status aplikacji: zmiana zapisuje nowy status")
    void shouldUpdateApplicationStatus() {
        Application app = applicationWithPoints(10);
        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.updateApplicationStatus(1, STATUS_QUALIFIED);

        assertThat(result.getStatus()).isEqualTo(STATUS_QUALIFIED);
    }

    @Test
    @DisplayName("SCRUM-19: Status aplikacji: nieistniejaca aplikacja -> RuntimeException")
    void shouldFailUpdatingStatusOfMissingApplication() {
        when(applicationRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(404, STATUS_QUALIFIED))
                .isInstanceOf(RuntimeException.class);
        verify(applicationRepository, never()).save(any(Application.class));
    }
}
