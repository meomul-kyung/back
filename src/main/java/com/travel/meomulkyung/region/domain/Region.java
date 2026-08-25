package com.travel.meomulkyung.region.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
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

    @Column(name = "ldong_regn_cd", length = 10)
    private String ldongRegnCd;

    @Column(name = "ldong_signgu_cd", length = 10)
    private String ldongSignguCd;

    @Column(name = "identity_statement", nullable = false, length = 500)
    private String identityStatement;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @OneToMany(mappedBy = "region")
    private List<RegionTagScore> regionTagScores = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Region(Long id, String name, String ldongRegnCd, String ldongSignguCd,
                  String identityStatement, String thumbnailUrl) {
        this.id = id;
        this.name = name;
        this.ldongRegnCd = ldongRegnCd;
        this.ldongSignguCd = ldongSignguCd;
        this.identityStatement = identityStatement;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void addRegionTagScore(RegionTagScore regionTagScore) {
        regionTagScores.add(regionTagScore);
    }
}
