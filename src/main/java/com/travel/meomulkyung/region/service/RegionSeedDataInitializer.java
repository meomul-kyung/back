package com.travel.meomulkyung.region.service;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.domain.RepresentativeResource;
import com.travel.meomulkyung.region.repository.RegionRepository;
import com.travel.meomulkyung.region.repository.RepresentativeResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RegionSeedDataInitializer implements ApplicationRunner {

    private final RegionRepository regionRepository;
    private final RepresentativeResourceRepository representativeResourceRepository;

    @Override
    public void run(ApplicationArguments args) {
        initialize();
    }

    @Transactional
    public void initialize() {
        for (Seed seed : SEEDS) {
            if (regionRepository.existsById(seed.id())) {
                continue;
            }
            Region region = regionRepository.save(new Region(seed.id(), seed.name(), null, null, seed.identityStatement(),
                    seed.description(), seed.tags(), seed.companions()));
            for (int index = 0; index < seed.placeNames().size(); index++) {
                representativeResourceRepository.save(new RepresentativeResource(region, index,
                        null, seed.placeNames().get(index), null, null, null));
            }
        }
    }

    private static final List<Seed> SEEDS = List.of(
            seed(1, "\uC548\uB3D9", "\uC804\uD1B5\uBB38\uD654\uC640 \uC9C0\uC5ED \uC74C\uC2DD\uC744 \uCC9C\uCC9C\uD788 \uB9CC\uB098\uB294 \uC5EC\uD589\uC9C0", "\uC9C0\uC5ED \uC74C\uC2DD\uACFC \uC0B0\uCC45 \uC911\uC2EC\uC758 \uC5EC\uD589\uC5D0 \uC801\uD569\uD569\uB2C8\uB2E4.", tags(PreferenceTag.HISTORY, PreferenceTag.FOOD, PreferenceTag.HANOK_CONFUCIANISM), companions(CompanionType.FRIENDS, CompanionType.COUPLE), places("\uD558\uD68C\uB9C8\uC744", "\uC6D4\uC601\uAD50")),
            seed(2, "\uC601\uC8FC", "\uC18C\uBC31\uC0B0\uACFC \uC11C\uC6D0\uC744 \uB530\uB77C \uAC78\uC5B4\uBCF4\uB294 \uC5EC\uD589\uC9C0", "\uC790\uC5F0\uACFC \uC5ED\uC0AC\uB97C \uD568\uAED8 \uB290\uB07C\uAE30 \uC88B\uC2B5\uB2C8\uB2E4.", tags(PreferenceTag.NATURE, PreferenceTag.HISTORY, PreferenceTag.WALKING), companions(CompanionType.FAMILY, CompanionType.SOLO), places("\uBD80\uC11D\uC0AC", "\uC18C\uC218\uC11C\uC6D0")),
            seed(3, "\uBB38\uACBD", "\uC0C8\uC7AC\uC640 \uB3C4\uB9BD\uACF5\uC6D0\uC758 \uD65C\uAE30\uB97C \uB290\uB07C\uB294 \uC5EC\uD589\uC9C0", "\uC77C\uD589\uACFC \uD65C\uB3D9\uC801\uC778 \uC5EC\uD589\uC5D0 \uC5B4\uC6B8\uB9BD\uB2C8\uB2E4.", tags(PreferenceTag.WALKING, PreferenceTag.NATURE, PreferenceTag.HISTORY), companions(CompanionType.FRIENDS, CompanionType.FAMILY), places("\uBB38\uACBD\uC0C8\uC7AC", "\uBB38\uACBD\uC5D0\uCF54\uB7A8\uB4DC")),
            seed(4, "\uC0C1\uC8FC", "\uB099\uB3D9\uAC15 \uBCC0\uC5D0\uC11C \uC790\uC804\uAC70\uC640 \uC0B0\uCC45\uC744 \uC990\uAE30\uB294 \uC5EC\uD589\uC9C0", "\uC790\uC5F0 \uD65C\uB3D9\uACFC \uC5EC\uC720\uB85C\uC6B4 \uC0B0\uCC45\uC5D0 \uC801\uD569\uD569\uB2C8\uB2E4.", tags(PreferenceTag.BICYCLE, PreferenceTag.NATURE, PreferenceTag.WALKING), companions(CompanionType.FRIENDS, CompanionType.COUPLE), places("\uC0C1\uC8FC\uBCF4", "\uACBD\uCC9C\uB300")),
            seed(5, "\uBD09\uD654", "\uCCAD\uB7C9\uD55C \uACE0\uC6D0\uACFC \uBCC4\uBE5B \uD558\uB298\uC744 \uB9CC\uB098\uB294 \uC5EC\uD589\uC9C0", "\uC21C\uC218\uD55C \uC790\uC5F0\uACFC \uC57C\uAC04 \uAC10\uC0C1\uC5D0 \uC801\uD569\uD569\uB2C8\uB2E4.", tags(PreferenceTag.NATURE, PreferenceTag.NIGHT_SKY, PreferenceTag.HEALING), companions(CompanionType.SOLO, CompanionType.COUPLE), places("\uBD09\uD654 \uC740\uC5B4\uCD95\uC81C", "\uCCAD\uB7C9\uC0B0")),
            seed(6, "\uC601\uC591", "\uC870\uC6A9\uD55C \uC0B0\uACFC \uCCAD\uC815\uD55C \uBC24\uC744 \uD488\uC740 \uC5EC\uD589\uC9C0", "\uC26C\uC5B4\uAC00\uB294 \uC790\uC5F0 \uC5EC\uD589\uC5D0 \uC801\uD569\uD569\uB2C8\uB2E4.", tags(PreferenceTag.HEALING, PreferenceTag.NATURE, PreferenceTag.NIGHT_SKY), companions(CompanionType.SOLO, CompanionType.COUPLE), places("\uC77C\uC6D4\uC0B0", "\uC218\uBE44\uBA74 \uBC18\uB527\uBD88\uC774 \uC0DD\uD0DC\uACF5\uC6D0")),
            seed(7, "\uCCAD\uC1A1", "\uC8FC\uC655\uC0B0\uACFC \uC628\uCC9C\uC5D0\uC11C \uC790\uC5F0\uC744 \uC26C\uC5B4\uAC00\uB294 \uC5EC\uD589\uC9C0", "\uC790\uC5F0 \uD0D0\uBC29\uACFC \uD734\uC2DD\uC744 \uBAA8\uB450 \uC6D0\uD560 \uB54C \uC88B\uC2B5\uB2C8\uB2E4.", tags(PreferenceTag.NATURE, PreferenceTag.HEALING, PreferenceTag.WALKING), companions(CompanionType.COUPLE, CompanionType.FAMILY), places("\uC8FC\uC655\uC0B0\uAD6D\uB9BD\uACF5\uC6D0", "\uC8FC\uC0B0\uC9C0")),
            seed(8, "\uC758\uC131", "\uC870\uC6A9\uD55C \uBBFC\uC18D\uBB38\uD654\uC640 \uC2DD\uC0AC\uB97C \uB9CC\uB098\uB294 \uC5EC\uD589\uC9C0", "\uB73B\uAE4A\uC740 \uBB38\uD654\uC640 \uC74C\uC2DD\uC744 \uCC3E\uB294 \uC5EC\uD589\uC5D0 \uC801\uD569\uD569\uB2C8\uB2E4.", tags(PreferenceTag.HISTORY, PreferenceTag.FOOD, PreferenceTag.HANOK_CONFUCIANISM), companions(CompanionType.FAMILY, CompanionType.SOLO), places("\uC870\uBB38\uAD6D \uC0AC\uC801\uC9C0", "\uBE59\uACC4\uACC4\uACE1")),
            seed(9, "\uCCAD\uB3C4", "\uC640\uC778\uACFC \uC18C\uBC15\uD55C \uC2DC\uACE8 \uD48D\uACBD\uC744 \uB9CC\uB07C\uB294 \uC5EC\uD589\uC9C0", "\uCE5C\uAD6C\uC640 \uD568\uAED8 \uBA39\uACE0 \uAC78\uC73C\uBA70 \uC26C\uAE30 \uC88B\uC2B5\uB2C8\uB2E4.", tags(PreferenceTag.FOOD, PreferenceTag.NATURE, PreferenceTag.WALKING), companions(CompanionType.FRIENDS, CompanionType.COUPLE), places("\uCCAD\uB3C4\uC640\uC778\uD130\uB110", "\uC6B4\uBB38\uC0AC")),
            seed(10, "\uC608\uCC9C", "\uB0B4\uC131\uCC9C\uBCC0\uC758 \uC5EC\uC720\uC640 \uC804\uD1B5\uC744 \uD488\uC740 \uC5EC\uD589\uC9C0", "\uCC9C\uCC9C\uD788 \uAC77\uB294 \uBB38\uD654 \uC5EC\uD589\uC5D0 \uC5B4\uC6B8\uB9BD\uB2C8\uB2E4.", tags(PreferenceTag.WALKING, PreferenceTag.HISTORY, PreferenceTag.NATURE), companions(CompanionType.FAMILY, CompanionType.SOLO), places("\uD68C\uB8E1\uD3EC", "\uC608\uCC9C\uC628\uCC9C")),
            seed(11, "\uC6B8\uC9C4", "\uBC14\uB2E4\uC640 \uC628\uCC9C\uC774 \uC5B4\uC6B0\uB7EC\uC9C4 \uB3D9\uD574 \uC5EC\uD589\uC9C0", "\uBC14\uB2E4 \uD48D\uACBD\uACFC \uD734\uC2DD\uC744 \uD568\uAED8 \uB204\uB9AC\uAE30 \uC88B\uC2B5\uB2C8\uB2E4.", tags(PreferenceTag.SEA, PreferenceTag.HEALING, PreferenceTag.NATURE), companions(CompanionType.COUPLE, CompanionType.FRIENDS), places("\uC131\uB958\uAD74", "\uB355\uAD6C\uC628\uCC9C")),
            seed(12, "\uC601\uB355", "\uD478\uB978 \uB3D9\uD574\uC640 \uAC8C\uC694\uB9AC\uB97C \uC990\uAE30\uB294 \uC5EC\uD589\uC9C0", "\uC74C\uC2DD\uACFC \uBC14\uB2E4\uB97C \uC990\uAE30\uB294 \uCE5C\uAD6C \uC5EC\uD589\uC5D0 \uC801\uD569\uD569\uB2C8\uB2E4.", tags(PreferenceTag.FOOD, PreferenceTag.SEA, PreferenceTag.NATURE), companions(CompanionType.FRIENDS, CompanionType.FAMILY), places("\uAC15\uAD6C\uD56D", "\uC601\uB355 \uB300\uAC8C\uAC70\uB9AC")),
            seed(13, "\uACE0\uB839", "\uB300\uAC00\uC57C\uC758 \uC5ED\uC0AC\uC640 \uC9C0\uC5ED \uBB38\uD654\uB97C \uC5FC\uACB0\uD558\uB294 \uC5EC\uD589\uC9C0", "\uC5ED\uC0AC \uD0D0\uBC29\uACFC \uC5EC\uC720\uB85C\uC6B4 \uAC77\uAE30\uC5D0 \uC801\uD569\uD569\uB2C8\uB2E4.", tags(PreferenceTag.HISTORY, PreferenceTag.WALKING, PreferenceTag.FOOD), companions(CompanionType.FAMILY, CompanionType.COUPLE), places("\uB300\uAC00\uC57C\uBC15\uBB3C\uAD00", "\uC9C0\uC0B0\uB3D9 \uACE0\uBD84\uAD70")),
            seed(14, "\uC131\uC8FC", "\uCC38\uC678\uC640 \uAC00\uC57C\uC0B0\uC774 \uC870\uD654\uB97C \uC774\uB8E8\uB294 \uC5EC\uD589\uC9C0", "\uC790\uC5F0\uACFC \uC9C0\uC5ED \uB9DB\uC744 \uC990\uAE30\uAE30 \uC88B\uC2B5\uB2C8\uB2E4.", tags(PreferenceTag.FOOD, PreferenceTag.NATURE, PreferenceTag.HISTORY), companions(CompanionType.FRIENDS, CompanionType.FAMILY), places("\uC131\uC8FC\uCC38\uC678\uCCB4\uD5D8\uD615\uD14C\uB9C8\uACF5\uC6D0", "\uAC00\uC57C\uC0B0\uC5ED\uC0AC\uC2E0\uD654\uD14C\uB9C8\uAD00")),
            seed(15, "\uC6B8\uB989", "\uC12C\uC758 \uC808\uACBD\uACFC \uBC14\uB2E4\uB97C \uAE4A\uAC8C \uC990\uAE30\uB294 \uC5EC\uD589\uC9C0", "\uC5EC\uC720 \uC788\uB294 \uC77C\uC815\uC73C\uB85C \uC12C \uC790\uC5F0\uC744 \uB9CC\uB07C\uAE30 \uC88B\uC2B5\uB2C8\uB2E4.", tags(PreferenceTag.SEA, PreferenceTag.NATURE, PreferenceTag.HEALING), companions(CompanionType.SOLO, CompanionType.COUPLE), places("\uB3C5\uB3C4\uC804\uB9DD\uB300 \uCF00\uC774\uBE14\uCE74", "\uC131\uC778\uBD09"))
    );

    private static Seed seed(long id, String name, String identity, String description, List<PreferenceTag> tags,
                             List<CompanionType> companions, List<String> places) {
        return new Seed(id, name, identity, description, tags, companions, places);
    }

    private static List<PreferenceTag> tags(PreferenceTag... tags) {
        return List.of(tags);
    }

    private static List<CompanionType> companions(CompanionType... companions) {
        return List.of(companions);
    }

    private static List<String> places(String... places) {
        return List.of(places);
    }

    private record Seed(long id, String name, String identityStatement, String description, List<PreferenceTag> tags,
                        List<CompanionType> companions, List<String> placeNames) {
    }
}
