package com.talentcircle.adapter.out.newsletter;

import com.talentcircle.domain.port.out.ChannelPublisherPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida para el canal Newsletter.
 *
 * <p>El Newsletter no requiere llamada a una API externa: el borrador ya está
 * almacenado en la base de datos y {@code NewsletterController} lo expone en
 * {@code GET /api/v1/public/newsletters} cuando su {@code status} es {@code PUBLISHED}.
 * {@code PublicationService} se encarga de actualizar ese estado; por tanto,
 * este adaptador es un no-op que retorna {@code null} como {@code externalPostId}.
 */
@Component
public class NewsletterPublisherAdapter implements ChannelPublisherPort {

    /**
     * No realiza ninguna llamada externa.
     *
     * <p>La publicación del newsletter se completa cuando {@code PublicationService}
     * actualiza {@code Draft.status = PUBLISHED}. No hay ID externo que retornar.
     *
     * @param content Texto del borrador (no utilizado en este adaptador).
     * @return {@code null}, ya que no existe un ID de post externo para Newsletter.
     */
    @Override
    public String publish(String content) {
        // La publicación de newsletter se completa cuando PublicationService
        // actualiza Draft.status = PUBLISHED. No hay llamada externa.
        return null;
    }
}
