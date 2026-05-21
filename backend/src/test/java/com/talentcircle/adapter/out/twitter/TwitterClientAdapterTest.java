package com.talentcircle.adapter.out.twitter;

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
 * Tests unitarios para {@link TwitterClientAdapter}.
 *
 * <p>La validación del bearer token ocurre en {@link TwitterClientAdapter#publish(String)},
 * no en el constructor. Se usa {@link ReflectionTestUtils} para inyectar un
 * {@link RestTemplate} controlado por {@link MockRestServiceServer}, siguiendo el mismo
 * patrón que {@code LinkedInClientAdapterTest}.
 */
class TwitterClientAdapterTest {

    private static final String VALID_TOKEN = "valid-bearer-token";
    private static final String TWEETS_URL  = "https://api.twitter.com/2/tweets";

    // -------------------------------------------------------------------------
    // publish() — validación del token
    // -------------------------------------------------------------------------

    @Test
    void publish_emptyBearerToken_throwsIllegalStateException() {
        TwitterClientAdapter adapter = new TwitterClientAdapter("");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adapter.publish("Contenido de prueba")
        );
        assertTrue(ex.getMessage().contains("TWITTER_BEARER_TOKEN"),
                "El mensaje debe mencionar TWITTER_BEARER_TOKEN");
    }

    @Test
    void publish_blankBearerToken_throwsIllegalStateException() {
        TwitterClientAdapter adapter = new TwitterClientAdapter("   ");

        assertThrows(
                IllegalStateException.class,
                () -> adapter.publish("Contenido de prueba")
        );
    }

    // -------------------------------------------------------------------------
    // publish() — truncación de contenido
    // -------------------------------------------------------------------------

    @Test
    void publish_contentOf280Chars_doesNotTruncate() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        TwitterClientAdapter adapter = new TwitterClientAdapter(VALID_TOKEN);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        String content280 = "A".repeat(280);

        // El cuerpo enviado debe contener exactamente los 280 caracteres originales
        server.expect(requestTo(TWEETS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"" + content280 + "\"")))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"data\":{\"id\":\"111\",\"text\":\"" + content280 + "\"}}"));

        String result = adapter.publish(content280);

        assertEquals("111", result);
        server.verify();
    }

    @Test
    void publish_contentOf281Chars_truncatesTo277PlusEllipsis() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        TwitterClientAdapter adapter = new TwitterClientAdapter(VALID_TOKEN);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        String content281 = "B".repeat(281);
        String expectedText = "B".repeat(277) + "...";

        // El cuerpo enviado debe contener el texto truncado, no el original
        server.expect(requestTo(TWEETS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"" + expectedText + "\"")))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"data\":{\"id\":\"222\",\"text\":\"" + expectedText + "\"}}"));

        String result = adapter.publish(content281);

        assertEquals("222", result);
        server.verify();
    }

    // -------------------------------------------------------------------------
    // publish() — respuesta 201 con ID
    // -------------------------------------------------------------------------

    @Test
    void publish_http201WithId_returnsExternalTweetId() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        TwitterClientAdapter adapter = new TwitterClientAdapter(VALID_TOKEN);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        server.expect(requestTo(TWEETS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"data\":{\"id\":\"1234567890\",\"text\":\"tweet content\"}}"));

        String result = adapter.publish("tweet content");

        assertEquals("1234567890", result);
        server.verify();
    }

    // -------------------------------------------------------------------------
    // publish() — respuesta 403 → ChannelPublicationException
    // -------------------------------------------------------------------------

    @Test
    void publish_http403_throwsChannelPublicationException() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rt);

        TwitterClientAdapter adapter = new TwitterClientAdapter(VALID_TOKEN);
        ReflectionTestUtils.setField(adapter, "restTemplate", rt);

        server.expect(requestTo(TWEETS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"title\":\"Forbidden\",\"detail\":\"You are not allowed to create a Tweet.\"}"));

        ChannelPublicationException ex = assertThrows(
                ChannelPublicationException.class,
                () -> adapter.publish("Contenido de prueba")
        );

        assertEquals(403, ex.getHttpStatus());
        server.verify();
    }
}
