package com.elorankingbot.backend.command;

import com.elorankingbot.backend.command_legacy.ChallengeAsUserInteraction;
import com.elorankingbot.backend.commands.*;
import com.elorankingbot.backend.commands.admin.SetPermission;
import com.elorankingbot.backend.commands.admin.settings.SetVariable;
import com.elorankingbot.backend.commands.player.Help;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.rest.http.client.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Hooks;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class EventParser {

	private final Services services;
	private final DBService dbService;
	private final DiscordBotService bot;
	private final CommandClassScanner commandClassScanner;

	public EventParser(Services services, CommandClassScanner commandClassScanner) {
		this.services = services;
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.commandClassScanner = commandClassScanner;
		GatewayDiscordClient client = services.client;

		client.on(ReadyEvent.class)
				.subscribe(event -> {
					User self = event.getSelf();
					log.info("Logged in as {}#{}", self.getUsername(), self.getDiscriminator());
					String activityMessage = services.props.getActivityMessage();
					client.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.playing(activityMessage))).subscribe();
					logGlobalCommands();
				});

		client.on(ChatInputInteractionEvent.class)
				.subscribe(this::createAndExecuteSlashCommand);

		client.on(ButtonInteractionEvent.class)
				.subscribe(this::processButtonInteractionEvent);

		client.on(SelectMenuInteractionEvent.class)
				.subscribe(event -> {// TODO zusammen mit Help umbauen
					if (event.getCustomId().startsWith(Help.customId)) {
						Help.executeSelectMenuSelection(services, event);
					} else {
						processSelectMenuInteractionEvent(event);
					}
				});

		client.on(ModalSubmitInteractionEvent.class)
				.subscribe(event -> new SetVariable(event, services).doExecute());

		client.on(MessageInteractionEvent.class)
				.subscribe(this::processMessageInteractionEvent);

		client.on(GuildCreateEvent.class)
				.subscribe(event -> {
					Optional<Server> maybeServer = dbService.findServerByGuildId(event.getGuild().getId().asLong());
					if (maybeServer.isEmpty()) {
						Server server = new Server(event.getGuild().getId().asLong());
						dbService.saveServer(server);
						//long everyoneRoleId = server.getGuildId();
						//bot.setCommandPermissionForRole(server, SetPermissions.getRequest().name(), everyoneRoleId);
					} else {
						maybeServer.get().setMarkedForDeletion(false);
						dbService.saveServer(maybeServer.get());
					}
				});

		client.on(RoleDeleteEvent.class)
				.subscribe(event -> {
					Server server = dbService.findServerByGuildId(event.getGuildId().asLong()).get();
					if (server.getAdminRoleId() == event.getRoleId().asLong()) {
						long everyoneRoleId = server.getGuildId();
						bot.setCommandPermissionForRole(server, SetPermission.class.getSimpleName().toLowerCase(), everyoneRoleId);
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
		try {
			Command command = createSlashCommand(event);
			command.doExecute();
		} catch (Exception e) {
			handleException(e, event, event.getCommandName());
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
			handleException(e, event, event.getCustomId().split(":")[0]);
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
			handleException(e, event, event.getCustomId().split(":")[0]);
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
			handleException(e, event, event.getCommandName().replace(" ", "").toLowerCase());
		}
	}

	private MessageCommand createMessageCommand(MessageInteractionEvent event) throws Exception {
		String commandFullClassName = commandClassScanner.getFullClassName(event.getCommandName().replace(" ", "").toLowerCase());
		if (commandFullClassName == null) throw new RuntimeException("Unknown Command");
		return (MessageCommand) Class.forName(commandFullClassName)
				.getConstructor(MessageInteractionEvent.class, Services.class)
				.newInstance(event, services);
	}

	private void handleException(Throwable throwable, DeferrableInteractionEvent event, String commandName) {
		String guildName = event.getInteraction().getGuild().map(Guild::getName).onErrorReturn("unknown").block();
		String errorReport = String.format("Error executing %s on %s by %s:\n%s", commandName,
				guildName, event.getInteraction().getUser().getTag(), throwable.getMessage());
		log.error(errorReport);
		bot.sendToOwner(errorReport);
		if (throwable instanceof ClientException) {
			log.error("ClientException caused by request:\n" + ((ClientException) throwable).getRequest());
		}
		throwable.printStackTrace();

		String userErrorMessage = "Error: " + throwable.getMessage() + "\nI sent a report to the developer.";
		try {
			event.reply(userErrorMessage).block();
		} catch (ClientException e) {
			event.createFollowup(userErrorMessage).subscribe();
		}
	}

	private void logGlobalCommands() {
		List<ApplicationCommandData> globalCommands = bot.getAllGlobalCommands().block();
		log.info("Global Commands:");
		for (ApplicationCommandData globalCommand : globalCommands) {
			log.info(globalCommand.name());
		}
	}
}
