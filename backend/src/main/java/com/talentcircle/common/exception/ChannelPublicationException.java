package com.talentcircle.common.exception;

/**
 * Excepción lanzada cuando un adaptador de canal externo falla al publicar contenido.
 * Encapsula el código de estado HTTP recibido de la API externa para facilitar
 * el diagnóstico y el registro de errores.
 */
public class ChannelPublicationException extends RuntimeException {

    private final int httpStatus;

    /**
     * Construye una nueva excepción con el mensaje de error y el código HTTP.
     *
     * @param message   Descripción del error, incluyendo el cuerpo de la respuesta si aplica.
     * @param httpStatus Código de estado HTTP retornado por la API externa (4xx o 5xx).
     */
    public ChannelPublicationException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * Retorna el código de estado HTTP asociado al fallo de publicación.
     *
     * @return Código HTTP (e.g. 400, 401, 500).
     */
    public int getHttpStatus() {
        return httpStatus;
    }
}
