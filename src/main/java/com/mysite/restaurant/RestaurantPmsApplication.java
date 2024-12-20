package com.mysite.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RestaurantPmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestaurantPmsApplication.class, args);
	}

}
