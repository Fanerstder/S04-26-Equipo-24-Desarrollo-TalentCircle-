package com.talentcircle.adapter.out.twitter;

import com.talentcircle.common.exception.ChannelPublicationException;
import com.talentcircle.domain.port.out.ChannelPublisherPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Adaptador de salida para la API de Twitter v2.
 * Implementa {@link ChannelPublisherPort} para publicar tweets mediante el endpoint
 * {@code POST https://api.twitter.com/2/tweets}.
 *
 * <p>El contenido que supere los 280 caracteres es truncado automáticamente a
 * 277 caracteres + {@code "..."} antes de enviarse, sin modificar el borrador original.</p>
 *
 * <p>La validación del bearer token se realiza en {@link #publish(String)} y no en el
 * constructor, para evitar que el contexto de Spring falle al arrancar cuando el canal
 * Twitter no está configurado en el entorno.</p>
 */
@Component
public class TwitterClientAdapter implements ChannelPublisherPort {

    private static final String TWEETS_URL = "https://api.twitter.com/2/tweets";
    private static final int MAX_TWEET_LENGTH = 280;
    private static final int TRUNCATE_AT = 277;

    private final String bearerToken;
    private final RestTemplate restTemplate;

    public TwitterClientAdapter(@Value("${app.twitter.bearer-token:}") String bearerToken) {
        this.bearerToken = bearerToken;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Publica el contenido como tweet en Twitter.
     *
     * <p>Si el contenido supera los {@value #MAX_TWEET_LENGTH} caracteres, se trunca a
     * {@value #TRUNCATE_AT} + {@code "..."} antes de enviarlo.</p>
     *
     * @param content Texto a publicar.
     * @return ID externo del tweet creado ({@code data.id} de la respuesta de Twitter).
     * @throws IllegalStateException       si {@code TWITTER_BEARER_TOKEN} no está configurado.
     * @throws ChannelPublicationException si la API de Twitter responde con 4xx/5xx.
     */
    @Override
    public String publish(String content) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalStateException("TWITTER_BEARER_TOKEN is not configured");
        }

        String tweetText = content.length() > MAX_TWEET_LENGTH
                ? content.substring(0, TRUNCATE_AT) + "..."
                : content;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("text", tweetText);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    TWEETS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.CREATED) {
                throw new ChannelPublicationException(
                        "Twitter API error: " + response.getStatusCode(),
                        response.getStatusCode().value());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            return (String) data.get("id");

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new ChannelPublicationException(
                    "Twitter API error: " + ex.getStatusCode() + " — " + ex.getResponseBodyAsString(),
                    ex.getStatusCode().value());
        }
    }
}
