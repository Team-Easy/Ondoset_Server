package com.ondoset.dto.admin.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ModelDTO {
    private Long modelId;
    private Double version;
    private Long date;
}