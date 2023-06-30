package com.elorankingbot.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.text.SimpleDateFormat;

@Configuration
@EnableAspectJAutoProxy
public class AopConfig {

	@Bean
	public static SimpleDateFormat getDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
	}
}
