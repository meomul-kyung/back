package com.travel.meomulkyung.itinerary.domain;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.UpdateTimestamp; import java.time.LocalDate; import java.time.LocalDateTime;
@Entity @Table(name="itinerary_items") @Getter @NoArgsConstructor
public class ItineraryItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="item_id") private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="itinerary_id") private Itinerary itinerary;
 @Column(nullable=false) private int dayNumber; @Column(nullable=false,name="sequence_number") private int sequence;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ItineraryItemType itemType;
 private Long contentId; @Column(length=200) private String customTitle; @Column(length=500) private String imageUrl; @Column(length=500) private String address; private LocalDate eventStartDate; private LocalDate eventEndDate; @Column(nullable=false) private boolean replaceable;
 @CreationTimestamp @Column(updatable=false) private LocalDateTime createdAt; @UpdateTimestamp private LocalDateTime updatedAt;
 public ItineraryItem(Itinerary i,int d,int s,ItineraryItemType t,Long content,String title){itinerary=i;dayNumber=d;sequence=s;itemType=t;contentId=content;customTitle=title;replaceable=t==ItineraryItemType.TOURIST_SPOT||t==ItineraryItemType.RESTAURANT||t==ItineraryItemType.EXPERIENCE;}
 public ItineraryItem(Itinerary i,int d,int s,Long festivalId,String title,String imageUrl,String address,LocalDate eventStartDate,LocalDate eventEndDate){itinerary=i;dayNumber=d;sequence=s;itemType=ItineraryItemType.FESTIVAL;contentId=festivalId;customTitle=title;this.imageUrl=imageUrl;this.address=address;this.eventStartDate=eventStartDate;this.eventEndDate=eventEndDate;replaceable=false;}
 public ItineraryItem(Itinerary i,int d,int s,ItineraryItemType t,Long content,String title,String imageUrl,String address){this(i,d,s,t,content,title);this.imageUrl=imageUrl;this.address=address;}
 public void replace(Long content){contentId=content;customTitle=null;imageUrl=null;address=null;}
 public void replace(Long content,String title,String imageUrl,String address){contentId=content;customTitle=title;this.imageUrl=imageUrl;this.address=address;}
}
