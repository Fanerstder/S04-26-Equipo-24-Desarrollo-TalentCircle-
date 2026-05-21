package com.talentcircle.adapter.out.linkedin;

import com.talentcircle.common.exception.ChannelPublicationException;
import org.junit.jupiter.api.Test;
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
 * Tests unitarios para {@link LinkedInClientAdapter}.
 *
 * <p>Dado que {@link RestTemplate} se crea internamente en el constructor,
 * se usa {@link ReflectionTestUtils} para inyectar un {@link RestTemplate}
 * controlado por {@link MockRestServiceServer}, que es el enfoque idiomático
 * de Spring para interceptar llamadas HTTP en tests unitarios.
 */
class LinkedInClientAdapterTest {

    private static final String VALID_TOKEN  = "valid-access-token";
    private static final String VALID_PERSON = "person-123";
    private static final String UGCPOSTS_URL = "https://api.linkedin.com/v2/ugcPosts";

    // -------------------------------------------------------------------------
    // Construcción — validación de tokens
    // -------------------------------------------------------------------------

    @Test
    void constructor_emptyAccessToken_throwsIllegalStateException() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new LinkedInClientAdapter("", VALID_PERSON)
        );
        assertTrue(ex.getMessage().contains("LINKEDIN_ACCESS_TOKEN"),
                "El mensaje debe mencionar LINKEDIN_ACCESS_TOKEN");
    }

    @Test
    void constructor_blankAccessToken_throwsIllegalStateException() {
        assertThrows(
                IllegalStateException.class,
                () -> new LinkedInClientAdapter("   ", VALID_PERSON)
        );
    }

    @Test
    void constructor_nullAccessToken_throwsIllegalStateException() {
        assertThrows(
                IllegalStateException.class,
                () -> new LinkedInClientAdapter(null, VALID_PERSON)
        );
    }

    @Test
    void constructor_emptyPersonId_throwsIllegalStateException() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new LinkedInClientAdapter(VALID_TOKEN, "")
        );
        assertTrue(ex.getMessage().contains("LINKEDIN_PERSON_ID"),
                "El mensaje debe mencionar LINKEDIN_PERSON_ID");
    }

    @Test
    void constructor_validTokens_doesNotThrow() {
        assertDoesNotThrow(() -> new LinkedInClientAdapter(VALID_TOKEN, VALID_PERSON));
    }

    // -------------------------------------------------------------------------
    // publish() — respuesta 201 con ID
    // -------------------------------------------------------------------------

    @Test
    void publish_http201WithId_returnsExternalPostId() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        LinkedInClientAdapter adapter = new LinkedInClientAdapter(VALID_TOKEN, VALID_PERSON);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        server.expect(requestTo(UGCPOSTS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\": \"urn:li:share:9876543210\"}"));

        String result = adapter.publish("Contenido de prueba para LinkedIn");

        assertEquals("urn:li:share:9876543210", result);
        server.verify();
    }

    // -------------------------------------------------------------------------
    // publish() — respuesta 400 → ChannelPublicationException
    // -------------------------------------------------------------------------

    @Test
    void publish_http400_throwsChannelPublicationException() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        LinkedInClientAdapter adapter = new LinkedInClientAdapter(VALID_TOKEN, VALID_PERSON);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        server.expect(requestTo(UGCPOSTS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Invalid request\"}"));

        ChannelPublicationException ex = assertThrows(
                ChannelPublicationException.class,
                () -> adapter.publish("Contenido de prueba")
        );

        assertEquals(400, ex.getHttpStatus());
        server.verify();
    }

    // -------------------------------------------------------------------------
    // publish() — respuesta 500 → ChannelPublicationException
    // -------------------------------------------------------------------------

    @Test
    void publish_http500_throwsChannelPublicationException() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        LinkedInClientAdapter adapter = new LinkedInClientAdapter(VALID_TOKEN, VALID_PERSON);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        server.expect(requestTo(UGCPOSTS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Internal server error\"}"));

        ChannelPublicationException ex = assertThrows(
                ChannelPublicationException.class,
                () -> adapter.publish("Contenido de prueba")
        );

        assertEquals(500, ex.getHttpStatus());
        server.verify();
    }

    // -------------------------------------------------------------------------
    // publish() — respuesta 401 → ChannelPublicationException
    // -------------------------------------------------------------------------

    @Test
    void publish_http401_throwsChannelPublicationException() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        LinkedInClientAdapter adapter = new LinkedInClientAdapter(VALID_TOKEN, VALID_PERSON);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        server.expect(requestTo(UGCPOSTS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Unauthorized\"}"));

        ChannelPublicationException ex = assertThrows(
                ChannelPublicationException.class,
                () -> adapter.publish("Contenido de prueba")
        );

        assertEquals(401, ex.getHttpStatus());
        server.verify();
    }
}
