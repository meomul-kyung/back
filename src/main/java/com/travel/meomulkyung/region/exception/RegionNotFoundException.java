package com.travel.meomulkyung.region.exception;

public class RegionNotFoundException extends RuntimeException {

    public RegionNotFoundException(long regionId) {
        super("Region not found: " + regionId);
    }
}
