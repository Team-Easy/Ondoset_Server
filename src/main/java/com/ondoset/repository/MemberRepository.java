package com.ondoset.repository;

import com.ondoset.domain.Member;
import com.ondoset.dto.admin.blacklist.GetRootDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	Boolean existsByName(String name);

	Boolean existsByNickname(String nickname);

	Member findByName(String name);

	@Query("select new com.ondoset.dto.admin.blacklist.GetRootDTO(m.id, m.nickname, m.banPeriod) " +
			"from Member m where m.banPeriod>0")
	List<GetRootDTO> findByBanPeriodGreaterThan();

	@Query(nativeQuery = true, value =
			"select count(distinct member_id) " +
			"from (select updated, member_id from clothes " +
				"union select cs.updated, member_id from consisting cs join clothes ct " +
				"union select cd.updated, member_id from coordi cd join consisting cs join clothes ct " +
				"union select updated, member_id from ootd " +
				"union select w.updated, member_id from wearing w join ootd o " +
				"union select updated, follower as member_id from `following` " +
				"union select updated, member_id from `like`) as s " +
			"where updated>=:startDate and updated<:endDate")
	Long countActiveMember(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
