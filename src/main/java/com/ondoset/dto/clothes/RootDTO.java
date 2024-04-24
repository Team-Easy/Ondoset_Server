package com.ondoset.dto.clothes;

import com.ondoset.common.Enum;
import com.ondoset.domain.Enum.Thickness;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public class RootDTO {

	@Getter
	@Setter
	public static class req {

		@NotEmpty
		private String name;
		@NotNull
		private Long tagId;
		@Enum(enumClass = Thickness.class)
		private String thickness;
		private MultipartFile image;
		@NotNull
		private Boolean imageUpdated;
	}

	@Getter
	@Setter
	public static class res {

		private Long clothesId;
	}
}
