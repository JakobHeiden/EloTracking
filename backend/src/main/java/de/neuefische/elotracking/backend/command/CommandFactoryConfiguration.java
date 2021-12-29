package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.commands.ButtonCommand;
import de.neuefische.elotracking.backend.commands.SlashCommand;
import de.neuefische.elotracking.backend.commands.ChallengeAsUserInteraction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;

@Slf4j
@Configuration
public class CommandFactoryConfiguration {

	@Bean
	public Function<ChatInputInteractionEventWrapper, SlashCommand> slashCommandFactory() {
		return eventWrapper -> createSlashCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public SlashCommand createSlashCommand(ChatInputInteractionEventWrapper eventWrapper) {
		return CommandParser.createSlashCommand(eventWrapper);
	}

	@Bean
	public Function<ButtonInteractionEventWrapper, ButtonCommand> buttonCommandFactory() {
		return eventWrapper -> createButtonCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public ButtonCommand createButtonCommand(ButtonInteractionEventWrapper eventWrapper) {
		return CommandParser.createButtonCommand(eventWrapper);
	}

	@Bean
	public Function<UserInteractionEventWrapper, ChallengeAsUserInteraction> userInteractionChallengeFactory() {
		return eventWrapper -> createUserInteractionChallenge(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public ChallengeAsUserInteraction createUserInteractionChallenge(UserInteractionEventWrapper eventWrapper) {
		return CommandParser.createUserInteractionChallenge(eventWrapper);
	}




}
