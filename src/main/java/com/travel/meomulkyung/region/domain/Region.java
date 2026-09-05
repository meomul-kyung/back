package com.travel.meomulkyung.region.domain;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "regions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @Column(name = "region_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "hero_image_url", length = 1000)
    private String heroImageUrl;

    @Column(name = "identity_statement", nullable = false, length = 500)
    private String identityStatement;

    @Column(nullable = false, length = 1000)
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "region_representative_tags", joinColumns = @JoinColumn(name = "region_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag_code", nullable = false, length = 50)
    @OrderColumn(name = "display_order")
    private List<PreferenceTag> representativeTags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "region_recommended_companions", joinColumns = @JoinColumn(name = "region_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "companion_code", nullable = false, length = 50)
    @OrderColumn(name = "display_order")
    private List<CompanionType> recommendedCompanions = new ArrayList<>();

    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<RepresentativeResource> representativeResources = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "region_local_tips", joinColumns = @JoinColumn(name = "region_id"))
    @Column(name = "tip", nullable = false, length = 1000)
    @OrderColumn(name = "display_order")
    private List<String> localTips = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "region_source_attributions", joinColumns = @JoinColumn(name = "region_id"))
    @Column(name = "source_name", nullable = false, length = 200)
    @OrderColumn(name = "display_order")
    private List<String> sourceAttributions = new ArrayList<>();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Region(Long id, String name, String thumbnailUrl, String heroImageUrl, String identityStatement,
                  String description, List<PreferenceTag> representativeTags,
                  List<CompanionType> recommendedCompanions) {
        this.id = id;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.heroImageUrl = heroImageUrl;
        this.identityStatement = identityStatement;
        this.description = description;
        this.representativeTags = new ArrayList<>(representativeTags);
        this.recommendedCompanions = new ArrayList<>(recommendedCompanions);
    }

    public void addRepresentativeResource(RepresentativeResource resource) {
        representativeResources.add(resource);
    }
}
