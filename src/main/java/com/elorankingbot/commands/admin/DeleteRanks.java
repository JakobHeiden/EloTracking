package com.elorankingbot.commands.admin;

import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.command.annotations.RankingCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.HashMap;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

@AdminCommand
@RankingCommand
public class DeleteRanks extends SlashCommand {

	public DeleteRanks(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		var requestBuilder = ApplicationCommandRequest.builder()
				.name("deleteranks")
				.description(getShortDescription())
				.defaultPermission(true);
		if (server.getGames().size() > 1) {
			requestBuilder.addOption(ApplicationCommandOptionData.builder()
					.name("ranking")
					.description("Delete ranks for which ranking?")
					.type(STRING.getValue())
					.required(true)
					.addAllChoices(server.getGames().stream().map(game ->
									(ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder()
											.name(game.getName())
											.value(game.getName())
											.build())
							.toList())
					.build());
		}
		return requestBuilder.build();
	}

	public static String getShortDescription() {
		return "Delete all ranks for a ranking.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `ranking` Delete all ranks for this ranking. " +
				"This option won't be present if the server only has one ranking.\n" +
				"This will also remove the roles that previously were ranks from all players. " +
				"This will not delete the roles formerly associated with the ranks.";
	}

	protected void execute() {
		Game game = null;
		if (server.getGames().size() > 1) {
			game = server.getGame(event.getOption("ranking").get().getValue().get().asString());
		}
		if (server.getGames().size() == 1) {
			game = server.getGames().get(0);
		}
		if (game.getRequiredRatingToRankId().size() == 0) {
			event.reply("This ranking has no ranks, so there is nothing to delete.").subscribe();
			return;
		}

		bot.removeAllRanks(game);
		game.setRequiredRatingToRankId(new HashMap<>());
		dbService.saveServer(server);
		event.reply(String.format("Deleted all ranks for %s.", game.getName()))
				.subscribe(NO_OP, super::forwardToExceptionHandler);
	}
}
