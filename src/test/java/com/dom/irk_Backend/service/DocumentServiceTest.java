package com.dom.irk_Backend.service;

import com.dom.irk_Backend.model.Document;
import com.dom.irk_Backend.repository.DocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe zmiany statusu dokumentu.
 * SCRUM-19 - uzupelnienie pokrycia kodu kluczowych procesow biznesowych.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    @DisplayName("SCRUM-19: Dokument: zmiana statusu zapisuje nowy status")
    void shouldUpdateDocumentStatus() {
        Document doc = new Document();
        doc.setStatus("Niedostarczone");
        when(documentRepository.findById(5)).thenReturn(Optional.of(doc));

        documentService.updateDocumentStatus(5, "Dostarczone");

        assertThat(doc.getStatus()).isEqualTo("Dostarczone");
        verify(documentRepository).save(doc);
    }

    @Test
    @DisplayName("SCRUM-19: Dokument: nieistniejacy dokument -> wyjatek, brak zapisu")
    void shouldFailForMissingDocument() {
        when(documentRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.updateDocumentStatus(404, "Dostarczone"))
                .isInstanceOf(RuntimeException.class);
        verify(documentRepository, never()).save(any(Document.class));
    }
}
