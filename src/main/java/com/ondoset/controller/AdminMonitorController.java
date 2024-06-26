package com.ondoset.controller;

import com.ondoset.controller.advice.ResponseCode;
import com.ondoset.controller.advice.ResponseMessage;
import com.ondoset.dto.admin.monitor.ActiveUserDTO;
import com.ondoset.dto.admin.monitor.LogDTO;
import com.ondoset.dto.admin.monitor.RecordingPathDTO;
import com.ondoset.service.AdminMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/monitor")
public class AdminMonitorController {

	private final AdminMonitorService adminMonitorService;

	@GetMapping("/ai")
	public ResponseEntity<ResponseMessage<String>> getAi() {

		try {
			adminMonitorService.getAi();
			return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, "Normal"));
		} catch (Exception e) {
			return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, "Error"));
		}
	}

	@GetMapping("/db")
	public ResponseEntity<ResponseMessage<String>> getDb() {

		try {
			adminMonitorService.getDb();
			return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, "Normal"));
		} catch (Exception e) {
			return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, "Error"));
		}
	}

	@GetMapping("/weather")
	public ResponseEntity<ResponseMessage<String>> getWeather() {

		try {
			String status = adminMonitorService.getWeather();
			return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, status));
		} catch (Exception e) {
			return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, "Error"));
		}
	}

	@GetMapping("/main")
	public ResponseEntity<ResponseMessage<List<LogDTO>>> getMain() {

		List<LogDTO> res = adminMonitorService.getMain();

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@GetMapping("/recording-path")
	public ResponseEntity<ResponseMessage<RecordingPathDTO>> getRecordingPath() {

		RecordingPathDTO res = adminMonitorService.getRecordingPath();

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}

	@GetMapping("/active-user")
	public ResponseEntity<ResponseMessage<List<ActiveUserDTO>>> getActiveUser() {

		List<ActiveUserDTO> res = adminMonitorService.getActiveUser();

		return ResponseEntity.ok(new ResponseMessage<>(ResponseCode.COM2000, res));
	}
}
