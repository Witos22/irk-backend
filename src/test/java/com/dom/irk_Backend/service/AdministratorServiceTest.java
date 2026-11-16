package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Administrator;
import com.dom.irk_Backend.repository.AdministratorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe logowania administratora.
 * SCRUM-19 - uzupelnienie pokrycia kodu kluczowych procesow biznesowych.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdministratorServiceTest {

    @Mock
    private AdministratorRepository administratorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdministratorService administratorService;

    private Administrator admin() {
        Administrator a = new Administrator();
        a.setEmail("admin@admin.com");
        a.setPasswordHash("HASH");
        return a;
    }

    @Test
    @DisplayName("SCRUM-19: Logowanie admina: poprawne haslo zwraca administratora")
    void shouldAuthenticateAdmin() {
        Administrator a = admin();
        when(administratorRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(a));
        when(passwordEncoder.matches("admin123", "HASH")).thenReturn(true);

        assertThat(administratorService.authenticate("admin@admin.com", "admin123")).isSameAs(a);
    }

    @Test
    @DisplayName("SCRUM-19: Logowanie admina: bledne haslo zwraca null")
    void shouldRejectWrongAdminPassword() {
        when(administratorRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin()));
        when(passwordEncoder.matches("zle", "HASH")).thenReturn(false);

        assertThat(administratorService.authenticate("admin@admin.com", "zle")).isNull();
    }

    @Test
    @DisplayName("SCRUM-19: Logowanie admina: nieznany email zwraca null")
    void shouldRejectUnknownAdminEmail() {
        when(administratorRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());

        assertThat(administratorService.authenticate("x@x.com", "admin123")).isNull();
    }
}
