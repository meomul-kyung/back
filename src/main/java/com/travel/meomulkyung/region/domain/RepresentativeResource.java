package com.travel.meomulkyung.region.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "region_representative_resources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RepresentativeResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "place_name", nullable = false, length = 200)
    private String placeName;

    @Column(length = 100)
    private String category;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "short_description", length = 1000)
    private String shortDescription;

    public RepresentativeResource(Region region, int displayOrder, Long placeId, String placeName, String category,
                                  String imageUrl, String shortDescription) {
        this.region = region;
        this.displayOrder = displayOrder;
        this.placeId = placeId;
        this.placeName = placeName;
        this.category = category;
        this.imageUrl = imageUrl;
        this.shortDescription = shortDescription;
    }
}
