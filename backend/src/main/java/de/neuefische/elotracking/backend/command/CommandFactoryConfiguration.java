package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.commands.ApplicationCommandInteractionCommand;
import de.neuefische.elotracking.backend.commands.ButtonInteractionCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;

@Slf4j
@Configuration
public class CommandFactoryConfiguration {

	@Bean
	public Function<ApplicationCommandInteractionEventWrapper, ApplicationCommandInteractionCommand> slashCommandFactory() {
		return eventWrapper -> createSlashCommand(eventWrapper);
	}

	@Bean
	public Function<ButtonInteractionEventWrapper, ButtonInteractionCommand> emojiCommandFactory() {
		return eventWrapper -> createButtonInteractionCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public ApplicationCommandInteractionCommand createSlashCommand(ApplicationCommandInteractionEventWrapper eventWrapper) {
		return CommandParser.createSlashCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public ButtonInteractionCommand createButtonInteractionCommand(ButtonInteractionEventWrapper eventWrapper) {
		return CommandParser.createButtonInteractionCommand(eventWrapper);
	}
}
