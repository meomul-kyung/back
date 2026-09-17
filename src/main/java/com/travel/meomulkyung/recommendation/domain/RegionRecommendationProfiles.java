package com.travel.meomulkyung.recommendation.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RegionRecommendationProfiles {

    public static final int PREFERENCE_TAG_WEIGHT = 10;
    public static final int STAY_DURATION_MATCH_SCORE = 4;

    private RegionRecommendationProfiles() {
    }

    public static final List<RegionRecommendationProfile> ALL = List.of(
            profile(1, "\uc548\ub3d9", "\uc804\ud1b5\ubb38\ud654\uc640 \uc9c0\uc5ed \uc74c\uc2dd\uc744 \ucc9c\ucc9c\ud788 \ub9cc\ub098\ub294 \uc5ec\ud589\uc9c0", "\uc9c0\uc5ed \uc74c\uc2dd\uacfc \uc0b0\ucc45 \uc911\uc2ec\uc758 \uc5ec\ud589\uc5d0 \uc801\ud569\ud569\ub2c8\ub2e4.", List.of("\ud558\ud68c\ub9c8\uc744", "\uc6d4\uc601\uad50"), tags(8, 4, 0, 7, 2, 9, 0, 10, 0), companions(2, 4, 5, 3), 2, 4),
            profile(2, "\uc601\uc8fc", "\uc18c\ubc31\uc0b0\uacfc \uc11c\uc6d0\uc744 \ub530\ub77c \uac78\uc5b4\ubcf4\ub294 \uc5ec\ud589\uc9c0", "\uc790\uc5f0\uacfc \uc5ed\uc0ac\ub97c \ud568\uaed8 \ub290\ub07c\uae30 \uc88b\uc2b5\ub2c8\ub2e4.", List.of("\ubd80\uc11d\uc0ac", "\uc18c\uc218\uc11c\uc6d0"), tags(6, 9, 0, 8, 5, 4, 2, 9, 3), companions(4, 4, 3, 5), 2, 4),
            profile(3, "\ubb38\uacbd", "\uc0c8\uc7ac\uc640 \ub3c4\ub9bd\uacf5\uc6d0\uc758 \ud65c\uae30\ub97c \ub290\ub07c\ub294 \uc5ec\ud589\uc9c0", "\uc77c\ud589\uacfc \ud65c\ub3d9\uc801\uc778 \uc5ec\ud589\uc5d0 \uc5b4\uc6b8\ub9bd\ub2c8\ub2e4.", List.of("\ubb38\uacbd\uc0c8\uc7ac", "\ubb38\uacbd\uc5d0\ucf54\ub7c4\ub4dc"), tags(5, 8, 0, 10, 3, 5, 5, 7, 2), companions(3, 3, 6, 4), 1, 3),
            profile(4, "\uc0c1\uc8fc", "\ub099\ub3d9\uac15 \ubcc0\uc5d0\uc11c \uc790\uc804\uac70\uc640 \uc0b0\ucc45\uc744 \uc990\uae30\ub294 \uc5ec\ud589\uc9c0", "\uc790\uc5f0 \ud65c\ub3d9\uacfc \uc5ec\uc720\ub85c\uc6b4 \uc0b0\ucc45\uc5d0 \uc801\ud569\ud569\ub2c8\ub2e4.", List.of("\uc0c1\uc8fc\ubcf4", "\uacbd\ucc9c\ub300"), tags(2, 7, 0, 7, 4, 6, 10, 5, 1), companions(3, 4, 5, 4), 1, 3),
            profile(5, "\ubd09\ud654", "\uccad\ub7c9\ud55c \uace0\uc6d0\uacfc \ubcc4\ube5b \ud558\ub298\uc744 \ub9cc\ub098\ub294 \uc5ec\ud589\uc9c0", "\uc21c\uc218\ud55c \uc790\uc5f0\uacfc \uc57c\uac04 \uac10\uc0c1\uc5d0 \uc801\ud569\ud569\ub2c8\ub2e4.", List.of("\ubd09\ud654 \uc740\uc5b4\ucd95\uc81c", "\uccad\ub7c9\uc0b0"), tags(1, 10, 0, 7, 8, 2, 2, 4, 10), companions(5, 4, 2, 4), 2, 4),
            profile(6, "\uc601\uc591", "\uc870\uc6a9\ud55c \uc0b0\uacfc \uccad\uc815\ud55c \ubc24\uc744 \ud488\uc740 \uc5ec\ud589\uc9c0", "\uc26c\uc5b4\uac00\ub294 \uc790\uc5f0 \uc5ec\ud589\uc5d0 \uc801\ud569\ud569\ub2c8\ub2e4.", List.of("\uc77c\uc6d4\uc0b0", "\uc218\ube44\uba74 \ubc18\ub527\ubd88\uc774 \uc0dd\ud0dc\uacf5\uc6d0"), tags(1, 9, 0, 6, 10, 3, 1, 3, 9), companions(6, 4, 2, 3), 2, 4),
            profile(7, "\uccad\uc1a1", "\uc8fc\uc655\uc0b0\uacfc \uc628\ucc9c\uc5d0\uc11c \uc790\uc5f0\uc744 \uc26c\uc5b4\uac00\ub294 \uc5ec\ud589\uc9c0", "\uc790\uc5f0 \ud0d0\ubc29\uacfc \ud734\uc2dd\uc744 \ubaa8\ub450 \uc6d0\ud560 \ub54c \uc88b\uc2b5\ub2c8\ub2e4.", List.of("\uc8fc\uc655\uc0b0\uad6d\ub9bd\uacf5\uc6d0", "\uc8fc\uc0b0\uc9c0"), tags(1, 10, 0, 8, 9, 5, 1, 5, 5), companions(4, 5, 4, 5), 2, 4),
            profile(8, "\uc758\uc131", "\uc870\uc6a9\ud55c \ubbfc\uc18d\ubb38\ud654\uc640 \uc2dd\uc0ac\ub97c \ub9cc\ub098\ub294 \uc5ec\ud589\uc9c0", "\ub73b\uae4a\uc740 \ubb38\ud654\uc640 \uc74c\uc2dd\uc744 \ucc3e\ub294 \uc5ec\ud589\uc5d0 \uc801\ud569\ud569\ub2c8\ub2e4.", List.of("\uc870\ubb38\uad6d \uc0ac\uc801\uc9c0", "\ube59\uacc4\uacc4\uace1"), tags(7, 6, 0, 5, 4, 8, 1, 9, 1), companions(4, 3, 3, 5), 1, 3),
            profile(9, "\uccad\ub3c4", "\uc640\uc778\uacfc \uc18c\ubc15\ud55c \uc2dc\uace8 \ud48d\uacbd\uc744 \ub9cc\ub07c\ub294 \uc5ec\ud589\uc9c0", "\uce5c\uad6c\uc640 \ud568\uaed8 \uba39\uace0 \uac78\uc73c\uba70 \uc26c\uae30 \uc88b\uc2b5\ub2c8\ub2e4.", List.of("\uccad\ub3c4\uc640\uc778\ud130\ub110", "\uc6b4\ubb38\uc0ac"), tags(3, 7, 0, 7, 5, 9, 1, 5, 1), companions(2, 5, 6, 4), 1, 3),
            profile(10, "\uc601\ucc9c", "\ubcc4\ube5b \ud558\ub298\uacfc \uc640\uc778\uc774 \ud750\ub974\ub294 \ubcf4\ud604\uc0b0 \uc544\ub7ab\ub9c8\uc744", "\ubc24\ud558\ub298 \uac10\uc0c1\uacfc \ub85c\uceec \uba39\uac70\ub9ac\ub97c \ud568\uaed8 \uc990\uae30\uae30 \uc88b\uc2b5\ub2c8\ub2e4.", List.of("\ubcf4\ud604\uc0b0\ucc9c\ubb38\uacfc\ud559\uad00", "\uc2dc\uc548\ubbf8\uc220\uad00"), tags(4, 8, 0, 6, 7, 8, 1, 7, 10), companions(4, 5, 4, 5), 1, 3),
            profile(11, "\uc6b8\uc9c4", "\ubc14\ub2e4\uc640 \uc628\ucc9c\uc774 \uc5b4\uc6b0\ub7ec\uc9c4 \ub3d9\ud574 \uc5ec\ud589\uc9c0", "\ubc14\ub2e4 \ud48d\uacbd\uacfc \ud734\uc2dd\uc744 \ud568\uaed8 \ub204\ub9ac\uae30 \uc88b\uc2b5\ub2c8\ub2e4.", List.of("\uc131\ub958\uad74", "\ub355\uad6c\uc628\ucc9c"), tags(0, 8, 10, 6, 9, 8, 1, 3, 3), companions(4, 5, 5, 5), 2, 5),
            profile(12, "\uc601\ub355", "\ud478\ub978 \ub3d9\ud574\uc640 \uac8c\uc694\ub9ac\ub97c \uc990\uae30\ub294 \uc5ec\ud589\uc9c0", "\uc74c\uc2dd\uacfc \ubc14\ub2e4\ub97c \uc990\uae30\ub294 \uce5c\uad6c \uc5ec\ud589\uc5d0 \uc801\ud569\ud569\ub2c8\ub2e4.", List.of("\uac15\uad6c\ud56d", "\uc601\ub355 \ub300\uac8c\uac70\ub9ac"), tags(0, 7, 10, 6, 4, 10, 1, 2, 1), companions(2, 4, 6, 5), 1, 3),
            profile(13, "\uace0\ub839", "\ub300\uac00\uc57c\uc758 \uc5ed\uc0ac\uc640 \uc9c0\uc5ed \ubb38\ud654\ub97c \uc5fc\uacb0\ud558\ub294 \uc5ec\ud589\uc9c0", "\uc5ed\uc0ac \ud0d0\ubc29\uacfc \uc5ec\uc720\ub85c\uc6b4 \uac77\uae30\uc5d0 \uc801\ud569\ud569\ub2c8\ub2e4.", List.of("\ub300\uac00\uc57c\ubc15\ubb3c\uad00", "\uc9c0\uc0b0\ub3d9 \uace0\ubd84\uad70"), tags(4, 4, 0, 6, 3, 5, 1, 10, 1), companions(3, 4, 4, 5), 1, 3),
            profile(14, "\uc131\uc8fc", "\ucc38\uc678\uc640 \uac00\uc57c\uc0b0\uc774 \uc870\ud654\ub97c \uc774\ub8e8\ub294 \uc5ec\ud589\uc9c0", "\uc790\uc5f0\uacfc \uc9c0\uc5ed \ub9db\uc744 \uc990\uae30\uae30 \uc88b\uc2b5\ub2c8\ub2e4.", List.of("\uc131\uc8fc\ucc38\uc678\uccb4\ud5d8\ud615\ud14c\ub9c8\uacf5\uc6d0", "\uac00\uc57c\uc0b0\uc5ed\uc0ac\uc2e0\ud654\ud14c\ub9c8\uad00"), tags(4, 8, 0, 6, 4, 9, 2, 6, 1), companions(3, 4, 5, 5), 1, 3),
            profile(15, "\uc6b8\ub989", "\uc12c\uc758 \uc808\uacbd\uacfc \ubc14\ub2e4\ub97c \uae4a\uac8c \uc990\uae30\ub294 \uc5ec\ud589\uc9c0", "\uc5ec\uc720 \uc788\ub294 \uc77c\uc815\uc73c\ub85c \uc12c \uc790\uc5f0\uc744 \ub9cc\ub07c\uae30 \uc88b\uc2b5\ub2c8\ub2e4.", List.of("\ub3c5\ub3c4\uc804\ub9dd\ub300 \ucf00\uc774\ube14\uce74", "\uc131\uc778\ubd09"), tags(0, 9, 10, 7, 7, 6, 2, 3, 6), companions(5, 5, 5, 4), 3, 5)
    );

    private static RegionRecommendationProfile profile(long id, String name, String identity, String reason, List<String> places,
                                                       Map<PreferenceTag, Integer> tagScores, Map<CompanionType, Integer> companionScores,
                                                       int idealMinimumNights, int idealMaximumNights) {
        return new RegionRecommendationProfile(id, name, identity, reason, places, tagScores, companionScores, idealMinimumNights, idealMaximumNights);
    }

    private static Map<PreferenceTag, Integer> tags(int hanok, int nature, int sea, int walking, int healing, int food, int bicycle, int history, int nightSky) {
        Map<PreferenceTag, Integer> scores = new EnumMap<>(PreferenceTag.class);
        scores.put(PreferenceTag.HANOK_CONFUCIANISM, hanok); scores.put(PreferenceTag.NATURE, nature); scores.put(PreferenceTag.SEA, sea);
        scores.put(PreferenceTag.WALKING, walking); scores.put(PreferenceTag.HEALING, healing); scores.put(PreferenceTag.FOOD, food);
        scores.put(PreferenceTag.BICYCLE, bicycle); scores.put(PreferenceTag.HISTORY, history); scores.put(PreferenceTag.NIGHT_SKY, nightSky);
        return Map.copyOf(scores);
    }

    private static Map<CompanionType, Integer> companions(int solo, int couple, int friends, int family) {
        Map<CompanionType, Integer> scores = new EnumMap<>(CompanionType.class);
        scores.put(CompanionType.SOLO, solo); scores.put(CompanionType.COUPLE, couple); scores.put(CompanionType.FRIENDS, friends); scores.put(CompanionType.FAMILY, family);
        return Map.copyOf(scores);
    }

    public static RegionRecommendationProfile findByRegionId(long regionId) {
        return ALL.stream()
                .filter(profile -> profile.regionId() == regionId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown region id: " + regionId));
    }

    public record RegionRecommendationProfile(long regionId, String regionName, String identityStatement,
                                              String recommendationReason, List<String> representativePlaces,
                                              Map<PreferenceTag, Integer> tagScores, Map<CompanionType, Integer> companionScores,
                                              int idealMinimumNights, int idealMaximumNights) {
    }
}
