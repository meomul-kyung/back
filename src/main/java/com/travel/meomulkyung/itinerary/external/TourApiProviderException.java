package com.travel.meomulkyung.itinerary.external;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

public class TourApiProviderException extends RuntimeException {
    public enum FailureType {
        TIMEOUT,
        CONNECTION,
        HTTP_STATUS,
        UNKNOWN
    }

    private final FailureType failureType;
    private final Integer httpStatusCode;

    public TourApiProviderException(String message) {
        this(message, null, FailureType.UNKNOWN, null);
    }

    public TourApiProviderException(String message, Throwable cause) {
        this(message, cause, FailureType.UNKNOWN, null);
    }

    private TourApiProviderException(String message, Throwable cause, FailureType failureType, Integer httpStatusCode) {
        super(message, cause);
        this.failureType = failureType;
        this.httpStatusCode = httpStatusCode;
    }

    public static TourApiProviderException fromHttpError(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        return new TourApiProviderException("TourAPI returned HTTP status " + statusCode + ".", exception,
                FailureType.HTTP_STATUS, statusCode);
    }

    public static TourApiProviderException fromResourceAccessError(ResourceAccessException exception) {
        FailureType failureType = communicationFailureType(exception);
        return new TourApiProviderException("TourAPI " + communicationFailureDescription(failureType, exception) + ".", exception,
                failureType, null);
    }

    public static TourApiProviderException fromClientError(RuntimeException exception) {
        return new TourApiProviderException("TourAPI request failed (" + exception.getClass().getSimpleName() + ").", exception,
                FailureType.UNKNOWN, null);
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getCauseClassName() {
        Throwable cause = getCause();
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause == null ? null : cause.getClass().getSimpleName();
    }

    private static FailureType communicationFailureType(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return FailureType.TIMEOUT;
            }
            if (cause instanceof ConnectException) {
                return FailureType.CONNECTION;
            }
        }
        return FailureType.UNKNOWN;
    }

    private static String communicationFailureDescription(FailureType failureType, ResourceAccessException exception) {
        return switch (failureType) {
            case TIMEOUT -> "request timed out (SocketTimeoutException)";
            case CONNECTION -> "connection failed (ConnectException)";
            case UNKNOWN, HTTP_STATUS -> "request failed (" + exception.getClass().getSimpleName() + ")";
        };
    }
}
