package com.elorankingbot.backend.command;

import com.elorankingbot.backend.ExceptionHandler;
import com.elorankingbot.backend.commands.*;
import com.elorankingbot.backend.commands.admin.SetPermission;
import com.elorankingbot.backend.commands.admin.settings.SetVariable;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.DiscordCommandService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.discordjson.json.ApplicationCommandData;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Hooks;

import java.util.List;

@CommonsLog
@Component
public class EventParser {

	private final Services services;
	private final DBService dbService;
	private final DiscordBotService bot;
	private final ExceptionHandler exceptionHandler;
	private final DiscordCommandService discordCommandService;
	private final CommandClassScanner commandClassScanner;

	public EventParser(Services services, CommandClassScanner commandClassScanner) {
		this.services = services;
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.exceptionHandler = services.exceptionHandler;
		this.discordCommandService = services.discordCommandService;
		this.commandClassScanner = commandClassScanner;
		GatewayDiscordClient client = services.client;

		client.on(ReadyEvent.class)
				.subscribe(event -> {
					User self = event.getSelf();
					log.info(String.format("Logged in as %s", self.getTag()));
					String activityMessage = services.props.getActivityMessage();
					client.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.playing(activityMessage))).subscribe();
					logGlobalCommands();
				});

		client.on(ChatInputInteractionEvent.class)
				.subscribe(this::createAndExecuteSlashCommand);

		client.on(ButtonInteractionEvent.class)
				.subscribe(this::processButtonInteractionEvent);

		client.on(SelectMenuInteractionEvent.class)
				.subscribe(this::processSelectMenuInteractionEvent);

		client.on(ModalSubmitInteractionEvent.class)
				.subscribe(event -> {
					try {
						new SetVariable(event, services).doExecute();
					} catch (Exception e) {
						exceptionHandler.handleUnexpectedCommandException(e, event, SetVariable.class.getSimpleName());
					}
				});

		client.on(MessageInteractionEvent.class)
				.subscribe(this::processMessageInteractionEvent);

		client.on(RoleDeleteEvent.class)
				.subscribe(event -> {
					Server server = dbService.getOrCreateServer(event.getGuildId().asLong());
					if (server.getAdminRoleId() == event.getRoleId().asLong()) {
						long everyoneRoleId = server.getGuildId();
						discordCommandService.setCommandPermissionForRole(server, SetPermission.class.getSimpleName().toLowerCase(), everyoneRoleId);
					}
				});

		client.on(Event.class).subscribe(event -> log.trace(event.getClass().getSimpleName()));

		client.on(InteractionCreateEvent.class).subscribe(event -> {
			String commandString = "unknown";
			if (event.getClass().equals(ButtonInteractionEvent.class))
				commandString = ((ButtonInteractionEvent) event).getCustomId();
			if (event.getClass().equals(ChatInputInteractionEvent.class))
				commandString = ((ChatInputInteractionEvent) event).getCommandName();
			log.debug(String.format("%s : %s : %s",
					event.getClass().getSimpleName(),
					commandString,
					event.getInteraction().getId().asString()));
		});

		Hooks.onErrorDropped(throwable -> exceptionHandler.handleException(throwable, "Dropped Exception"));
	}

	@Transactional
	void createAndExecuteSlashCommand(ChatInputInteractionEvent event) {
		try {
			Command command = createSlashCommand(event);
			command.doExecute();
		} catch (Exception e) {
			exceptionHandler.handleUnexpectedCommandException(e, event, event.getCommandName());
		}
	}

	private SlashCommand createSlashCommand(ChatInputInteractionEvent event) throws Exception {
		String commandFullClassName = commandClassScanner.getFullClassName(event.getCommandName());
		if (commandFullClassName == null) throw new RuntimeException("Unknown Command");
		return (SlashCommand) Class.forName(commandFullClassName)
				.getConstructor(ChatInputInteractionEvent.class, Services.class)
				.newInstance(event, services);
	}

	@Transactional
	void processButtonInteractionEvent(ButtonInteractionEvent event) {
		try {
			Command command = createButtonCommand(event);
			command.doExecute();
		} catch (Exception e) {
			exceptionHandler.handleUnexpectedCommandException(e, event, event.getCustomId().split(":")[0]);
		}
	}

	private ButtonCommand createButtonCommand(ButtonInteractionEvent event) throws Exception {
		String commandFullClassName = commandClassScanner.getFullClassName(event.getCustomId().split(":")[0]);
		if (commandFullClassName == null) throw new RuntimeException("Unknown Command");
		return (ButtonCommand) Class.forName(commandFullClassName)
				.getConstructor(ButtonInteractionEvent.class, Services.class)
				.newInstance(event, services);
	}

	@Transactional
	void processSelectMenuInteractionEvent(SelectMenuInteractionEvent event) {
		try {
			Command command = createSelectMenuCommand(event);
			command.doExecute();
		} catch (Exception e) {
			exceptionHandler.handleUnexpectedCommandException(e, event, event.getCustomId().split(":")[0]);
		}
	}

	private SelectMenuCommand createSelectMenuCommand(SelectMenuInteractionEvent event) throws Exception {
		String commandFullClassName = commandClassScanner.getFullClassName(event.getCustomId().split(":")[0]);
		if (commandFullClassName == null) throw new RuntimeException("Unknown Command");
		return (SelectMenuCommand) Class.forName(commandFullClassName)
				.getConstructor(SelectMenuInteractionEvent.class, Services.class)
				.newInstance(event, services);
	}

	@Transactional
	void processMessageInteractionEvent(MessageInteractionEvent event) {
		try {
			Command command = createMessageCommand(event);
			command.doExecute();
		} catch (Exception e) {
			exceptionHandler.handleUnexpectedCommandException(e, event, event.getCommandName().replace(" ", "").toLowerCase());
		}
	}

	private MessageCommand createMessageCommand(MessageInteractionEvent event) throws Exception {
		String commandFullClassName = commandClassScanner.getFullClassName(event.getCommandName().replace(" ", "").toLowerCase());
		if (commandFullClassName == null) throw new RuntimeException("Unknown Command");
		return (MessageCommand) Class.forName(commandFullClassName)
				.getConstructor(MessageInteractionEvent.class, Services.class)
				.newInstance(event, services);
	}

	private void logGlobalCommands() {
		List<ApplicationCommandData> globalCommands = discordCommandService.getAllGlobalCommands().block();
		log.info("Global Commands: " + String.join(", ", globalCommands.stream().map(ApplicationCommandData::name).toList()));
	}
}
