package com.talentcircle.adapter.out.newsletter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests unitarios para {@link NewsletterPublisherAdapter}.
 *
 * <p>El adaptador es un no-op: no realiza llamadas externas y retorna
 * {@code null} como {@code externalPostId}, ya que la publicación del
 * newsletter se completa cuando {@code PublicationService} actualiza
 * {@code Draft.status = PUBLISHED}.
 */
class NewsletterPublisherAdapterTest {

    private final NewsletterPublisherAdapter adapter = new NewsletterPublisherAdapter();

    // -------------------------------------------------------------------------
    // publish() — retorna null (no-op)
    // -------------------------------------------------------------------------

    @Test
    void publish_anyContent_returnsNull() {
        String result = adapter.publish("Contenido de prueba para el newsletter");

        assertNull(result, "publish() debe retornar null ya que no hay ID externo para Newsletter");
    }

    @Test
    void publish_emptyContent_returnsNull() {
        String result = adapter.publish("");

        assertNull(result);
    }

    @Test
    void publish_nullContent_returnsNull() {
        String result = adapter.publish(null);

        assertNull(result);
    }
}
