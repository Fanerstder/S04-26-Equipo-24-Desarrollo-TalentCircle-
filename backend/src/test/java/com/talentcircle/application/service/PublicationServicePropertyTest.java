package com.talentcircle.application.service;

import com.talentcircle.adapter.out.linkedin.LinkedInClientAdapter;
import com.talentcircle.adapter.out.newsletter.NewsletterPublisherAdapter;
import com.talentcircle.adapter.out.twitter.TwitterClientAdapter;
import com.talentcircle.common.exception.ChannelPublicationException;
import com.talentcircle.common.exception.ConflictException;
import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.in.PublicationUseCase;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.PublicationRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-Based Tests for {@link PublicationService}.
 *
 * Feature: draft-publication
 *
 * Properties covered:
 *   Property 3:  El servicio enruta al adaptador correcto según el canal — Validates: Requisito 2.1
 *   Property 4:  Publicación exitosa actualiza estado y persiste registro SUCCESS — Validates: Requisitos 2.2, 5.1, 8.1
 *   Property 5:  Fallo en canal externo preserva estado del borrador y persiste registro FAILED — Validates: Requisitos 2.3, 2.6, 8.1
 *   Property 6:  Publicación siempre persiste un registro Publication — Validates: Requisitos 2.6, 8.1
 *   Property 7:  Rechazo de borradores no-APPROVED con ConflictException — Validates: Requisito 2.5
 *   Property 14: El registro Publication contiene todos los campos requeridos — Validates: Requisito 8.1
 */
class PublicationServicePropertyTest {

    // -------------------------------------------------------------------------
    // Property 3: El servicio enruta al adaptador correcto según el canal
    // Feature: draft-publication, Property 3: El servicio enruta al adaptador correcto según el canal
    // Validates: Requisito 2.1
    // -------------------------------------------------------------------------

