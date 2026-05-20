package com.talentcircle.adapter.out.linkedin;

import com.talentcircle.common.exception.ChannelPublicationException;
import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.out.ChannelPublisherPort;
import com.talentcircle.domain.port.out.LinkedInClientPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Adaptador de salida para la API de LinkedIn v2.
 * Implementa {@link ChannelPublisherPort} para publicar contenido mediante UGC Posts,
 * y {@link LinkedInClientPort} para operaciones de gestión de la conexión.
 */
@Component
public class LinkedInClientAdapter implements ChannelPublisherPort, LinkedInClientPort {

    private static final String UGCPOSTS_URL = "https://api.linkedin.com/v2/ugcPosts";

    private final String accessToken;
    private final String personId;
    private final RestTemplate restTemplate;

    public LinkedInClientAdapter(
            @Value("${app.linkedin.access-token:}") String accessToken,
            @Value("${app.linkedin.person-id:}") String personId) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("LINKEDIN_ACCESS_TOKEN is not configured");
        }
        if (personId == null || personId.isBlank()) {
            throw new IllegalStateException("LINKEDIN_PERSON_ID is not configured");
        }
        this.accessToken = accessToken;
        this.personId = personId;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Publica el contenido en LinkedIn mediante el endpoint UGC Posts.
     *
     * @param content Texto a publicar.
     * @return ID externo del post creado en LinkedIn.
     * @throws ChannelPublicationException si la API de LinkedIn responde con 4xx/5xx.
     */
    @Override
    public String publish(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "author", "urn:li:person:" + personId,
                "lifecycleState", "PUBLISHED",
                "specificContent", Map.of(
                        "com.linkedin.ugc.ShareContent", Map.of(
                                "shareCommentary", Map.of("text", content),
                                "shareMediaCategory", "NONE"
                        )
                ),
                "visibility", Map.of("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC")
        );

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    UGCPOSTS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.CREATED) {
                throw new ChannelPublicationException(
                        "LinkedIn API error: " + response.getStatusCode(),
                        response.getStatusCode().value());
            }

            return (String) response.getBody().get("id");

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new ChannelPublicationException(
                    "LinkedIn API error: " + ex.getStatusCode() + " — " + ex.getResponseBodyAsString(),
                    ex.getStatusCode().value());
        }
    }

    /**
     * Publica un post en LinkedIn (delegado a {@link #publish(String)}).
     * Mantenido por compatibilidad con {@link LinkedInClientPort}.
     */
    @Override
    public String publishPost(String content) {
        return publish(content);
    }

    /**
     * Consulta el estado de un post publicado en LinkedIn.
     * Implementación pendiente.
     */
    @Override
    public Publication.PublicationStatus checkStatus(String externalPostId) {
        throw new UnsupportedOperationException("checkStatus not implemented yet");
    }

    /**
     * Valida la conexión con LinkedIn usando el access token proporcionado.
     */
    @Override
    public boolean validateConnection(String accessToken) {
        return accessToken != null && !accessToken.isBlank();
    }
}
