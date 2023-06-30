package com.elorankingbot.commands.admin.deleteranking;

import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.command.annotations.RankingCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.components.Buttons;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

@AdminCommand
@RankingCommand
public class DeleteRanking extends SlashCommand {

	private String gameName;
	private long userId;

	public DeleteRanking(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		List<ApplicationCommandOptionChoiceData> rankingChoices = server.getGames().stream()
				.map(game -> game.getName())
				.map(gameName -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder()
						.name(gameName).value(gameName).build())
				.toList();
		return ApplicationCommandRequest.builder()
				.name(DeleteRanking.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.defaultPermission(true)
				.addOption(ApplicationCommandOptionData.builder()
						.name("ranking").description("Which ranking to delete")
						.type(STRING.getValue())
						.addAllChoices(rankingChoices)
						.required(true).build())
				.build();
	}

	public static String getShortDescription() {
		return "Delete a ranking.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `ranking` The ranking you want to delete.\n" +
				"This will also delete all queues added to this ranking, and all match histories for this ranking.";
	}

	protected void execute() {
		gameName = event.getOption("ranking").get().getValue().get().asString();
		userId = event.getInteraction().getUser().getId().asLong();
		event.reply(InteractionApplicationCommandCallbackSpec.builder()
				.content("Are you sure? This will delete the ranking, all queues added to it, its leaderboard, and " +
						"all player data and match history associated with this ranking.")
				.addComponent(createActionRow())
				.build()).subscribe();
	}

	private ActionRow createActionRow() {
		return ActionRow.of(
				Buttons.confirmDeleteRanking(gameName, userId),
				Buttons.abortDeleteRanking(gameName, userId)
		);
	}
}
