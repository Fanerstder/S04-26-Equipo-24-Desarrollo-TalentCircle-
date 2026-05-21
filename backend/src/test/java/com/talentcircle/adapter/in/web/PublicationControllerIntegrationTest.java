package com.talentcircle.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentcircle.common.exception.ConflictException;
import com.talentcircle.common.exception.GlobalExceptionHandler;
import com.talentcircle.common.exception.ResourceNotFoundException;
import com.talentcircle.common.security.JwtAuthFilter;
import com.talentcircle.common.security.JwtService;
import com.talentcircle.config.SecurityConfig;
import com.talentcircle.domain.port.in.DraftReviewUseCase;
import com.talentcircle.domain.port.in.PublicationUseCase;
import com.talentcircle.domain.port.in.PublicationUseCase.PublicationDto;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para el endpoint POST /api/v1/drafts/{id}/publish.
 *
 * <p>Usa {@link WebMvcTest} con {@link MockBean} para {@link PublicationUseCase}
 * y {@link DraftReviewUseCase}, evitando la instanciación de los adaptadores de canal
 * (LinkedInClientAdapter, TwitterClientAdapter, NewsletterPublisherAdapter) que
 * validan tokens en su constructor.
 *
 * <p>Incluye Property 13: Usuarios sin rol EDITOR/ADMIN reciben HTTP 403.
 */
@WebMvcTest(controllers = DraftController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class, GlobalExceptionHandler.class})
@MockBean(JpaMetamodelMappingContext.class)
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-minimum-32-characters-long",
        "app.jwt.access-token-expiration=28800000",
        "app.jwt.refresh-token-expiration=604800000",
        "app.cors.allowed-origins=http://localhost:5173"
})
class PublicationControllerIntegrationTest {

    private static final String PUBLISH_URL = "/api/v1/drafts/{id}/publish";
    private static final String TEST_SECRET = "test-secret-key-minimum-32-characters-long";
    private static final String DRAFT_ID    = "draft-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PublicationUseCase publicationUseCase;

    @MockBean
    private DraftReviewUseCase draftReviewUseCase;

