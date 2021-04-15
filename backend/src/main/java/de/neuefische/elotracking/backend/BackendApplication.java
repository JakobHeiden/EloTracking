package de.neuefische.elotracking.backend;

import de.neuefische.elotracking.backend.discord.DiscordBotConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
	    SpringApplication.run(BackendApplication.class, args);
	}
}
