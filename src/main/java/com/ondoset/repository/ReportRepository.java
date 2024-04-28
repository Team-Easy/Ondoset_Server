package com.ondoset.repository;

import com.ondoset.domain.Member;
import com.ondoset.domain.OOTD;
import com.ondoset.domain.Report;
import com.ondoset.dto.admin.blacklist.ReporterDTO;
import com.ondoset.dto.admin.blacklist.ReporterListDTO;
import com.ondoset.dto.admin.blacklist.ReportingOotdDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

	Boolean existsByReporterAndOotd(Member member, OOTD ootd);

	@Query("select new com.ondoset.dto.admin.blacklist.ReporterDTO(m.id, m.nickname, count(r)) " +
			"from Report r join r.reporter m group by r.reporter")
	List<ReporterDTO> findReporterList();

	@Query("select new com.ondoset.dto.admin.blacklist.ReportingOotdDTO(o.id, trunc((o.departTime+32400)/86400)*86400-32400 date, " +
			"o.weather, o.lowestTemp, o.highestTemp, o.imageURL, null, r.reason) " +
			"from Report r join r.ootd o where r.reporter=:member order by r.id desc limit 10")
	List<ReportingOotdDTO> findReportingOotdList(@Param("member") Member member);

	@Query("select new com.ondoset.dto.admin.blacklist.ReportingOotdDTO(o.id, trunc((o.departTime+32400)/86400)*86400-32400 date, " +
			"o.weather, o.lowestTemp, o.highestTemp, o.imageURL, null, r.reason) " +
			"from Report r join r.ootd o where r.reporter=:member and r.id<:lastPage order by r.id desc limit 10")
	List<ReportingOotdDTO> findReportingOotdList(@Param("member") Member member, @Param("lastPage") Long lastPage);
}
