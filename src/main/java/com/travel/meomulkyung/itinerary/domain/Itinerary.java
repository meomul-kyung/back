package com.travel.meomulkyung.itinerary.domain;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.*;
import java.util.*;

@Entity @Table(name = "itineraries") @Getter @NoArgsConstructor
public class Itinerary {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name="itinerary_id") private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="user_id") private User user;
 @ManyToOne(optional=false) @JoinColumn(name="region_id") private Region region;
 @Column(nullable=false,length=200) private String title;
 @Column(nullable=false) private LocalDate startDate;
 @Column(nullable=false) private LocalDate endDate;
 @Column(nullable=false) private int nights;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ItineraryStatus status;
 @Column(nullable=false) private boolean bookmarked;
 private LocalDateTime bookmarkedAt;
 @Column(nullable=false) private int generationVersion;
 @ElementCollection(targetClass=PreferenceTag.class) @CollectionTable(name="itinerary_preference_tags",joinColumns=@JoinColumn(name="itinerary_id")) @Enumerated(EnumType.STRING) @Column(name="preference_tag",nullable=false) private Set<PreferenceTag> preferenceTags=new LinkedHashSet<>();
 @Enumerated(EnumType.STRING) @Column(nullable=false) private CompanionType companionType;
 @OneToMany(mappedBy="itinerary",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("dayNumber ASC, sequence ASC") private List<ItineraryItem> items=new ArrayList<>();
 @CreationTimestamp @Column(updatable=false) private LocalDateTime createdAt;
 @UpdateTimestamp private LocalDateTime updatedAt;
 public Itinerary(User u,Region r,LocalDate s,int n,List<PreferenceTag> tags,CompanionType c){user=u;region=r;startDate=s;nights=n;endDate=s.plusDays(n);title=r.getName()+" "+n+"\ubc15 \uc77c\uc815";status=ItineraryStatus.DRAFT;generationVersion=1;preferenceTags.addAll(tags);companionType=c;}
 public void replaceItems(List<ItineraryItem> next){items.clear();items.addAll(next);}
 public void addItem(ItineraryItem item){items.add(item);}
 public void incrementGenerationVersion(){generationVersion++;}
 public void bookmark(){bookmarked=true;if(bookmarkedAt==null) bookmarkedAt=LocalDateTime.now();}
 public void unbookmark(){bookmarked=false;bookmarkedAt=null;}
 public void complete(){status=ItineraryStatus.COMPLETED;}
}
