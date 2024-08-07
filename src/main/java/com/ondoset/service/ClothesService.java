package com.ondoset.service;

import com.ondoset.common.Ai;
import com.ondoset.controller.advice.CustomException;
import com.ondoset.controller.advice.ResponseCode;
import com.ondoset.domain.*;
import com.ondoset.domain.Enum.Category;
import com.ondoset.domain.Enum.Thickness;
import com.ondoset.domain.Tag;
import com.ondoset.dto.clothes.*;
import com.ondoset.dto.kma.ForecastDTO;
import com.ondoset.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import com.ondoset.common.Kma;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Log4j2
@RequiredArgsConstructor
@Service
@Transactional
public class ClothesService {

	private final MemberRepository memberRepository;
	private final CoordiRepository coordiRepository;
	private final OOTDRepository ootdRepository;
	private final ClothesRepository clothesRepository;
	private final TagRepository tagRepository;
	private final Kma kma;
	private final Ai ai;
	@Value("${com.ondoset.resources.path}")
	private String resourcesPath;

	public HomeDTO.res getHome(HomeDTO.req req) {

		// 현재 사용자 조회
		Member member = memberRepository.findByName(SecurityContextHolder.getContext().getAuthentication().getName());

		// req 분해
		Long date = req.getDate();
		Double lat = req.getLat();
		Double lon = req.getLon();

		// 응답 정의
		HomeDTO.res res = new HomeDTO.res();

		log.trace("kma.getForecast start");
		// HomeDTO.res.forcast 획득 (recommend 획득 이전에 수행되어야 함)
		Map<String, Object> forecast = kma.getForecast(lat, lon, date);
		res.setForecast((ForecastDTO) forecast.get("result"));
		log.trace("kma.getForecast end");

		// HomeDTO.res.plan 획득
		Optional<Coordi> planCoordiOptional = coordiRepository.findByConsistings_Clothes_MemberAndDate(member, date);
		planCoordiOptional.ifPresent(coordi -> res.setPlan(getPlan(coordi)));

		log.trace("ai.getSimilarDate start");
		// HomeDTO.res.record 획득
		res.setRecord(getRecord(member, ai.getSimilarDate(member, lat, lon, date), (Double) forecast.get("tempAvg")));

		// 현재 사용자가 뉴비인지 확인
		String reqId = ai.reqIdOf(member.getId());

		log.trace("ai.getRecommend start");
		// HomeDTO.res.recommend 획득
		res.setRecommend(getRecommend(ai.getRecommend((Double) forecast.get("tempAvg"), reqId)));

		// HomeDTO.res.ootd 획득
		if (reqId.equals("0")) {
			res.setOotd(getOotdPreview());
		} else {
			log.trace("ai.getSimilarUser start");
			res.setOotd(getOotdPreview(ai.getSimilarUser(member.getId())));
		}

		return res;
	}
	public List<PlanDTO> getPlan(Coordi planCoordi) {

		List<PlanDTO> plan = new ArrayList<>();
		for (Consisting cs : planCoordi.getConsistings()) {

			Clothes ct = cs.getClothes();

			PlanDTO p = new PlanDTO();
			p.setClothesId(ct.getId());
			p.setName(ct.getName());
			p.setImageURL(ct.getImageURL());
			p.setCategory(ct.getTag().getCategory());
			p.setTag(ct.getTag().getName());
			p.setTagId(ct.getTag().getId());
			p.setThickness(ct.getThickness());

			plan.add(p);
		}
		return plan;
	}
	public List<RecordDTO> getRecord(Member member, List<Long> dateList, Double tempAvg) {
		log.trace("ai.getSimilarDate end");

		List<RecordDTO> record = new ArrayList<>();
		for (Long date : dateList) {

			Optional<Coordi> coordiOptional = coordiRepository.findByConsistings_Clothes_MemberAndDate(member, date - 32400);
			if (coordiOptional.isPresent() && coordiOptional.get().getDepartTime()!=null) {

				// 평균 기온을 비교하여 적절한 응답인지 확인
				Coordi coordi = coordiOptional.get();
				double diff = (Double.valueOf(coordi.getHighestTemp()) + Double.valueOf(coordi.getLowestTemp())) / 2 - tempAvg;
				if (diff > 5 || diff < -5) {
					log.debug("plan of {} is not offered to member {} because diff is {}", date, member.getId(), diff);
					continue;
				}

				List<Consisting> consistingList = coordi.getConsistings();

				RecordDTO r = new RecordDTO();
				r.setDate(date);

				List<ClothesDTO> clothesList = new ArrayList<>();
				for (Consisting cs : consistingList) {

					Clothes c = cs.getClothes();

					ClothesDTO clothes = new ClothesDTO();
					clothes.setClothesId(c.getId());
					clothes.setName(c.getName());
					clothes.setImageURL(c.getImageURL());
					clothes.setCategory(c.getTag().getCategory());
					clothes.setTag(c.getTag().getName());
					clothes.setTagId(c.getTag().getId());
					clothes.setThickness(c.getThickness());

					clothesList.add(clothes);
				}
				r.setClothesList(clothesList);

				record.add(r);
			}
		}
		return record;
	}
	public List<List<RecommendDTO>> getRecommend(List<List<List<Long>>> tagRecommendList) {
		log.trace("ai.getRecommend end");

		List<List<RecommendDTO>> recommend = new ArrayList<>();
		for (List<List<Long>> tagRecommend : tagRecommendList) {

			List<RecommendDTO> recommendElement = new ArrayList<>();
			for (int i = 0; i < tagRecommend.get(0).size(); i++) {

				RecommendDTO r = new RecommendDTO();

				// 중복 태그는 한 번만 조회되는 문제 때문에 in으로 한 번에 조회할 수 없음
				Tag tag = tagRepository.findById(tagRecommend.get(0).get(i)).get();
				String tagName = tag.getName();

				r.setTag(tagName);
				r.setTagId(tag.getId());
				Category category = tag.getCategory();
				r.setCategory(category);

				if (Arrays.asList(Category.SHOE, Category.ACC).contains(category)) {
					r.setThickness(null);
					r.setFullTag(tagName);
				} else {
					Long thicknessCode = tagRecommend.get(1).get(i);
					Thickness thickness = switch (thicknessCode.intValue()) {
						case -1 -> Thickness.THIN;
						case 1 ->  Thickness.THICK;
						default -> Thickness.NORMAL;
					};
					r.setThickness(thickness);
					r.setFullTag(String.join(" ", thickness.getName(), tagName));
				}
				recommendElement.add(r);
			}
			recommend.add(recommendElement);
		}
		return recommend;
	}
	public List<OotdShortDTO> getOotdPreview(List<Long> memberIdList) {
		log.trace("ai.getSimilarUser end");

		// 해당 memberId 목록에 속하는 ootd를 최신 3개만 획득
		List<OOTD> ootdList = ootdRepository.findTop3ByMember_IdInOrderByIdDesc(memberIdList);
		List<OotdShortDTO> ootdPreview = new ArrayList<>();
		for (OOTD o : ootdList) {

			OotdShortDTO ootd = new OotdShortDTO();
			ootd.setImageURL(o.getImageURL());
			ootd.setDate(((o.getDepartTime()+32400)/86400)*86400-32400);
			ootdPreview.add(ootd);
		}
		return ootdPreview;
	}
	public List<OotdShortDTO> getOotdPreview() {
		log.trace("ai.getSimilarUser end");

		// 전체 ootd를 최신 3개만 획득
		List<OOTD> ootdList = ootdRepository.findTop3ByOrderByIdDesc();
		List<OotdShortDTO> ootdPreview = new ArrayList<>();
		for (OOTD o : ootdList) {

			OotdShortDTO ootd = new OotdShortDTO();
			ootd.setImageURL(o.getImageURL());
			ootd.setDate(((o.getDepartTime()+32400)/86400)*86400-32400);
			ootdPreview.add(ootd);
		}
		return ootdPreview;
	}

