package com.talentcircle.adapter.out.twitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentcircle.common.exception.ChannelPublicationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Tests de propiedad para {@link TwitterClientAdapter} usando jqwik.
 *
 * <p>Valida propiedades universales del adaptador ante contenido de longitud variable
 * y códigos de estado HTTP de error, complementando los tests unitarios de ejemplo.
 */
class TwitterClientAdapterPropertyTest {

    private static final String VALID_TOKEN = "valid-bearer-token";
    private static final String TWEETS_URL  = "https://api.twitter.com/2/tweets";
    private static final int    MAX_LENGTH  = 280;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Property 10: Twitter trunca contenido que supera 280 caracteres
    // Validates: Requisito 4.3
    // -------------------------------------------------------------------------

    /**
     * Property 10: Twitter trunca contenido que supera 280 caracteres.
     *
     * <p>Para cualquier string de contenido con longitud mayor a 280 caracteres,
     * {@link TwitterClientAdapter} debe enviar a la API un texto de exactamente
     * 280 caracteres que termina en {@code "..."}.
     *
     * <p>// Feature: draft-publication, Property 10: Twitter trunca contenido que supera 280 caracteres
     *
     * <p><b>Validates: Requisito 4.3</b>
     */
    @Property
    void publish_contentOver280_isTruncatedTo280(
            @ForAll @StringLength(min = 281, max = 10_000) String content) throws Exception {

        // Arrange
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        TwitterClientAdapter adapter = new TwitterClientAdapter(VALID_TOKEN);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        AtomicReference<String> capturedBody = new AtomicReference<>();

        server.expect(requestTo(TWEETS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> capturedBody.set(
                        new String(((MockClientHttpRequest) request).getBodyAsBytes())))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"data\":{\"id\":\"999\",\"text\":\"truncated\"}}"));

        // Act
        adapter.publish(content);

        // Assert — parse the captured JSON body and inspect the "text" field
        @SuppressWarnings("unchecked")
        Map<String, String> sentBody = objectMapper.readValue(capturedBody.get(), Map.class);
        String sentText = sentBody.get("text");

        assertEquals(MAX_LENGTH, sentText.length(),
                "El texto enviado debe tener exactamente 280 caracteres para contenido > 280");
        assertTrue(sentText.endsWith("..."),
                "El texto enviado debe terminar en '...' cuando se trunca");

        server.verify();
    }

    // -------------------------------------------------------------------------
    // Property 9 (Twitter): Los adaptadores lanzan excepción ante errores HTTP 4xx/5xx
    // Validates: Requisito 4.4
    // -------------------------------------------------------------------------

    /**
     * Property 9 (Twitter): Los adaptadores lanzan excepción ante errores HTTP 4xx/5xx.
     *
     * <p>Para cualquier código de estado HTTP en el rango [400, 599], el adaptador
     * debe lanzar {@link ChannelPublicationException} con el mismo código de estado.
     *
     * <p>// Feature: draft-publication, Property 9: Los adaptadores lanzan excepción ante errores HTTP 4xx/5xx
     *
     * <p><b>Validates: Requisito 4.4</b>
     */
    @Property
    void publish_throwsChannelPublicationException_onHttpError(
            @ForAll @IntRange(min = 400, max = 599) int statusCode) {

        // Arrange
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        TwitterClientAdapter adapter = new TwitterClientAdapter(VALID_TOKEN);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        HttpStatus httpStatus = HttpStatus.resolve(statusCode);
        // Skip status codes not recognized by Spring's HttpStatus enum
        Assume.that(httpStatus != null);

        server.expect(requestTo(TWEETS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(httpStatus)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"title\":\"Error\",\"detail\":\"simulated error\"}"));

        // Act & Assert
        ChannelPublicationException ex = assertThrows(
                ChannelPublicationException.class,
                () -> adapter.publish("Contenido de prueba para Twitter"),
                "Debe lanzar ChannelPublicationException para HTTP " + statusCode
        );

        assertEquals(statusCode, ex.getHttpStatus(),
                "El httpStatus de la excepción debe coincidir con el código HTTP recibido");
        server.verify();
    }
}
