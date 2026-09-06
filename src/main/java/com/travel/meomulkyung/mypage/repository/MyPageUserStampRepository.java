package com.travel.meomulkyung.mypage.repository;

import com.travel.meomulkyung.itinerary.domain.UserStamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 마이페이지 전용 스탬프 조회. */
public interface MyPageUserStampRepository extends JpaRepository<UserStamp, Long> {

    @Query("""
            select stamp from UserStamp stamp
            join fetch stamp.region
            where stamp.user.id = :userId
            """)
    List<UserStamp> findAllForUser(@Param("userId") Long userId);
}