	public SearchTagDTO.res getSearchTag(Thickness thickness, Long tagId) {

		// 현재 사용자 조회
		Member member = memberRepository.findByName(SecurityContextHolder.getContext().getAuthentication().getName());

		// 카테고리에 따라 분기 처리
		Tag tag = tagRepository.findById(tagId).get();

		SearchTagDTO.res res = new SearchTagDTO.res();

		List<Clothes> clothesList;
		if (Arrays.asList(Category.SHOE, Category.ACC).contains(tag.getCategory())) {

			clothesList = clothesRepository.findByFullTag(member, tag);
			res.setCoupangURL("https://www.coupang.com/np/search?q=" + tag.getName());
		} else {

			clothesList = clothesRepository.findByFullTag(member, thickness, tag);
			String query = String.join(" ", thickness.getName(), tag.getName());
			res.setCoupangURL("https://www.coupang.com/np/search?q=" + query);
		}

		res.setClothesShortList(new ArrayList<>());
		for (Clothes c : clothesList) {

			res.getClothesShortList().add(new ClothesShortDTO(c.getId(), c.getName(), c.getImageURL()));
		}

		return res;
	}

	public AllDTO.res getAll(AllDTO.req req) {

		// 현재 사용자 조회
		Member member = memberRepository.findByName(SecurityContextHolder.getContext().getAuthentication().getName());

		// 페이지 확인
		long reqLastPage = req.getLastPage();

		// 사용자의 clothes 18개 조회
		List<ClothesDTO> clothesList;
		String reqCategory = req.getCategory();
		if (reqCategory != null) {

			// 카테고리가 요청된 경우
			Category category;
			try {
				category = Category.valueOfLower(reqCategory);
			} catch (Exception e) {
				throw new CustomException(ResponseCode.COM4000);
			}

			if (reqLastPage == -1) {
				clothesList = clothesRepository.pageAllClothes(member, category);
			} else {
				clothesList = clothesRepository.pageAllClothes(member, category, reqLastPage);
			}
		} else {

			// 전체 요청된 경우
			if (reqLastPage == -1) {
				clothesList = clothesRepository.pageAllClothes(member);
			} else {
				clothesList = clothesRepository.pageAllClothes(member, reqLastPage);
			}
		}

		// 마지막에 조회된 clothes id
		Long lastPage;
		if (clothesList.size() < 18) {
			lastPage = -2L;
		} else {
			lastPage = clothesList.get(17).getClothesId();
		}

		// 응답 정의
		AllDTO.res res = new AllDTO.res();
		res.setLastPage(lastPage);
		res.setClothesList(clothesList);

		Thickness.class.getEnumConstants();

		return res;
	}

