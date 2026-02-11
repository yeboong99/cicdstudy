package com.neulboong.forcicd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class ForcicdApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ForcicdApplication.class);
		app.setDefaultProperties(Map.of("spring.profiles.default", "local"));
		app.run(args);
	}

}
