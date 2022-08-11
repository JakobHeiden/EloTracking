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
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.rest.http.client.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Hooks;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class EventParser {

	private final Services services;
	private final DBService dbService;
	private final DiscordBotService bot;
	private final CommandClassScanner commandClassScanner;
	private final Function<SelectMenuInteractionEvent, SelectMenuCommand> selectMenuCommandFactory;
	private final Function<ButtonInteractionEvent, ButtonCommand> buttonCommandFactory;
	private final Function<MessageInteractionEvent, MessageCommand> messageCommandFactory;

	public EventParser(Services services,
					   Function<ButtonInteractionEvent, ButtonCommand> buttonCommandFactory,
					   Function<MessageInteractionEvent, MessageCommand> messageCommandFactory,
					   Function<UserInteractionEvent, ChallengeAsUserInteraction> userInteractionChallengeFactory,
					   CommandClassScanner commandClassScanner, Function<SelectMenuInteractionEvent, SelectMenuCommand> selectMenuCommandFactory) {
		this.services = services;
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.buttonCommandFactory = buttonCommandFactory;
		this.messageCommandFactory = messageCommandFactory;
		this.commandClassScanner = commandClassScanner;
		this.selectMenuCommandFactory = selectMenuCommandFactory;
		GatewayDiscordClient client = services.client;

		client.on(ReadyEvent.class)
				.subscribe(event -> {
					User self = event.getSelf();
					log.info("Logged in as {}#{}", self.getUsername(), self.getDiscriminator());
					String activityMessage = services.props.getActivityMessage();
					client.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.playing(activityMessage))).subscribe();
				});

		client.on(ChatInputInteractionEvent.class)
				.subscribe(this::createAndExecuteSlashCommand);

		client.on(ButtonInteractionEvent.class)
				.doOnNext(this::createAndExecuteButtonCommand)
				.onErrorContinue(this::handleException)
				.subscribe();

		client.on(SelectMenuInteractionEvent.class)
				.subscribe(event -> {// TODO zusammen mit Help umbauen
					if (event.getCustomId().startsWith(Help.customId)) {
						Help.executeSelectMenuSelection(services, event);
					} else {
						try {
							createAndExecuteSelectMenuCommand(event);
						} catch (Exception e) {
							handleException(e, event);
						}
					}
				});

		client.on(ModalSubmitInteractionEvent.class)
				// this will not create a Spring Bean and consequently will not trigger AOP logging...
				.subscribe(event -> new SetVariable(event, services).doExecute());

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
			bot.logCommand(command);
			command.doExecute();
		} catch (Exception e) {
			handleException(e, event);	// TODO!
		}
	}

	@Transactional
	void createAndExecuteSelectMenuCommand(SelectMenuInteractionEvent event) {
		SelectMenuCommand command = selectMenuCommandFactory.apply(event);
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

	private void handleException(Throwable throwable, Object event) {// TODO schauen ob event.reply vllt verallgemeinert werden muss
		// bei ner exception kommt es evtl zu mehreren event.reply sonst
		if (throwable instanceof ClientException) {
			log.error(((ClientException) throwable).getRequest().toString());
		}
		String errorMessage = String.format("Error in EventParser: %s - " +
				"Occured during %s", throwable.toString(), bot.getLatestCommandLog());
		bot.sendToOwner(errorMessage);
		log.error(errorMessage);
		throwable.printStackTrace();
		log.error(Arrays.toString(throwable.getStackTrace()));// TODO auf dem server den error stream nach out umbiegen
	}

	public SlashCommand createSlashCommand(ChatInputInteractionEvent event) throws Exception {
		String commandClassName = commandClassScanner.getFullClassName(event.getCommandName());
		if (commandClassName == null) throw new RuntimeException("Unknown Command");
		return (SlashCommand) Class.forName(commandClassName)
				.getConstructor(ChatInputInteractionEvent.class, Services.class)
				.newInstance(event, services);
	}

	public SelectMenuCommand createSelectMenuCommand(SelectMenuInteractionEvent event) {
		String commandClassName = mapCommandNameToFullClassName(event.getCustomId().split(":")[0]);
		try {
			return (SelectMenuCommand) Class.forName(commandClassName)
					.getConstructor(SelectMenuInteractionEvent.class, Services.class)
					.newInstance(event, services);
		} catch (Exception e) {
			String errorMessage = String.format("exception creating SelectMenuCommand %s on %s by %s",
					commandClassName, event.getInteraction().getGuild().block().getName(), event.getInteraction().getUser().getTag());
			bot.sendToOwner(errorMessage);
			System.out.println(errorMessage);
			e.printStackTrace();
			return null;
		}
	}

	public ButtonCommand createButtonCommand(ButtonInteractionEvent event) {
		String commandClassName = mapCommandNameToFullClassName(event.getCustomId().split(":")[0]);
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
		String commandClassName = mapCommandNameToFullClassName(event.getCommandName().replace(" ", "").toLowerCase());
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

	private String mapCommandNameToFullClassName(String className) {
		String fullClassName = commandClassScanner.getFullClassName(className);
		if (fullClassName == null) {
			String errorMessage = "Error mapping command name to full class name: " + className;
			log.error(errorMessage);
			bot.sendToOwner(errorMessage);
		}
		return fullClassName;
	}

	// TODO weg
	public ChallengeAsUserInteraction createUserInteractionChallenge(UserInteractionEvent event) {
		return new ChallengeAsUserInteraction(event, services);
	}
}
