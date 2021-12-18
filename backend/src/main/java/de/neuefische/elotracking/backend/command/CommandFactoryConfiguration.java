package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.commands.Command;
import de.neuefische.elotracking.backend.commands.EmojiCommand;
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
	public Function<ReactionAddEventWrapper, EmojiCommand> emojiCommandFactory() {
		return eventWrapper -> createEmojiCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public Command createSlashCommand(ApplicationCommandInteractionEventWrapper eventWrapper) {
		return CommandParser.createSlashCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public EmojiCommand createEmojiCommand(ReactionAddEventWrapper eventWrapper) {
		return CommandParser.createEmojiCommand(eventWrapper);
	}
}