	public SearchNameDTO.res getSearch(Category category, String clothesName) {

		// 현재 사용자 조회
		Member member = memberRepository.findByName(SecurityContextHolder.getContext().getAuthentication().getName());

		// 전체 카테고리에 대한 조회인지 검사 및 분기 처리
		SearchNameDTO.res res = new SearchNameDTO.res();
		List<ClothesDTO> clothesList;
		if (category == null) {
			clothesList = clothesRepository.findBySearch(member, clothesName);
		} else {
			clothesList = clothesRepository.findBySearch(member, category, clothesName);
		}
		res.setClothesList(clothesList);

		return res;
	}

	public TagDTO getTag() {

		TagDTO res = new TagDTO();
		res.setTop(tagRepository.findTop());
		res.setBottom(tagRepository.findBottom());
		res.setOuter(tagRepository.findOuter());
		res.setShoe(tagRepository.findShoe());
		res.setAcc(tagRepository.findAcc());

		return res;
	}

	public RootDTO.res postRoot(RootDTO.req req) {

		// 현재 사용자 조회
		Member member = memberRepository.findByName(SecurityContextHolder.getContext().getAuthentication().getName());

		// clothes 엔티티 생성
		Clothes clothes = new Clothes();
		clothes.setMember(member);
		clothes.setName(req.getName());

		Tag tag = tagRepository.findById(req.getTagId()).get();
		clothes.setTag(tag);

		// thickness를 필수로 받는 카테고리라면, thickness가 정상적으로 요청되었는지 검사
		Thickness thickness = null;
		if (!Arrays.asList(Category.SHOE, Category.ACC).contains(tag.getCategory())) {
			if (req.getThickness() == null) {
				throw new CustomException(ResponseCode.COM4000, "thickness가 필요한 카테고리입니다.");
			} else {
				thickness = Thickness.valueOfLower(req.getThickness());
			}
		}
		clothes.setThickness(thickness);

		// 이미지 처리
		MultipartFile image = req.getImage();

		if (image == null || image.isEmpty()) {

			clothes.setImageURL(null);
		}
		else {

			String filename = "/clothes/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
			Path savePath = Paths.get(resourcesPath+filename);

			try {
				image.transferTo(savePath);
				clothes.setImageURL(filename);
			}
			catch (Exception e) {
				throw new CustomException(ResponseCode.COM4150);
			}
		}

		// 응답 정의
		RootDTO.res res = new RootDTO.res();
		res.setClothesId(clothesRepository.save(clothes).getId());

		return res;
	}

