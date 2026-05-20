package com.talentcircle.application.service;

import com.talentcircle.adapter.out.linkedin.LinkedInClientAdapter;
import com.talentcircle.adapter.out.newsletter.NewsletterPublisherAdapter;
import com.talentcircle.adapter.out.twitter.TwitterClientAdapter;
import com.talentcircle.common.exception.ChannelPublicationException;
import com.talentcircle.common.exception.ConflictException;
import com.talentcircle.common.exception.ResourceNotFoundException;
import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.in.PublicationUseCase;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.PublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link PublicationService}.
 *
 * Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    @Mock
    private DraftRepository draftRepository;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private LinkedInClientAdapter linkedInAdapter;

    @Mock
    private TwitterClientAdapter twitterAdapter;

    @Mock
    private NewsletterPublisherAdapter newsletterAdapter;

    private PublicationService publicationService;

    @BeforeEach
    void setUp() {
        // Construct manually because @InjectMocks cannot handle the Map.of(...) constructor
        publicationService = new PublicationService(
                draftRepository,
                publicationRepository,
                linkedInAdapter,
                twitterAdapter,
                newsletterAdapter
        );
    }

    // -------------------------------------------------------------------------
    // 2.4 — Draft not found → ResourceNotFoundException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("publishDraft: borrador no encontrado → ResourceNotFoundException")
    void publishDraft_draftNotFound_throwsResourceNotFoundException() {
        when(draftRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicationService.publishDraft("missing-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing-id");

        verify(publicationRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 2.5 — Draft with status PENDING → ConflictException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("publishDraft: borrador con estado PENDING → ConflictException")
    void publishDraft_draftStatusPending_throwsConflictException() {
        Draft draft = draftWithStatus(Draft.DraftStatus.PENDING);
        when(draftRepository.findById("draft-1")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> publicationService.publishDraft("draft-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("APPROVED");

        verify(publicationRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 2.5 — Draft with status PUBLISHED → ConflictException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("publishDraft: borrador con estado PUBLISHED → ConflictException")
    void publishDraft_draftStatusPublished_throwsConflictException() {
        Draft draft = draftWithStatus(Draft.DraftStatus.PUBLISHED);
        when(draftRepository.findById("draft-1")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> publicationService.publishDraft("draft-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("APPROVED");

        verify(publicationRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 2.2 — Successful publication → Draft.status = PUBLISHED, Publication.status = SUCCESS
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("publishDraft: publicación exitosa → Draft.status=PUBLISHED, Publication.status=SUCCESS")
    void publishDraft_success_updatesDraftAndPersistsSuccessPublication() {
        Draft draft = approvedLinkedInDraft("draft-ok", "Hello LinkedIn");
        when(draftRepository.findById("draft-ok")).thenReturn(Optional.of(draft));
        when(linkedInAdapter.publish("Hello LinkedIn")).thenReturn("urn:li:share:123");
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Publication> pubCaptor = ArgumentCaptor.forClass(Publication.class);
        ArgumentCaptor<Draft> draftCaptor = ArgumentCaptor.forClass(Draft.class);

        PublicationUseCase.PublicationDto result = publicationService.publishDraft("draft-ok");

        // Verify returned DTO
        assertThat(result.status()).isEqualTo(Publication.PublicationStatus.SUCCESS.name());
        assertThat(result.externalPostId()).isEqualTo("urn:li:share:123");
        assertThat(result.publishedAt()).isNotNull();
        assertThat(result.errorMessage()).isNull();

        // Verify Draft was saved with PUBLISHED status
        verify(draftRepository).save(draftCaptor.capture());
        assertThat(draftCaptor.getValue().getStatus()).isEqualTo(Draft.DraftStatus.PUBLISHED);

        // Verify Publication was saved with SUCCESS status and externalPostId
        verify(publicationRepository).save(pubCaptor.capture());
        Publication savedPub = pubCaptor.getValue();
        assertThat(savedPub.getStatus()).isEqualTo(Publication.PublicationStatus.SUCCESS);
        assertThat(savedPub.getExternalPostId()).isEqualTo("urn:li:share:123");
        assertThat(savedPub.getPublishedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // 2.3 — Adapter failure → Draft.status unchanged, Publication.status = FAILED
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("publishDraft: fallo en adaptador → Draft.status sin cambio, Publication.status=FAILED")
    void publishDraft_adapterThrows_draftStatusUnchangedAndPublicationFailed() {
        Draft draft = approvedLinkedInDraft("draft-fail", "Content");
        when(draftRepository.findById("draft-fail")).thenReturn(Optional.of(draft));
        when(linkedInAdapter.publish("Content"))
                .thenThrow(new ChannelPublicationException("LinkedIn 500", 500));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Publication> pubCaptor = ArgumentCaptor.forClass(Publication.class);

        PublicationUseCase.PublicationDto result = publicationService.publishDraft("draft-fail");

        // Draft status must remain APPROVED
        assertThat(draft.getStatus()).isEqualTo(Draft.DraftStatus.APPROVED);
        verify(draftRepository, never()).save(any(Draft.class));

        // Publication must be FAILED with errorMessage
        verify(publicationRepository).save(pubCaptor.capture());
        Publication savedPub = pubCaptor.getValue();
        assertThat(savedPub.getStatus()).isEqualTo(Publication.PublicationStatus.FAILED);
        assertThat(savedPub.getErrorMessage()).contains("LinkedIn 500");

        // DTO reflects failure
        assertThat(result.status()).isEqualTo(Publication.PublicationStatus.FAILED.name());
        assertThat(result.errorMessage()).contains("LinkedIn 500");
    }

    // -------------------------------------------------------------------------
    // 2.6 — publicationRepository.save() called in both success and failure paths
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("publishDraft: publicationRepository.save() se llama en éxito")
    void publishDraft_success_publicationRepositorySaveIsCalled() {
        Draft draft = approvedLinkedInDraft("draft-s", "Content");
        when(draftRepository.findById("draft-s")).thenReturn(Optional.of(draft));
        when(linkedInAdapter.publish(any())).thenReturn("urn:li:share:999");
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        publicationService.publishDraft("draft-s");

        verify(publicationRepository, times(1)).save(any(Publication.class));
    }

    @Test
    @DisplayName("publishDraft: publicationRepository.save() se llama en fallo")
    void publishDraft_failure_publicationRepositorySaveIsCalled() {
        Draft draft = approvedLinkedInDraft("draft-f", "Content");
        when(draftRepository.findById("draft-f")).thenReturn(Optional.of(draft));
        when(linkedInAdapter.publish(any()))
                .thenThrow(new ChannelPublicationException("error", 503));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        publicationService.publishDraft("draft-f");

        verify(publicationRepository, times(1)).save(any(Publication.class));
    }

    // -------------------------------------------------------------------------
    // 2.1 — Routing: editedContent takes precedence over content when non-blank
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("publishDraft: usa editedContent cuando no es nulo/vacío")
    void publishDraft_usesEditedContentWhenPresent() {
        Draft draft = approvedLinkedInDraft("draft-edit", "Original content");
        draft.setEditedContent("Edited content");
        when(draftRepository.findById("draft-edit")).thenReturn(Optional.of(draft));
        when(linkedInAdapter.publish("Edited content")).thenReturn("urn:li:share:edited");
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicationUseCase.PublicationDto result = publicationService.publishDraft("draft-edit");

        verify(linkedInAdapter).publish("Edited content");
        assertThat(result.status()).isEqualTo(Publication.PublicationStatus.SUCCESS.name());
    }

    @Test
    @DisplayName("publishDraft: usa content cuando editedContent es nulo")
    void publishDraft_usesContentWhenEditedContentIsNull() {
        Draft draft = approvedLinkedInDraft("draft-orig", "Original content");
        draft.setEditedContent(null);
        when(draftRepository.findById("draft-orig")).thenReturn(Optional.of(draft));
        when(linkedInAdapter.publish("Original content")).thenReturn("urn:li:share:orig");
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        publicationService.publishDraft("draft-orig");

        verify(linkedInAdapter).publish("Original content");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Draft draftWithStatus(Draft.DraftStatus status) {
        Draft draft = new Draft();
        draft.setId("draft-1");
        draft.setStatus(status);
        draft.setChannel(Draft.Channel.LINKEDIN);
        draft.setContent("Some content");
        return draft;
    }

    private Draft approvedLinkedInDraft(String id, String content) {
        Draft draft = new Draft();
        draft.setId(id);
        draft.setStatus(Draft.DraftStatus.APPROVED);
        draft.setChannel(Draft.Channel.LINKEDIN);
        draft.setContent(content);
        return draft;
    }
}
