package com.elorankingbot.backend.command;

import com.elorankingbot.backend.command_legacy.ChallengeAsUserInteraction;
import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
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
	public Function<ChatInputInteractionEvent, SlashCommand> slashCommandFactory() {
		return event -> createSlashCommand(event);
	}

	@Bean
	@Scope("prototype")
	public SlashCommand createSlashCommand(ChatInputInteractionEvent event) {
		return eventParser.createSlashCommand(event);
	}

	@Bean
	public Function<ButtonInteractionEvent, ButtonCommand> buttonCommandFactory() {
		return event -> createButtonCommand(event);
	}

	@Bean
	@Scope("prototype")
	public ButtonCommand createButtonCommand(ButtonInteractionEvent event) {
		return eventParser.createButtonCommand(event);
	}

	@Bean
	public Function<UserInteractionEvent, ChallengeAsUserInteraction> userInteractionChallengeFactory() {
		return event -> createUserInteractionChallenge(event);
	}

	@Bean
	@Scope("prototype")
	public ChallengeAsUserInteraction createUserInteractionChallenge(UserInteractionEvent event) {
		return eventParser.createUserInteractionChallenge(event);
	}
}
