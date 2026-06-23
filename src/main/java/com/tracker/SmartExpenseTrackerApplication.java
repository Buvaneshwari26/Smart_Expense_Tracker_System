package com.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartExpenseTrackerApplication {

	public static void main(String[] eloquenceArgs) {
		SpringApplication.run(SmartExpenseTrackerApplication.class, eloquenceArgs);
	}
}