    /** JwtService configurado con el mismo secreto que el contexto de test. */
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 28800000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L);
    }

    // -------------------------------------------------------------------------
    // HTTP 401 — sin token JWT
    // -------------------------------------------------------------------------

    @Test
    void publishDraft_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(PUBLISH_URL, DRAFT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // HTTP 403 — rol incorrecto (VIEWER)
    // -------------------------------------------------------------------------

    @Test
    void publishDraft_withViewerRole_returns403() throws Exception {
        String token = jwtService.generateAccessToken("user-viewer", "VIEWER");

        mockMvc.perform(post(PUBLISH_URL, DRAFT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // HTTP 404 — borrador inexistente
    // -------------------------------------------------------------------------

    @Test
    void publishDraft_withNonExistentDraft_returns404() throws Exception {
        String token = jwtService.generateAccessToken("user-editor", "EDITOR");

        when(publicationUseCase.publishDraft("nonexistent-id"))
                .thenThrow(new ResourceNotFoundException("Draft not found with id: 'nonexistent-id'"));

        mockMvc.perform(post(PUBLISH_URL, "nonexistent-id")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // HTTP 409 — borrador en estado no-APPROVED
    // -------------------------------------------------------------------------

    @Test
    void publishDraft_withNonApprovedDraft_returns409() throws Exception {
        String token = jwtService.generateAccessToken("user-editor", "EDITOR");

        when(publicationUseCase.publishDraft(DRAFT_ID))
                .thenThrow(new ConflictException("Solo borradores APPROVED pueden publicarse"));

        mockMvc.perform(post(PUBLISH_URL, DRAFT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // HTTP 200 — publicación exitosa → PublicationDto status SUCCESS
    // -------------------------------------------------------------------------

    @Test
    void publishDraft_withEditorRole_successfulPublication_returns200WithSuccess() throws Exception {
        String token = jwtService.generateAccessToken("user-editor", "EDITOR");

        PublicationDto dto = new PublicationDto(
                "pub-001",
                DRAFT_ID,
                "SUCCESS",
                "urn:li:share:123456",
                "2026-05-02T18:15:00",
                null
        );
        when(publicationUseCase.publishDraft(DRAFT_ID)).thenReturn(dto);

        mockMvc.perform(post(PUBLISH_URL, DRAFT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.draftId").value(DRAFT_ID))
                .andExpect(jsonPath("$.externalPostId").value("urn:li:share:123456"));
    }

    @Test
    void publishDraft_withAdminRole_successfulPublication_returns200WithSuccess() throws Exception {
        String token = jwtService.generateAccessToken("user-admin", "ADMIN");

        PublicationDto dto = new PublicationDto(
                "pub-002",
                DRAFT_ID,
                "SUCCESS",
                "urn:li:share:789012",
                "2026-05-02T19:00:00",
                null
        );
        when(publicationUseCase.publishDraft(DRAFT_ID)).thenReturn(dto);

        mockMvc.perform(post(PUBLISH_URL, DRAFT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.draftId").value(DRAFT_ID));
    }

    // -------------------------------------------------------------------------
    // HTTP 200 — fallo en adaptador → PublicationDto status FAILED
    // -------------------------------------------------------------------------

    @Test
    void publishDraft_whenAdapterThrowsException_returns200WithFailed() throws Exception {
        String token = jwtService.generateAccessToken("user-editor", "EDITOR");

        PublicationDto dto = new PublicationDto(
                "pub-003",
                DRAFT_ID,
                "FAILED",
                null,
                null,
                "LinkedIn API error: 503 Service Unavailable"
        );
        when(publicationUseCase.publishDraft(DRAFT_ID)).thenReturn(dto);

        mockMvc.perform(post(PUBLISH_URL, DRAFT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.draftId").value(DRAFT_ID))
                .andExpect(jsonPath("$.errorMessage").value("LinkedIn API error: 503 Service Unavailable"));
    }

    // -------------------------------------------------------------------------
    // Property 13: Usuarios sin rol EDITOR/ADMIN reciben HTTP 403
    // Feature: draft-publication, Property 13: Usuarios sin rol EDITOR/ADMIN reciben HTTP 403
    // Validates: Requirement 7.2
    // -------------------------------------------------------------------------

    /**
     * Property 13: Usuarios sin rol EDITOR/ADMIN reciben HTTP 403.
     *
     * <p>Para cualquier rol que no sea EDITOR ni ADMIN, el endpoint de publicación
     * debe retornar HTTP 403 Forbidden.
     *
     * <p>// Feature: draft-publication, Property 13: Usuarios sin rol EDITOR/ADMIN reciben HTTP 403
     *
     * <p><b>Validates: Requirement 7.2</b>
     *
     * <p>Usa el {@link MockMvc} inyectado por Spring (con la configuración de seguridad
     * completa incluyendo {@code @PreAuthorize}) para validar la propiedad.
     * jqwik ejecuta este test en la misma instancia que JUnit 5, por lo que los
     * campos {@code @Autowired} están disponibles.
     */
    @Property(tries = 20)
    void property13_nonEditorNonAdminRole_returns403(
            @ForAll("nonPrivilegedRoles") String role) throws Exception {

        // Arrange: generar token con el rol no privilegiado
        JwtService localJwtService = new JwtService();
        ReflectionTestUtils.setField(localJwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(localJwtService, "accessTokenExpiration", 28800000L);
        ReflectionTestUtils.setField(localJwtService, "refreshTokenExpiration", 604800000L);

        String token = localJwtService.generateAccessToken("user-test", role);

        // Act & Assert: el endpoint debe rechazar con 403 cualquier rol que no sea EDITOR/ADMIN
        mockMvc.perform(post(PUBLISH_URL, DRAFT_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    /**
     * Genera roles que NO son EDITOR ni ADMIN.
     * Incluye roles comunes del sistema y roles arbitrarios de letras mayúsculas.
     */
    @Provide
    Arbitrary<String> nonPrivilegedRoles() {
        Arbitrary<String> fixedRoles = Arbitraries.of(
                "VIEWER", "USER", "GUEST", "MODERATOR", "READER",
                "SUBSCRIBER", "MEMBER", "OPERATOR", "SUPPORT", "ANALYST"
        );
        Arbitrary<String> randomRoles = Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(3)
                .ofMaxLength(15)
                .filter(r -> !r.equals("EDITOR") && !r.equals("ADMIN"));

        return Arbitraries.oneOf(fixedRoles, randomRoles);
    }
}
