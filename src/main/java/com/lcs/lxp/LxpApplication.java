package com.lcs.lxp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LxpApplication {

	public static void main(String[] args) {
		SpringApplication.run(LxpApplication.class, args);
	}

}
