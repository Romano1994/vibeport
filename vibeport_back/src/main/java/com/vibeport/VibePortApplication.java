package com.vibeport;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VibePortApplication {

	public static void main(String[] args) {
		SpringApplication.run(VibePortApplication.class, args);
	}

}
