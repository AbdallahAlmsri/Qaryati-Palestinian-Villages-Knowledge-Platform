package com.qaryati.qaryati;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class QaryatiApplication {

	public static void main(String[] args) {
		SpringApplication.run(QaryatiApplication.class, args);
	}

}
