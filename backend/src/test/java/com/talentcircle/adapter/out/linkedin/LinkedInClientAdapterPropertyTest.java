package com.talentcircle.adapter.out.linkedin;

import com.talentcircle.common.exception.ChannelPublicationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Tests de propiedad para {@link LinkedInClientAdapter} usando jqwik.
 *
 * <p>Valida propiedades universales del adaptador ante distintos IDs de post
 * y códigos de estado HTTP de error, complementando los tests unitarios de ejemplo.
 */
class LinkedInClientAdapterPropertyTest {

    private static final String VALID_TOKEN  = "valid-access-token";
    private static final String VALID_PERSON = "person-123";
    private static final String UGCPOSTS_URL = "https://api.linkedin.com/v2/ugcPosts";

    // -------------------------------------------------------------------------
    // Property 8: LinkedIn retorna el ID externo del post creado
    // Validates: Requisito 3.2
    // -------------------------------------------------------------------------

    /**
     * Property 8: LinkedIn retorna el ID externo del post creado.
     *
     * <p>Para cualquier ID de post arbitrario que la API de LinkedIn devuelva en el
     * cuerpo de la respuesta 201, el adaptador debe retornar exactamente ese mismo ID.
     *
     * <p>// Feature: draft-publication, Property 8: LinkedIn retorna el ID externo del post creado
     *
     * <p><b>Validates: Requisito 3.2</b>
     */
    @Property
    void publish_returnsExternalPostId(
            @ForAll @StringLength(min = 1, max = 50) @AlphaChars String postId) {

        // Arrange
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        LinkedInClientAdapter adapter = new LinkedInClientAdapter(VALID_TOKEN, VALID_PERSON);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        String responseBody = "{\"id\": \"" + postId + "\"}";

        server.expect(requestTo(UGCPOSTS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));

        // Act
        String result = adapter.publish("Contenido de prueba para LinkedIn");

        // Assert
        assertEquals(postId, result,
                "El adaptador debe retornar exactamente el ID externo devuelto por la API");
        server.verify();
    }

    // -------------------------------------------------------------------------
    // Property 9 (LinkedIn): Los adaptadores lanzan excepción ante errores HTTP 4xx/5xx
    // Validates: Requisito 3.3
    // -------------------------------------------------------------------------

    /**
     * Property 9 (LinkedIn): Los adaptadores lanzan excepción ante errores HTTP 4xx/5xx.
     *
     * <p>Para cualquier código de estado HTTP en el rango [400, 599], el adaptador
     * debe lanzar {@link ChannelPublicationException} con el mismo código de estado.
     *
     * <p>// Feature: draft-publication, Property 9: Los adaptadores lanzan excepción ante errores HTTP 4xx/5xx
     *
     * <p><b>Validates: Requisito 3.3</b>
     */
    @Property
    void publish_throwsChannelPublicationException_onHttpError(
            @ForAll @IntRange(min = 400, max = 599) int statusCode) {

        // Arrange
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        LinkedInClientAdapter adapter = new LinkedInClientAdapter(VALID_TOKEN, VALID_PERSON);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        HttpStatus httpStatus = HttpStatus.resolve(statusCode);
        // Skip status codes not recognized by Spring's HttpStatus enum
        Assume.that(httpStatus != null);

        server.expect(requestTo(UGCPOSTS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(httpStatus)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"simulated error\"}"));

        // Act & Assert
        ChannelPublicationException ex = assertThrows(
                ChannelPublicationException.class,
                () -> adapter.publish("Contenido de prueba"),
                "Debe lanzar ChannelPublicationException para HTTP " + statusCode
        );

        assertEquals(statusCode, ex.getHttpStatus(),
                "El httpStatus de la excepción debe coincidir con el código HTTP recibido");
        server.verify();
    }
}
