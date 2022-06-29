package com.elorankingbot.backend.command;

import com.elorankingbot.backend.command_legacy.ChallengeAsUserInteraction;
import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.commands.MessageCommand;
import com.elorankingbot.backend.commands.SelectMenuCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import discord4j.core.event.domain.interaction.*;
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
		return this::createSlashCommand;
	}

	@Bean
	@Scope("prototype")
	public SlashCommand createSlashCommand(ChatInputInteractionEvent event) {
		return eventParser.createSlashCommand(event);
	}

	@Bean
	public Function<SelectMenuInteractionEvent, SelectMenuCommand> selectMenuCommandFactory() {
		return this::createSelectMenuCommand;
	}

	@Bean
	@Scope("prototype")
	public SelectMenuCommand createSelectMenuCommand(SelectMenuInteractionEvent event) {
		return eventParser.createSelectMenuCommand(event);
	}

	@Bean
	public Function<ButtonInteractionEvent, ButtonCommand> buttonCommandFactory() {
		return this::createButtonCommand;
	}

	@Bean
	@Scope("prototype")
	public ButtonCommand createButtonCommand(ButtonInteractionEvent event) {
		return eventParser.createButtonCommand(event);
	}

	@Bean
	public Function<MessageInteractionEvent, MessageCommand> messageCommandFactory() {
		return this::createMessageCommand;
	}

	@Bean
	@Scope("prototype")
	public MessageCommand createMessageCommand(MessageInteractionEvent event) {
		return eventParser.createMessageCommand(event);
	}


	@Bean
	public Function<UserInteractionEvent, ChallengeAsUserInteraction> userInteractionChallengeFactory() {
		return this::createUserInteractionChallenge;
	}

	@Bean
	@Scope("prototype")
	public ChallengeAsUserInteraction createUserInteractionChallenge(UserInteractionEvent event) {
		return eventParser.createUserInteractionChallenge(event);
	}
}
