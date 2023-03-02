package com.elorankingbot.backend.command;

import com.elorankingbot.backend.logging.ExceptionHandler;
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
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.discordjson.json.ApplicationCommandData;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Hooks;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@CommonsLog
@Component
public class EventParser {

	private final Services services;
	private final DBService dbService;
	// TOKEN
	private final DiscordBotService bot;
	private final ExceptionHandler exceptionHandler;
	private final DiscordCommandService discordCommandService;
	private final CommandClassScanner commandClassScanner;

	public EventParser(Services services, CommandClassScanner commandClassScanner) {
		this.services = services;
		this.dbService = services.dbService;
		// TOKEN
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

		client.on(Event.class).subscribe(event -> log.trace(event.getClass().getSimpleName()));

		Hooks.onErrorDropped(throwable -> exceptionHandler.handleException(throwable, "Dropped Exception"));

		// TOKEN
		client.on(GuildCreateEvent.class).subscribe(guildCreateEvent -> {
			if (bot.isOld()) return;

			Server server = dbService.getOrCreateServer(guildCreateEvent.getGuild().getId().asLong());
			if (server.isOldBot()) {
				log.debug(String.format("setting isOldBot=false on %s:%s", server.getGuildId(), guildCreateEvent.getGuild().getName()));
				server.setOldBot(false);
				dbService.saveServer(server);

				log.debug(String.format("updating commands on %s:%s", server.getGuildId(), guildCreateEvent.getGuild().getName()));
				services.discordCommandService.updateGuildCommandsByRanking(server, commandFailedCallbackFactory(server.getGuildId()));
				services.discordCommandService.updateGuildCommandsByQueue(server, commandFailedCallbackFactory(server.getGuildId()));
			}
		});
	}

	private BiFunction<String, Boolean, Consumer<Throwable>> commandFailedCallbackFactory(long guildId) {
		return (commandName, isDeploy) -> throwable -> log.error(String.format("failed to %s command %s on %s",
				isDeploy ? "deploy" : "delete", commandName, guildId));
	}

	@Transactional
	void createAndExecuteSlashCommand(ChatInputInteractionEvent event) {
		try {
			// TOKEN
			if (services.bot.isOld()) {
				Server server = dbService.getOrCreateServer(event.getInteraction().getGuildId().get().asLong());
				bot.sendToOwner(event.getCommandName() + " : " + server.getGuildId());
				log.debug("Deleting channels for " + server.getGuildId());
				server.getGames().forEach(game -> {
					bot.getChannelById(game.getLeaderboardChannelId()).subscribe(channel -> channel.delete().subscribe());
					bot.getChannelById(game.getResultChannelId()).subscribe(channel -> channel.delete().subscribe());
				});
				bot.deleteChannel(server.getMatchCategoryId());
				bot.deleteChannel(server.getDisputeCategoryId());
				server.getArchiveCategoryIds().forEach(categoryId -> {
					bot.getChannelById(categoryId).subscribe(category -> ((Category) category)
							.getChannels().subscribe(channel ->
									channel.delete().subscribe()));
					bot.deleteChannel(categoryId);
				});
				event.reply("This bot is being moved to a different account, since the developer has lost access to this one. " +
						"This account ceases function. " +
						"To keep using the bot, the server owner or a user with Manage Server permissions needs to invite the new account using the following link:\n" +
						"https://discord.com/oauth2/authorize?client_id=1072967745613860931&permissions=1342498832&scope=bot\n" +
						"This account will remove itself from your server automatically after that. " +
						"All data and settings will be preserved across bot accounts.\n" +
						"I have deleted all my channels and categories. These will regenerate once the bot is being used on the new account.\n" +
						"If you have questions or problems, please visit " + ExceptionHandler.supportServerInvite + ", or contact Ente#1658.").subscribe();
				return;
			}

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
			// TOKEN
			if (services.bot.isOld()) {
				event.reply("This bot is being moved to a different account, since the developer has lost access to this one. " +
						"This account ceases function. " +
						"To keep using the bot, the server owner or a user with Manage Server will permissions needs to invite the new account using the following link:\n" +
						"https://discord.com/oauth2/authorize?client_id=1072967745613860931&permissions=1342498832&scope=bot\n" +
						"This account will remove itself from your server automatically after that. " +
						"All data and settings will be preserved across bot accounts.\n" +
						"I have deleted all my channels and categories. These will regenerate once the bot is being used on the new account.\n" +
						"If you have questions or problems, please visit " + ExceptionHandler.supportServerInvite + ", or contact Ente#1658.\n" +
						"**Results for this match cannot be reported anymore. After the bot has moved to its new account, " +
						"a moderator can use /forcewin to resolve this match. Also this channel needs to be deleted manually.**").subscribe();
				return;
			}

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
