package com.travel.meomulkyung.itinerary.domain;
import com.travel.meomulkyung.region.domain.Region; import com.travel.meomulkyung.user.domain.User; import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import java.time.LocalDateTime;
@Entity @Table(name="completed_trips") @Getter @NoArgsConstructor public class CompletedTrip { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="completed_trip_id") private Long id; @OneToOne(optional=false) @JoinColumn(name="itinerary_id",unique=true) private Itinerary itinerary; @ManyToOne(optional=false) @JoinColumn(name="user_id") private User user; @ManyToOne(optional=false) @JoinColumn(name="region_id") private Region region; private int stayHours; private int partySize; private long totalSpent; private LocalDateTime completedAt;
 /** 국민여행조사 단가 기반 예상 소비 금액(원). 완료 시점 스냅샷 — 단가 정책이 바뀌어도 과거 기록은 유지된다. */
 @Column(name="estimated_spending") private long estimatedSpending;
 /** 생활인구 산입 일수. 완료 시점 스냅샷. */
 @Column(name="population_contribution_days") private int populationContributionDays;
 /** 산출에 사용한 기여도 정책 버전. */
 @Column(name="contribution_policy_version",length=40) private String contributionPolicyVersion;
 public CompletedTrip(Itinerary i,int h,int p,long total){itinerary=i;user=i.getUser();region=i.getRegion();stayHours=h;partySize=p;totalSpent=total;completedAt=LocalDateTime.now();}
 /** 완료 등록 시 산출한 기여 지표를 스냅샷으로 기록한다. */
 public void applyContribution(long estimatedSpending,int populationContributionDays,String contributionPolicyVersion){this.estimatedSpending=estimatedSpending;this.populationContributionDays=populationContributionDays;this.contributionPolicyVersion=contributionPolicyVersion;}
}
