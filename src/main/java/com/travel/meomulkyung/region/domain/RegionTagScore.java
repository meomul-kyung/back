package com.travel.meomulkyung.region.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "region_tag_scores", uniqueConstraints = @UniqueConstraint(
        name = "uk_region_tag_score_region_tag", columnNames = {"region_id", "tag_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionTagScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_tag_score_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(nullable = false)
    private int score;

    public RegionTagScore(Region region, Tag tag, int score) {
        this.region = region;
        this.tag = tag;
        this.score = score;
    }
}
