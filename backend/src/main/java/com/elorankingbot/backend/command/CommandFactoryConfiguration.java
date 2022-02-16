package com.elorankingbot.backend.command;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.commands.player.ChallengeAsUserInteraction;
import com.elorankingbot.backend.tools.ButtonInteractionEventWrapper;
import com.elorankingbot.backend.tools.ChatInputInteractionEventWrapper;
import com.elorankingbot.backend.tools.UserInteractionEventWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;

@Slf4j
@Configuration
public class CommandFactoryConfiguration {

	private final EventParser eventParser;

	public CommandFactoryConfiguration(@Lazy EventParser eventParser) {
		this.eventParser = eventParser;
	}

	@Bean
	public Function<ChatInputInteractionEventWrapper, SlashCommand> slashCommandFactory() {
		return eventWrapper -> createSlashCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public SlashCommand createSlashCommand(ChatInputInteractionEventWrapper eventWrapper) {
		return eventParser.createSlashCommand(eventWrapper);
	}

	@Bean
	public Function<ButtonInteractionEventWrapper, ButtonCommand> buttonCommandFactory() {
		return eventWrapper -> createButtonCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public ButtonCommand createButtonCommand(ButtonInteractionEventWrapper eventWrapper) {
		return eventParser.createButtonCommand(eventWrapper);
	}

	@Bean
	public Function<UserInteractionEventWrapper, ChallengeAsUserInteraction> userInteractionChallengeFactory() {
		return eventWrapper -> createUserInteractionChallenge(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public ChallengeAsUserInteraction createUserInteractionChallenge(UserInteractionEventWrapper eventWrapper) {
		return eventParser.createUserInteractionChallenge(eventWrapper);
	}
}
