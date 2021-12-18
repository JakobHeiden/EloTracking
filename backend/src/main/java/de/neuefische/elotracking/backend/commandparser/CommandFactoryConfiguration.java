package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.commands.Command;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;

@Slf4j
@Configuration
public class CommandFactoryConfiguration {

	@Bean
	public Function<ApplicationCommandInteractionEventWrapper, Command> slashCommandFactory() {
		return eventWrapper -> createSlashCommand(eventWrapper);
	}

	@Bean
	public Function<ReactionAddEventWrapper, Command> emojiCommandFactory() {
		return eventWrapper -> createEmojiCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public Command createSlashCommand(ApplicationCommandInteractionEventWrapper eventWrapper) {
		return CommandParser.createSlashCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public Command createEmojiCommand(ReactionAddEventWrapper eventWrapper) {
		return CommandParser.createEmojiCommand(eventWrapper);
	}
}
