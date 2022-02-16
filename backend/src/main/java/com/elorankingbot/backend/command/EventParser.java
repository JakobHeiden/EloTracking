package com.elorankingbot.backend.command;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.commands.admin.SetRole;
import com.elorankingbot.backend.commands.challenge.ChallengeAsUserInteraction;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.ButtonInteractionEventWrapper;
import com.elorankingbot.backend.tools.ChatInputInteractionEventWrapper;
import com.elorankingbot.backend.tools.UserInteractionEventWrapper;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Hooks;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class EventParser {

	private final DiscordBotService bot;
	private final Map<String, String> commandStringToClassName;

	public EventParser(GatewayDiscordClient client, EloRankingService service, DiscordBotService bot,
					   TimedTaskQueue queue, CommandClassScanner scanner,
					   Function<ChatInputInteractionEventWrapper, SlashCommand> slashCommandFactory,
					   Function<ButtonInteractionEventWrapper, ButtonCommand> buttonCommandFactory,
					   Function<UserInteractionEventWrapper, ChallengeAsUserInteraction> userInteractionChallengeFactory) {
		this.bot = bot;
		ApplicationService applicationService = client.getRestClient().getApplicationService();
		long botId = client.getSelfId().asLong();
		this.commandStringToClassName = scanner.getCommandStringToClassName();

		client.on(ChatInputInteractionEvent.class)
				.map(event -> new ChatInputInteractionEventWrapper(event, service, bot, queue, client))
				.map(slashCommandFactory::apply)
				.doOnNext(bot::logCommand)
				.doOnNext(SlashCommand::execute)
				.doOnError(this::handleError)
				.subscribe();

		client.on(ButtonInteractionEvent.class)
				.map(event -> new ButtonInteractionEventWrapper(event, service, bot, queue, client))
				.map(buttonCommandFactory::apply)
				.doOnNext(bot::logCommand)
				.doOnNext(ButtonCommand::execute)
				.doOnError(this::handleError)
				.subscribe();

		client.on(UserInteractionEvent.class)
				.map(event -> new UserInteractionEventWrapper(event, service, bot, queue, client))
				.map(userInteractionChallengeFactory::apply)
				.doOnNext(bot::logCommand)
				.doOnNext(ChallengeAsUserInteraction::execute)
				.doOnError(this::handleError)
				.subscribe();

		client.on(GuildCreateEvent.class)
				.subscribe(event -> {
					Optional<Server> maybeServer = service.findServerByGuildId(event.getGuild().getId().asLong());
					if (maybeServer.isEmpty()) {
						applicationService.createGuildApplicationCommand(botId, event.getGuild().getId().asLong(),
								SetRole.getRequest()).subscribe();
						// TODO! commands deployen
						Server server = new Server(event.getGuild().getId().asLong());
						service.saveServer(server);
					}
				});

		client.on(RoleDeleteEvent.class)
				.subscribe(event -> {
					Optional<Game> maybeGame = service.findGameByGuildId(event.getGuildId().asLong());
					if (maybeGame.isEmpty()) return;

					if (event.getRoleId().asLong() == maybeGame.get().getAdminRoleId()) {
						bot.setDiscordCommandPermissions(
								event.getGuildId().asLong(),
								"permission",
								event.getGuild().block().getEveryoneRole().block());
					}
				});

		client.on(Event.class).subscribe(event -> log.trace(event.getClass().getSimpleName()));

		Hooks.onErrorDropped(throwable -> {
			throwable.printStackTrace();
			bot.sendToOwner("Hooks::onErrorDropped: " + throwable.getMessage());
		});
	}

	private void handleError(Throwable throwable) {
		bot.sendToOwner(String.format("Error in EventParser: %s\n" +
				"Occured during %s", throwable.toString(), bot.getLatestCommandLog()));
	}

	public SlashCommand createSlashCommand(ChatInputInteractionEventWrapper wrapper) {
		String commandClassName = commandStringToClassName.get(wrapper.event().getCommandName());
		log.trace("commandClassName = " + commandClassName);
		try {
			return (SlashCommand) Class.forName(commandClassName)
					.getConstructor(ChatInputInteractionEvent.class, EloRankingService.class,
							DiscordBotService.class, TimedTaskQueue.class, GatewayDiscordClient.class)
					.newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
		} catch (Exception e) {
			wrapper.bot().sendToOwner("exception occurred while instantiating command " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public ButtonCommand createButtonCommand(ButtonInteractionEventWrapper wrapper) {
		String commandClassName = commandStringToClassName.get(wrapper.event().getCustomId().split(":")[0]);
		log.trace("commandClassName = " + commandClassName);
		try {
			return (ButtonCommand) Class.forName(commandClassName)
					.getConstructor(ButtonInteractionEvent.class, EloRankingService.class, DiscordBotService.class,
							TimedTaskQueue.class, GatewayDiscordClient.class)
					.newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
		} catch (Exception e) {
			wrapper.bot().sendToOwner("exception occurred while instantiating command " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public ChallengeAsUserInteraction createUserInteractionChallenge(UserInteractionEventWrapper wrapper) {
		return new ChallengeAsUserInteraction(
				wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
	}
}
