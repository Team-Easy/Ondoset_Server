package com.ondoset.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ondoset.common.Kma;
import com.ondoset.controller.advice.CustomException;
import com.ondoset.dto.admin.monitor.ActiveUserDTO;
import com.ondoset.dto.kma.PastWDTO;
import com.ondoset.repository.MemberRepository;
import com.ondoset.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Log4j2
@RequiredArgsConstructor
@Service
public class AdminMonitorService {

	private final TagRepository tagRepository;
	private final MemberRepository memberRepository;
	private final Kma kma;
	@Value("${com.ondoset.data.service_key}")
	private String serviceKey;

	public void getDb() throws Exception {

		tagRepository.findById(1L).get();
	}

	public String getWeather() throws Exception {

		// 어제 날씨가 제대로 조회 되는지 확인
		String reqTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMddHH"));

		URL url = new URL(String.format("https://apis.data.go.kr/1360000/AsosHourlyInfoService/getWthrDataList" +
						"?numOfRows=1&pageNo=1&dataType=JSON&dataCd=ASOS&dateCd=HR&startDt=%s&startHh=%s00&endDt=%1$s&endHh=%2$s&stnIds=108&serviceKey=%s",
				reqTime.substring(0,8), reqTime.substring(8,10), serviceKey));

		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", "application/json");

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String response = in.readLine();

		in.close();
		con.disconnect();

		JsonObject result = JsonParser.parseString(response).getAsJsonObject();
		String kmaErrorCode = result.getAsJsonObject("response").getAsJsonObject("header").get("resultCode").toString().replace("\"", "");
		if (!kmaErrorCode.equals("00")) return "Maintenance";
		return "Normal";
	}

	public List<ActiveUserDTO> getActiveUser() {

		DateTimeFormatter sqlFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(TimeZone.getDefault().toZoneId());

		// 서비스와 관련된 테이블에 대하여 최근 업데이트를 발생시킨 멤버 수를 달별로 획득;
		LocalDateTime dateTime = LocalDateTime.now();

		List<ActiveUserDTO> res = new ArrayList<>();
		for (long i = 0; i < 12; i++) {

			Long period = dateTime.minusMonths(i-1).toLocalDate().toEpochDay() * 86400 - 32400;

			String startDate = dateTime.minusMonths(i-1).format(sqlFormatter);
			String endDate = dateTime.minusMonths(i).format(sqlFormatter);
			Long count = memberRepository.countActiveMember(startDate, endDate);

			res.add(new ActiveUserDTO(period, count));
		}

		return res;
	}
}