package com.talentcircle.domain.port.out;

import com.talentcircle.common.exception.ChannelPublicationException;

/**
 * Puerto genérico para publicar contenido en un canal externo.
 * Cada adaptador (LinkedIn, Twitter, Newsletter) implementa esta interfaz.
 */
public interface ChannelPublisherPort {

    /**
     * Publica el contenido en el canal externo.
     *
     * @param content Texto a publicar (ya procesado: truncado, seleccionado, etc.)
     * @return ID externo del post/tweet creado, o {@code null} para Newsletter.
     * @throws ChannelPublicationException si la publicación falla por un error
     *                                     en el canal externo (respuesta HTTP 4xx/5xx)
     *                                     o por configuración incorrecta del adaptador.
     */
    String publish(String content);
}
