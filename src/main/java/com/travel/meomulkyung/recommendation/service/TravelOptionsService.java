package com.travel.meomulkyung.recommendation.service;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.recommendation.dto.TravelOptionsResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TravelOptionsService {

    private static final int MINIMUM_PREFERENCE_SELECTION = 1;
    private static final int MAXIMUM_PREFERENCE_SELECTION = 3;
    private static final int MINIMUM_STAY_NIGHTS = 1;
    private static final int MAXIMUM_STAY_NIGHTS = 7;

    public TravelOptionsResponse getTravelOptions() {
        return new TravelOptionsResponse(
                toOptions(PreferenceTag.values()),
                new TravelOptionsResponse.PreferenceSelection(
                        MINIMUM_PREFERENCE_SELECTION,
                        MAXIMUM_PREFERENCE_SELECTION
                ),
                toOptions(CompanionType.values()),
                new TravelOptionsResponse.StayDuration(MINIMUM_STAY_NIGHTS, MAXIMUM_STAY_NIGHTS)
        );
    }

    private List<TravelOptionsResponse.Option> toOptions(PreferenceTag[] preferenceTags) {
        return Arrays.stream(preferenceTags)
                .map(preferenceTag -> new TravelOptionsResponse.Option(
                        preferenceTag.getCode(),
                        preferenceTag.getLabel()
                ))
                .toList();
    }

    private List<TravelOptionsResponse.Option> toOptions(CompanionType[] companionTypes) {
        return Arrays.stream(companionTypes)
                .map(companionType -> new TravelOptionsResponse.Option(
                        companionType.getCode(),
                        companionType.getLabel()
                ))
                .toList();
    }
}