    @Property
    void property3_publishDraft_routesToCorrectAdapter(@ForAll Draft.Channel channel) {
        // Arrange — create fresh mocks per iteration (Map.of() constructor requires concrete types)
        DraftRepository draftRepository = mock(DraftRepository.class);
        PublicationRepository publicationRepository = mock(PublicationRepository.class);
        LinkedInClientAdapter linkedInAdapter = mock(LinkedInClientAdapter.class);
        TwitterClientAdapter twitterAdapter = mock(TwitterClientAdapter.class);
        NewsletterPublisherAdapter newsletterAdapter = mock(NewsletterPublisherAdapter.class);

        PublicationService service = new PublicationService(
                draftRepository, publicationRepository,
                linkedInAdapter, twitterAdapter, newsletterAdapter);

        Draft draft = approvedDraft("draft-route", "content", channel);
        when(draftRepository.findById("draft-route")).thenReturn(Optional.of(draft));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        // Stub the correct adapter to return a non-null ID
        switch (channel) {
            case LINKEDIN    -> when(linkedInAdapter.publish(any())).thenReturn("li-id");
            case TWITTER     -> when(twitterAdapter.publish(any())).thenReturn("tw-id");
            case NEWSLETTER  -> when(newsletterAdapter.publish(any())).thenReturn(null);
        }

        // Act
        service.publishDraft("draft-route");

        // Assert — only the adapter matching the channel is invoked
        switch (channel) {
            case LINKEDIN -> {
                verify(linkedInAdapter, times(1)).publish(any());
                verify(twitterAdapter, never()).publish(any());
                verify(newsletterAdapter, never()).publish(any());
            }
            case TWITTER -> {
                verify(twitterAdapter, times(1)).publish(any());
                verify(linkedInAdapter, never()).publish(any());
                verify(newsletterAdapter, never()).publish(any());
            }
            case NEWSLETTER -> {
                verify(newsletterAdapter, times(1)).publish(any());
                verify(linkedInAdapter, never()).publish(any());
                verify(twitterAdapter, never()).publish(any());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Property 4: Publicación exitosa actualiza estado y persiste registro SUCCESS
    // Feature: draft-publication, Property 4: Publicación exitosa actualiza estado y persiste registro SUCCESS
    // Validates: Requisitos 2.2, 5.1, 8.1
    // -------------------------------------------------------------------------

    @Property
    void property4_publishDraft_onSuccess_updatesDraftAndPersistsSuccessPublication(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String content) {

        DraftRepository draftRepository = mock(DraftRepository.class);
        PublicationRepository publicationRepository = mock(PublicationRepository.class);
        LinkedInClientAdapter linkedInAdapter = mock(LinkedInClientAdapter.class);
        TwitterClientAdapter twitterAdapter = mock(TwitterClientAdapter.class);
        NewsletterPublisherAdapter newsletterAdapter = mock(NewsletterPublisherAdapter.class);

        PublicationService service = new PublicationService(
                draftRepository, publicationRepository,
                linkedInAdapter, twitterAdapter, newsletterAdapter);

        Draft draft = approvedDraft("draft-success", content, Draft.Channel.LINKEDIN);
        when(draftRepository.findById("draft-success")).thenReturn(Optional.of(draft));
        when(linkedInAdapter.publish(content)).thenReturn("ext-id-123");
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Publication> pubCaptor = ArgumentCaptor.forClass(Publication.class);
        ArgumentCaptor<Draft> draftCaptor = ArgumentCaptor.forClass(Draft.class);

        // Act
        PublicationUseCase.PublicationDto result = service.publishDraft("draft-success");

        // Assert — Draft status updated to PUBLISHED
        verify(draftRepository).save(draftCaptor.capture());
        assertThat(draftCaptor.getValue().getStatus())
                .isEqualTo(Draft.DraftStatus.PUBLISHED);

        // Assert — Publication persisted with SUCCESS status
        verify(publicationRepository).save(pubCaptor.capture());
        Publication saved = pubCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Publication.PublicationStatus.SUCCESS);
        assertThat(saved.getExternalPostId()).isEqualTo("ext-id-123");
        assertThat(saved.getPublishedAt()).isNotNull();
        assertThat(saved.getErrorMessage()).isNull();

        // Assert — DTO reflects success
        assertThat(result.status()).isEqualTo(Publication.PublicationStatus.SUCCESS.name());
        assertThat(result.externalPostId()).isEqualTo("ext-id-123");
        assertThat(result.publishedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Property 5: Fallo en canal externo preserva estado del borrador y persiste registro FAILED
    // Feature: draft-publication, Property 5: Fallo en canal externo preserva estado del borrador y persiste registro FAILED
    // Validates: Requisitos 2.3, 2.6, 8.1
    // -------------------------------------------------------------------------

    @Property
    void property5_publishDraft_onAdapterFailure_preservesDraftStatusAndPersistsFailedPublication(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String errorMessage) {

        DraftRepository draftRepository = mock(DraftRepository.class);
        PublicationRepository publicationRepository = mock(PublicationRepository.class);
        LinkedInClientAdapter linkedInAdapter = mock(LinkedInClientAdapter.class);
        TwitterClientAdapter twitterAdapter = mock(TwitterClientAdapter.class);
        NewsletterPublisherAdapter newsletterAdapter = mock(NewsletterPublisherAdapter.class);

        PublicationService service = new PublicationService(
                draftRepository, publicationRepository,
                linkedInAdapter, twitterAdapter, newsletterAdapter);

        Draft draft = approvedDraft("draft-fail", "some content", Draft.Channel.LINKEDIN);
        when(draftRepository.findById("draft-fail")).thenReturn(Optional.of(draft));
        when(linkedInAdapter.publish(any()))
                .thenThrow(new ChannelPublicationException(errorMessage, 500));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Publication> pubCaptor = ArgumentCaptor.forClass(Publication.class);

        // Act
        PublicationUseCase.PublicationDto result = service.publishDraft("draft-fail");

        // Assert — Draft status must remain APPROVED (not saved)
        assertThat(draft.getStatus()).isEqualTo(Draft.DraftStatus.APPROVED);
        verify(draftRepository, never()).save(any(Draft.class));

        // Assert — Publication persisted with FAILED status and error message
        verify(publicationRepository).save(pubCaptor.capture());
        Publication saved = pubCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Publication.PublicationStatus.FAILED);
        assertThat(saved.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(saved.getPublishedAt()).isNull();

        // Assert — DTO reflects failure
        assertThat(result.status()).isEqualTo(Publication.PublicationStatus.FAILED.name());
        assertThat(result.errorMessage()).isEqualTo(errorMessage);
    }

    // -------------------------------------------------------------------------
    // Property 6: Publicación siempre persiste un registro Publication
    // Feature: draft-publication, Property 6: Publicación siempre persiste un registro Publication
    // Validates: Requisitos 2.6, 8.1
    // -------------------------------------------------------------------------

    @Property
    void property6_publishDraft_alwaysPersistsOnePublication(
            @ForAll Draft.Channel channel,
            @ForAll boolean adapterSucceeds) {

        DraftRepository draftRepository = mock(DraftRepository.class);
        PublicationRepository publicationRepository = mock(PublicationRepository.class);
        LinkedInClientAdapter linkedInAdapter = mock(LinkedInClientAdapter.class);
        TwitterClientAdapter twitterAdapter = mock(TwitterClientAdapter.class);
        NewsletterPublisherAdapter newsletterAdapter = mock(NewsletterPublisherAdapter.class);

        PublicationService service = new PublicationService(
                draftRepository, publicationRepository,
                linkedInAdapter, twitterAdapter, newsletterAdapter);

        Draft draft = approvedDraft("draft-always", "content", channel);
        when(draftRepository.findById("draft-always")).thenReturn(Optional.of(draft));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        if (adapterSucceeds) {
            switch (channel) {
                case LINKEDIN   -> when(linkedInAdapter.publish(any())).thenReturn("li-id");
                case TWITTER    -> when(twitterAdapter.publish(any())).thenReturn("tw-id");
                case NEWSLETTER -> when(newsletterAdapter.publish(any())).thenReturn(null);
            }
        } else {
            switch (channel) {
                case LINKEDIN   -> when(linkedInAdapter.publish(any()))
                        .thenThrow(new ChannelPublicationException("error", 500));
                case TWITTER    -> when(twitterAdapter.publish(any()))
                        .thenThrow(new ChannelPublicationException("error", 500));
                case NEWSLETTER -> when(newsletterAdapter.publish(any()))
                        .thenThrow(new ChannelPublicationException("error", 500));
            }
        }

        // Act
        service.publishDraft("draft-always");

        // Assert — publicationRepository.save() called exactly once regardless of outcome
        ArgumentCaptor<Publication> pubCaptor = ArgumentCaptor.forClass(Publication.class);
        verify(publicationRepository, times(1)).save(pubCaptor.capture());

        Publication saved = pubCaptor.getValue();
        // The saved Publication must reference the correct draft and channel
        assertThat(saved.getDraft()).isEqualTo(draft);
        assertThat(saved.getChannel().name()).isEqualTo(channel.name());
    }

    // -------------------------------------------------------------------------
    // Property 7: Rechazo de borradores no-APPROVED con ConflictException
    // Feature: draft-publication, Property 7: Rechazo de borradores no-APPROVED con ConflictException
    // Validates: Requisito 2.5
    // -------------------------------------------------------------------------

    @Property
    void property7_publishDraft_nonApprovedStatus_throwsConflictException(
            @ForAll("nonApprovedStatuses") Draft.DraftStatus status) {

        DraftRepository draftRepository = mock(DraftRepository.class);
        PublicationRepository publicationRepository = mock(PublicationRepository.class);
        LinkedInClientAdapter linkedInAdapter = mock(LinkedInClientAdapter.class);
        TwitterClientAdapter twitterAdapter = mock(TwitterClientAdapter.class);
        NewsletterPublisherAdapter newsletterAdapter = mock(NewsletterPublisherAdapter.class);

        PublicationService service = new PublicationService(
                draftRepository, publicationRepository,
                linkedInAdapter, twitterAdapter, newsletterAdapter);

        Draft draft = new Draft();
        draft.setId("draft-non-approved");
        draft.setStatus(status);
        draft.setChannel(Draft.Channel.LINKEDIN);
        draft.setContent("content");
        when(draftRepository.findById("draft-non-approved")).thenReturn(Optional.of(draft));

        // Act + Assert — ConflictException must be thrown
        assertThatThrownBy(() -> service.publishDraft("draft-non-approved"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("APPROVED");

        // publicationRepository.save() must never be called
        verify(publicationRepository, never()).save(any());
    }

    @Provide
    Arbitrary<Draft.DraftStatus> nonApprovedStatuses() {
        return Arbitraries.of(
                Draft.DraftStatus.PENDING,
                Draft.DraftStatus.REJECTED,
                Draft.DraftStatus.PUBLISHED
        );
    }

    // -------------------------------------------------------------------------
    // Property 14: El registro Publication contiene todos los campos requeridos
    // Feature: draft-publication, Property 14: El registro Publication contiene todos los campos requeridos
    // Validates: Requisito 8.1
    // -------------------------------------------------------------------------

    @Property
    void property14_publishDraft_success_publicationHasAllRequiredFields(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String content) {

        DraftRepository draftRepository = mock(DraftRepository.class);
        PublicationRepository publicationRepository = mock(PublicationRepository.class);
        LinkedInClientAdapter linkedInAdapter = mock(LinkedInClientAdapter.class);
        TwitterClientAdapter twitterAdapter = mock(TwitterClientAdapter.class);
        NewsletterPublisherAdapter newsletterAdapter = mock(NewsletterPublisherAdapter.class);

        PublicationService service = new PublicationService(
                draftRepository, publicationRepository,
                linkedInAdapter, twitterAdapter, newsletterAdapter);

        Draft draft = approvedDraft("draft-fields", content, Draft.Channel.TWITTER);
        when(draftRepository.findById("draft-fields")).thenReturn(Optional.of(draft));
        when(twitterAdapter.publish(content)).thenReturn("tw-ext-id");
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Publication> pubCaptor = ArgumentCaptor.forClass(Publication.class);

        // Act
        service.publishDraft("draft-fields");

        // Assert — all required fields present on success
        verify(publicationRepository).save(pubCaptor.capture());
        Publication saved = pubCaptor.getValue();

        assertThat(saved.getDraft()).isNotNull();                                          // draftId (via draft reference)
        assertThat(saved.getDraft().getId()).isEqualTo("draft-fields");                   // correct draftId
        assertThat(saved.getChannel()).isEqualTo(Publication.Channel.TWITTER);            // correct channel
        assertThat(saved.getStatus()).isEqualTo(Publication.PublicationStatus.SUCCESS);   // status = SUCCESS
        assertThat(saved.getPublishedAt()).isNotNull();                                   // publishedAt set
        assertThat(saved.getExternalPostId()).isEqualTo("tw-ext-id");                    // externalPostId set
        assertThat(saved.getErrorMessage()).isNull();                                     // no error on success
    }

    @Property
    void property14_publishDraft_failure_publicationHasAllRequiredFields(
            @ForAll @StringLength(min = 1, max = 100) @AlphaChars String content) {

        DraftRepository draftRepository = mock(DraftRepository.class);
        PublicationRepository publicationRepository = mock(PublicationRepository.class);
        LinkedInClientAdapter linkedInAdapter = mock(LinkedInClientAdapter.class);
        TwitterClientAdapter twitterAdapter = mock(TwitterClientAdapter.class);
        NewsletterPublisherAdapter newsletterAdapter = mock(NewsletterPublisherAdapter.class);

        PublicationService service = new PublicationService(
                draftRepository, publicationRepository,
                linkedInAdapter, twitterAdapter, newsletterAdapter);

        Draft draft = approvedDraft("draft-fields-fail", content, Draft.Channel.TWITTER);
        when(draftRepository.findById("draft-fields-fail")).thenReturn(Optional.of(draft));
        when(twitterAdapter.publish(content))
                .thenThrow(new ChannelPublicationException("Twitter error", 503));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Publication> pubCaptor = ArgumentCaptor.forClass(Publication.class);

        // Act
        service.publishDraft("draft-fields-fail");

        // Assert — all required fields present on failure
        verify(publicationRepository).save(pubCaptor.capture());
        Publication saved = pubCaptor.getValue();

        assertThat(saved.getDraft()).isNotNull();                                          // draftId (via draft reference)
        assertThat(saved.getDraft().getId()).isEqualTo("draft-fields-fail");              // correct draftId
        assertThat(saved.getChannel()).isEqualTo(Publication.Channel.TWITTER);            // correct channel
        assertThat(saved.getStatus()).isEqualTo(Publication.PublicationStatus.FAILED);    // status = FAILED
        assertThat(saved.getErrorMessage()).isEqualTo("Twitter error");                   // errorMessage set
        assertThat(saved.getPublishedAt()).isNull();                                      // no publishedAt on failure
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Draft approvedDraft(String id, String content, Draft.Channel channel) {
        Draft draft = new Draft();
        draft.setId(id);
        draft.setStatus(Draft.DraftStatus.APPROVED);
        draft.setChannel(channel);
        draft.setContent(content);
        return draft;
    }
}
