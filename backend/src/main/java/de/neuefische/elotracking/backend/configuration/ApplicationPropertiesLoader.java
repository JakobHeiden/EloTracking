package de.neuefische.elotracking.backend.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "elotracking")
public class ApplicationPropertiesLoader {

	private String adminId;
	private String defaultCommandPrefix;
	private String baseUrl;
	private int numberOfTimeSlots;
}