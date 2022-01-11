package com.elorankingbot.backend;

import com.elorankingbot.backend.logging.LogFileTools;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		LogFileTools.archiveOldLogFile();
		SpringApplication.run(BackendApplication.class, args);
	}
}
