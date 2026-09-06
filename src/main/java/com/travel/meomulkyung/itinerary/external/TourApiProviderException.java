package com.travel.meomulkyung.itinerary.external;

public class TourApiProviderException extends RuntimeException {
    public TourApiProviderException(String message) {
        super(message);
    }

    public TourApiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
