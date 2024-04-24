package com.ondoset.controller;

import com.ondoset.controller.advice.ResponseCode;
import com.ondoset.controller.advice.ResponseMessage;
import com.ondoset.dto.kma.PastWDTO;
import com.ondoset.dto.ootd.*;
import com.ondoset.service.OOTDService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/ootd")
public class OOTDController {

	private final OOTDService ootdService;

	@GetMapping("/my-profile")
	public ResponseEntity<ResponseMessage<MyProfileDTO>> getMyProfile() {

		MyProfileDTO res = ootdService.getMyProfile();

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@GetMapping("/my-profile/page")
	public ResponseEntity<ResponseMessage<MyProfilePageDTO>> getMyProfilePage(PageDTO req) {

		MyProfilePageDTO res = ootdService.getMyProfilePage(req);

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@GetMapping("/weather")
	public ResponseEntity<ResponseMessage<OotdPageDTO>> getWeather(@Valid WeatherDTO req) {

		OotdPageDTO res = ootdService.getWeather(req);

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@GetMapping("/like-list")
	public ResponseEntity<ResponseMessage<OotdPageDTO>> getLikeList(PageDTO req) {

		OotdPageDTO res = ootdService.getLikeList(req);

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@GetMapping("/follow-list")
	public ResponseEntity<ResponseMessage<FollowingPageDTO>> getFollowList(FollowingSearchDTO req) {

		FollowingPageDTO res = ootdService.getFollowList(req);

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@GetMapping("/ban-period")
	public ResponseEntity<ResponseMessage<BanPeriodDTO>> getBanPeriod() {

		BanPeriodDTO res = ootdService.getBanPeriod();

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@GetMapping("/weather-preview")
	public ResponseEntity<ResponseMessage<PastWDTO>> getWeatherPreview(@Valid WeatherPreviewDTO req) {

		PastWDTO res = ootdService.getWeatherPreview(req);

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@PostMapping({"/", ""})
	public ResponseEntity<ResponseMessage<RootDTO.res>> postRoot(@Valid RootDTO.req req) {

		RootDTO.res res = ootdService.postRoot(req);

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@PutMapping("/{ootdId}")
	public ResponseEntity<ResponseMessage<RootDTO.res>> putRoot(@PathVariable("ootdId") Long ootdId, @Valid RootDTO.putReq req) {

		RootDTO.res res = ootdService.putRoot(ootdId, req);

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@DeleteMapping("/{ootdId}")
	public ResponseEntity<ResponseMessage<String>> deleteRoot(@PathVariable("ootdId") Long ootdId) {

		ootdService.deleteRoot(ootdId);

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, "삭제 성공"));
	}
}
