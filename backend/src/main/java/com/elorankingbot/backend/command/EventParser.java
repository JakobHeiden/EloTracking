package com.elorankingbot.backend.command;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.commands.admin.Setup;
import com.elorankingbot.backend.commands.challenge.ChallengeAsUserInteraction;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.ButtonInteractionEventWrapper;
import com.elorankingbot.backend.tools.ChatInputInteractionEventWrapper;
import com.elorankingbot.backend.tools.UserInteractionEventWrapper;
import com.google.common.collect.ImmutableMap;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class EventParser {

	static Map<String, String> commandStringToClassName = ImmutableMap.<String, String>builder()
			.put("permission", "admin.Permission")
			.put("reset", "admin.Reset")
			.put("setup", "admin.Setup")

			.put("forcematch", "mod.ForceMatch")

			.put("accept", "challenge.Accept")
			.put("cancel", "challenge.Cancel")
			.put("cancelonconflict", "challenge.CancelOnConflict")
			.put("challenge", "challenge.Challenge")
			.put("decline", "challenge.Decline")
			.put("dispute", "challenge.Dispute")
			.put("draw", "challenge.Draw")
			.put("lose", "challenge.Lose")
			.put("redo", "challenge.Redo")
			.put("redoorcancel", "challenge.RedoOrCancel")
			.put("win", "challenge.Win")

			.put("closechannellater", "dispute.CloseChannelLater")
			.put("closechannelnow", "dispute.CloseChannelNow")
			.put("ruleascancel", "dispute.RuleAsCancel")
			.put("ruleasdraw", "dispute.RuleAsDraw")
			.put("ruleaswin", "dispute.RuleAsWin")

			.build();

	public EventParser(GatewayDiscordClient client, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue,
					   Function<ChatInputInteractionEventWrapper, SlashCommand> slashCommandFactory,
					   Function<ButtonInteractionEventWrapper, ButtonCommand> buttonCommandFactory,
					   Function<UserInteractionEventWrapper, ChallengeAsUserInteraction> userInteractionChallengeFactory) {
		ApplicationService applicationService = client.getRestClient().getApplicationService();
		long botId = client.getSelfId().asLong();

		client.on(ChatInputInteractionEvent.class)
				.map(event -> new ChatInputInteractionEventWrapper(event, service, bot, queue, client))
				.map(slashCommandFactory::apply)
				.doOnNext(slashCommand -> log.debug(slashCommand.getClass().getSimpleName() + "::execute"))
				.subscribe(SlashCommand::execute);

		client.on(ButtonInteractionEvent.class)
				.map(event -> new ButtonInteractionEventWrapper(event, service, bot, queue, client))
				.map(buttonCommandFactory::apply)
				.doOnNext(buttonCommand -> log.debug(buttonCommand.getClass().getSimpleName() + "::execute"))
				.subscribe(ButtonCommand::execute);

		client.on(UserInteractionEvent.class)
				.map(event -> new UserInteractionEventWrapper(event, service, bot, queue, client))
				.map(userInteractionChallengeFactory::apply)
				.doOnNext(userInteraction -> log.debug(userInteraction.getClass().getSimpleName() + "::execute"))
				.subscribe(ChallengeAsUserInteraction::execute);

		client.on(GuildCreateEvent.class)
				.subscribe(event -> {
					Optional<Game> maybeGame = service.findGameByGuildId(event.getGuild().getId().asLong());
					if (maybeGame.isEmpty())
						applicationService.createGuildApplicationCommand(
										botId, event.getGuild().getId().asLong(), Setup.getRequest())
								.subscribe();
					else
						maybeGame.get().setMarkedForDeletion(false);
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
	}

	public static SlashCommand createSlashCommand(ChatInputInteractionEventWrapper wrapper) {
		String commandClassName = commandStringToClassName.get(wrapper.event().getCommandName());
		log.trace("commandClassName = " + commandClassName);
		try {
			return (SlashCommand) Class.forName("com.elorankingbot.backend.commands." + commandClassName)
					.getConstructor(ChatInputInteractionEvent.class, EloRankingService.class,
							DiscordBotService.class, TimedTaskQueue.class, GatewayDiscordClient.class)
					.newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
		} catch (Exception e) {
			wrapper.bot().sendToOwner("exception occurred while instantiating command " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static ButtonCommand createButtonCommand(ButtonInteractionEventWrapper wrapper) {
		String commandName = wrapper.event().getCustomId().split(":")[0];
		String commandClassName = commandStringToClassName.get(commandName);
		log.trace("commandClassName = " + commandClassName);
		try {
			return (ButtonCommand) Class.forName("com.elorankingbot.backend.commands." + commandClassName)
					.getConstructor(ButtonInteractionEvent.class, EloRankingService.class, DiscordBotService.class,
							TimedTaskQueue.class, GatewayDiscordClient.class)
					.newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
		} catch (Exception e) {
			wrapper.bot().sendToOwner("exception occurred while instantiating command " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static ChallengeAsUserInteraction createUserInteractionChallenge(UserInteractionEventWrapper wrapper) {
		return new ChallengeAsUserInteraction(
				wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
	}
}
