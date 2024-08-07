package com.ondoset.config;

import jakarta.persistence.*;
import lombok.Getter;

import java.sql.Clob;
import java.util.Date;

@Getter
@Entity
@Table(name="log")
public class LogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "log_id", columnDefinition = "int unsigned")
	private Long id;

	private Date date;

	@Column(length = 5)
	private String level;

	private String location;

	@Column(length = 15)
	private String user;

	@Lob
	@Column(columnDefinition = "text")
	private String msg;
}
