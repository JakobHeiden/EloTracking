package com.elorankingbot.backend.command;

import com.elorankingbot.backend.command_legacy.ChallengeAsUserInteraction;
import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.commands.MessageCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.commands.admin.SetPermissions;
import com.elorankingbot.backend.commands.player.Help;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.rest.http.client.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Hooks;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class EventParser {

	private final Services services;
	private final DBService dbService;
	private final DiscordBotService bot;
	private final CommandClassScanner commandClassScanner;
	private final Function<ChatInputInteractionEvent, SlashCommand> slashCommandFactory;
	private final Function<ButtonInteractionEvent, ButtonCommand> buttonCommandFactory;
	private final Function<MessageInteractionEvent, MessageCommand> messageCommandFactory;

	public EventParser(Services services, CommandClassScanner scanner,
					   Function<ChatInputInteractionEvent, SlashCommand> slashCommandFactory,
					   Function<ButtonInteractionEvent, ButtonCommand> buttonCommandFactory,
					   Function<MessageInteractionEvent, MessageCommand> messageCommandFactory,
					   Function<UserInteractionEvent, ChallengeAsUserInteraction> userInteractionChallengeFactory, CommandClassScanner commandClassScanner) {
		this.services = services;
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.slashCommandFactory = slashCommandFactory;
		this.buttonCommandFactory = buttonCommandFactory;
		this.messageCommandFactory = messageCommandFactory;
		this.commandClassScanner = commandClassScanner;
		GatewayDiscordClient client = services.client;

		client.on(ChatInputInteractionEvent.class)
				.doOnNext(this::createAndExecuteSlashCommand)
				.onErrorContinue(this::handleException)
				.subscribe();

		client.on(ButtonInteractionEvent.class)
				.doOnNext(this::createAndExecuteButtonCommand)
				.onErrorContinue(this::handleException)
				.subscribe();

		client.on(SelectMenuInteractionEvent.class)
				.subscribe(event -> Help.executeSelectMenuSelection(services, event));

		client.on(MessageInteractionEvent.class)
				.doOnNext(this::createAndExecuteMessageCommand)
				.onErrorContinue(this::handleException)
				.subscribe();

		client.on(GuildCreateEvent.class)
				.subscribe(event -> {
					Optional<Server> maybeServer = dbService.findServerByGuildId(event.getGuild().getId().asLong());
					if (maybeServer.isEmpty()) {
						Server server = new Server(event.getGuild().getId().asLong());
						dbService.saveServer(server);
						bot.deployCommand(server, SetPermissions.getRequest()).block();
						//long everyoneRoleId = server.getGuildId();
						//bot.setCommandPermissionForRole(server, SetPermissions.getRequest().name(), everyoneRoleId);
						bot.deployCommand(server, Help.getRequest()).subscribe();
					}
				});

		client.on(RoleDeleteEvent.class)
				.subscribe(event -> {
					Server server = dbService.findServerByGuildId(event.getGuildId().asLong()).get();
					if (server.getAdminRoleId() == event.getRoleId().asLong()) {
						long everyoneRoleId = server.getGuildId();
						bot.setCommandPermissionForRole(server, SetPermissions.class.getSimpleName().toLowerCase(), everyoneRoleId);
					}
				});

		client.on(Event.class).subscribe(event -> log.trace(event.getClass().getSimpleName()));

		Hooks.onErrorDropped(throwable -> {
			throwable.printStackTrace();
			bot.sendToOwner("Hooks::onErrorDropped: " + throwable.getMessage());
		});
	}

	@Transactional
	void createAndExecuteSlashCommand(ChatInputInteractionEvent event) {
		SlashCommand command = slashCommandFactory.apply(event);
		bot.logCommand(command);
		command.doExecute();
	}

	@Transactional
	void createAndExecuteButtonCommand(ButtonInteractionEvent event) {
		ButtonCommand command = buttonCommandFactory.apply(event);
		bot.logCommand(command);
		command.doExecute();
	}

	@Transactional
	void createAndExecuteMessageCommand(MessageInteractionEvent event) {
		MessageCommand command = messageCommandFactory.apply(event);
		bot.logCommand(command);
		command.doExecute();
	}

	private void handleException(Throwable throwable, Object event) {
		if (throwable instanceof ClientException) {
			log.error(((ClientException) throwable).getRequest().toString());
		}
		String errorMessage = String.format("Error in EventParser: %s - " +
				"Occured during %s", throwable.toString(), bot.getLatestCommandLog());
		bot.sendToOwner(errorMessage);
		log.error(errorMessage);
		throwable.printStackTrace();
	}

	public SlashCommand createSlashCommand(ChatInputInteractionEvent event) {
		log.trace("commandName = " + event.getCommandName());
		String commandClassName = commandClassScanner.getFullClassName(event.getCommandName());
		log.trace("commandClassName = " + commandClassName);
		try {
			return (SlashCommand) Class.forName(commandClassName)
					.getConstructor(ChatInputInteractionEvent.class, Services.class)
					.newInstance(event, services);
		} catch (Exception e) {
			bot.sendToOwner("exception occurred while instantiating command " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public ButtonCommand createButtonCommand(ButtonInteractionEvent event) {
		String commandClassName = commandClassScanner.getFullClassName(event.getCustomId().split(":")[0]);
		log.trace("commandClassName = " + commandClassName);
		try {
			return (ButtonCommand) Class.forName(commandClassName)
					.getConstructor(ButtonInteractionEvent.class, Services.class)
					.newInstance(event, services);
		} catch (Exception e) {
			String errorMessage = String.format("exception creating %s on %s by %s",
					commandClassName, event.getInteraction().getGuild().block().getName(), event.getInteraction().getUser().getTag());
			bot.sendToOwner(errorMessage);
			System.out.println(errorMessage);
			e.printStackTrace();
			return null;
		}
	}

	public MessageCommand createMessageCommand(MessageInteractionEvent event) {
		String commandClassName = commandClassScanner.getFullClassName(event.getCommandName().replace(" ", "").toLowerCase());
		log.trace("commandClassName = " + commandClassName);
		try {
			return (MessageCommand) Class.forName(commandClassName)
					.getConstructor(MessageInteractionEvent.class, Services.class)
					.newInstance(event, services);
		} catch (Exception e) {
			String errorMessage = String.format("exception creating %s on %s by %s",
					commandClassName, event.getInteraction().getGuild().block().getName(), event.getInteraction().getUser().getTag());
			bot.sendToOwner(errorMessage);
			System.out.println(errorMessage);
			e.printStackTrace();
			return null;
		}
	}

	// TODO weg
	public ChallengeAsUserInteraction createUserInteractionChallenge(UserInteractionEvent event) {
		return new ChallengeAsUserInteraction(event, services);
	}
}
