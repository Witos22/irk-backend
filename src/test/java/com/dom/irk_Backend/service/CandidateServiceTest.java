package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Candidate;
import com.dom.irk_Backend.repository.CandidateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe procesu rejestracji, logowania i zarzadzania rolami kandydatow.
 * SCRUM-19 - uzupelnienie pokrycia kodu kluczowych procesow biznesowych.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CandidateService candidateService;

    private Candidate validCandidate() {
        Candidate c = new Candidate();
        c.setFirstName("Jan");
        c.setLastName("Kowalski");
        c.setEmail("jan.kowalski@example.com");
        c.setPhoneNumber("123456789");
        c.setPasswordHash("secret123");
        return c;
    }

    private void assertRegistrationRejected(Candidate candidate, Class<? extends Throwable> expected) {
        assertThatThrownBy(() -> candidateService.registerCandidate(candidate)).isInstanceOf(expected);
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    // ---------- rejestracja ----------

    @Test
    @DisplayName("SCRUM-19: Rejestracja: poprawne dane -> haslo zaszyfrowane, rola CANDIDATE, zapis w bazie")
    void shouldRegisterValidCandidate() {
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        when(candidateRepository.findByEmail("jan.kowalski@example.com")).thenReturn(Optional.empty());
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidate saved = candidateService.registerCandidate(validCandidate());

        assertThat(saved.getPasswordHash()).isEqualTo("ENCODED");
        assertThat(saved.getRole()).isEqualTo("CANDIDATE");
        verify(candidateRepository).save(saved);
    }

    @Test
    @DisplayName("SCRUM-19: Rejestracja: puste imie jest odrzucane")
    void shouldRejectBlankFirstName() {
        Candidate c = validCandidate();
        c.setFirstName("   ");
        assertRegistrationRejected(c, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Rejestracja: brak imienia (null) jest odrzucany")
    void shouldRejectNullFirstName() {
        Candidate c = validCandidate();
        c.setFirstName(null);
        assertRegistrationRejected(c, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Rejestracja: puste nazwisko jest odrzucane")
    void shouldRejectBlankLastName() {
        Candidate c = validCandidate();
        c.setLastName("");
        assertRegistrationRejected(c, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Rejestracja: brak nazwiska (null) jest odrzucany")
    void shouldRejectNullLastName() {
        Candidate c = validCandidate();
        c.setLastName(null);
        assertRegistrationRejected(c, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Rejestracja: haslo krotsze niz 6 znakow jest odrzucane")
    void shouldRejectTooShortPassword() {
        Candidate c = validCandidate();
        c.setPasswordHash("12345");
        assertRegistrationRejected(c, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Rejestracja: brak hasla (null) jest odrzucany")
    void shouldRejectNullPassword() {
        Candidate c = validCandidate();
        c.setPasswordHash(null);
        assertRegistrationRejected(c, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Rejestracja: niepoprawny format email jest odrzucany")
    void shouldRejectInvalidEmailFormat() {
        Candidate c = validCandidate();
        c.setEmail("to-nie-jest-email");
        assertRegistrationRejected(c, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Rejestracja: brak email (null) jest odrzucany")
    void shouldRejectNullEmail() {
        Candidate c = validCandidate();
        c.setEmail(null);
        assertRegistrationRejected(c, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Rejestracja: zajety email -> IllegalStateException (kontroler zwraca 409)")
    void shouldRejectAlreadyUsedEmail() {
        when(candidateRepository.findByEmail("jan.kowalski@example.com"))
                .thenReturn(Optional.of(new Candidate()));
        assertRegistrationRejected(validCandidate(), IllegalStateException.class);
    }

    // ---------- logowanie ----------

    @Test
    @DisplayName("SCRUM-19: Logowanie: poprawne haslo zwraca kandydata")
    void shouldAuthenticateWithCorrectPassword() {
        Candidate stored = validCandidate();
        stored.setPasswordHash("HASH");
        when(candidateRepository.findByEmail("jan.kowalski@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("secret123", "HASH")).thenReturn(true);

        assertThat(candidateService.authenticate("jan.kowalski@example.com", "secret123")).isSameAs(stored);
    }

    @Test
    @DisplayName("SCRUM-19: Logowanie: bledne haslo zwraca null")
    void shouldNotAuthenticateWithWrongPassword() {
        Candidate stored = validCandidate();
        stored.setPasswordHash("HASH");
        when(candidateRepository.findByEmail("jan.kowalski@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("zle", "HASH")).thenReturn(false);

        assertThat(candidateService.authenticate("jan.kowalski@example.com", "zle")).isNull();
    }

    @Test
    @DisplayName("SCRUM-19: Logowanie: nieznany email zwraca null")
    void shouldNotAuthenticateUnknownEmail() {
        when(candidateRepository.findByEmail("brak@example.com")).thenReturn(Optional.empty());

        assertThat(candidateService.authenticate("brak@example.com", "x")).isNull();
    }

    // ---------- pozostale ----------

    @Test
    @DisplayName("SCRUM-19: emailExists: true gdy kandydat istnieje, false gdy nie")
    void shouldCheckEmailExistence() {
        when(candidateRepository.findByEmail("jest@example.com")).thenReturn(Optional.of(new Candidate()));
        when(candidateRepository.findByEmail("nie@example.com")).thenReturn(Optional.empty());

        assertThat(candidateService.emailExists("jest@example.com")).isTrue();
        assertThat(candidateService.emailExists("nie@example.com")).isFalse();
    }

    @Test
    @DisplayName("SCRUM-19: getAllCandidates zwraca liste z repozytorium")
    void shouldReturnAllCandidates() {
        List<Candidate> all = List.of(validCandidate(), validCandidate());
        when(candidateRepository.findAll()).thenReturn(all);

        assertThat(candidateService.getAllCandidates()).hasSize(2);
    }

    // ---------- zarzadzanie rolami ----------

    @Test
    @DisplayName("SCRUM-19: Role: zmiana na 'admin' (male litery) zapisuje role ADMIN")
    void shouldUpdateRoleToUpperCase() {
        Candidate c = validCandidate();
        when(candidateRepository.findById(1)).thenReturn(Optional.of(c));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidate result = candidateService.updateRole(1, "admin");

        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("SCRUM-19: Role: pusta rola (null) jest odrzucana")
    void shouldRejectNullRole() {
        assertThatThrownBy(() -> candidateService.updateRole(1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Role: rola z samych spacji jest odrzucana")
    void shouldRejectBlankRole() {
        assertThatThrownBy(() -> candidateService.updateRole(1, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SCRUM-19: Role: niedozwolona rola jest odrzucana")
    void shouldRejectUnknownRole() {
        assertThatThrownBy(() -> candidateService.updateRole(1, "SUPERUSER"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    @DisplayName("SCRUM-19: Role: nieistniejacy kandydat -> IllegalArgumentException")
    void shouldRejectRoleChangeForMissingCandidate() {
        when(candidateRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateService.updateRole(99, "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