	public RootDTO.res putRoot(Long clothesId, RootDTO.putReq req) {

		// 현재 사용자 조회
		Member member = memberRepository.findByName(SecurityContextHolder.getContext().getAuthentication().getName());

		if (!clothesRepository.existsByIdAndMember(clothesId, member)) {
			throw new CustomException(ResponseCode.COM4010, "요청된 자원에 접근할 수 없는 계정입니다: " + member.getName());
		}

		// 요청된 옷 엔티티 획득
		Clothes clothes = clothesRepository.findById(clothesId).get();
		if (clothes.getIsDeleted()) throw new CustomException(ResponseCode.COM4091);

		// clothes 엔티티 수정
		clothes.setMember(member);
		clothes.setName(req.getName());

		Tag tag = tagRepository.findById(req.getTagId()).get();
		clothes.setTag(tag);

		// thickness를 필수로 받는 카테고리라면, thickness가 정상적으로 요청되었는지 검사
		Thickness thickness = null;
		if (!Arrays.asList(Category.SHOE, Category.ACC).contains(tag.getCategory())) {
			if (req.getThickness() == null) {
				throw new CustomException(ResponseCode.COM4000, "thickness가 필요한 카테고리입니다.");
			} else {
				thickness = Thickness.valueOfLower(req.getThickness());
			}
		}
		clothes.setThickness(thickness);

		// 이미지 처리
		MultipartFile image = req.getImage();

		// 기존에 존재하던 이미지 파일은 삭제(확장자가 다를 수도 있으니 보내주는 파일명을 항상 그대로 사용)
		String existingImage = clothes.getImageURL();
		if (existingImage != null) new File(resourcesPath+existingImage).delete();

		if (image == null || image.isEmpty()) {

			clothes.setImageURL(null);
		}
		else {

			String filename = "/clothes/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
			Path savePath = Paths.get(resourcesPath+filename);

			try {
				image.transferTo(savePath);
				clothes.setImageURL(filename);
			}
			catch (Exception e) {
				throw new CustomException(ResponseCode.COM4150);
			}
		}

		clothesRepository.save(clothes);

		// 응답 정의
		RootDTO.res res = new RootDTO.res();
		res.setClothesId(clothes.getId());

		return res;
	}

	public RootDTO.res patchRoot(Long clothesId, RootDTO.patchReq req) {

		// 현재 사용자 조회
		Member member = memberRepository.findByName(SecurityContextHolder.getContext().getAuthentication().getName());

		if (!clothesRepository.existsByIdAndMember(clothesId, member)) {
			throw new CustomException(ResponseCode.COM4010, "요청된 자원에 접근할 수 없는 계정입니다: " + member.getName());
		}

		// 요청된 옷 엔티티 획득
		Clothes clothes = clothesRepository.findById(clothesId).get();
		if (clothes.getIsDeleted()) throw new CustomException(ResponseCode.COM4091);

		// clothes 엔티티 수정
		clothes.setMember(member);
		clothes.setName(req.getName());

		Tag tag = tagRepository.findById(req.getTagId()).get();
		clothes.setTag(tag);

		// thickness를 필수로 받는 카테고리라면, thickness가 정상적으로 요청되었는지 검사
		Thickness thickness = null;
		if (!Arrays.asList(Category.SHOE, Category.ACC).contains(tag.getCategory())) {
			if (req.getThickness() == null) {
				throw new CustomException(ResponseCode.COM4000, "thickness가 필요한 카테고리입니다.");
			} else {
				thickness = Thickness.valueOfLower(req.getThickness());
			}
		}
		clothes.setThickness(thickness);

		clothesRepository.save(clothes);

		// 응답 정의
		RootDTO.res res = new RootDTO.res();
		res.setClothesId(clothes.getId());

		return res;
	}

	public void deleteRoot(Long clothesId) {

		// 현재 사용자 조회
		Member member = memberRepository.findByName(SecurityContextHolder.getContext().getAuthentication().getName());

		if (!clothesRepository.existsByIdAndMember(clothesId, member)) {
			throw new CustomException(ResponseCode.COM4010, "요청된 자원에 접근할 수 없는 계정입니다: " + member.getName());
		}

		// 요청된 옷 엔티티 획득
		Clothes clothes = clothesRepository.findById(clothesId).get();

		// 삭제할 시 연결된 coordi 데이터에 영향이 갈 수 있으므로, isDeleted 값만 변경
		clothes.setIsDeleted(true);
	}
}
