package de.neuefische.elotracking.backend.logging;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ExceptionHandling {

	@Autowired
	DiscordBotService service;
	@Autowired
	SimpleDateFormat dateFormat;

	@ExceptionHandler(RuntimeException.class)//TODO kann weg
	public void handleThing(RuntimeException e) {
		service.sendToAdmin(dateFormat.format(new Date()) + "\n" + e.getMessage());
	}
}
