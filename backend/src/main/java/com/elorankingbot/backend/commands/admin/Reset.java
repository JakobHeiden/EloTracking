package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

@AdminCommand
public class Reset extends SlashCommand {

	public Reset(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		return ApplicationCommandRequest.builder()
				.name("reset")
				.description(getShortDescription())
				.defaultPermission(false)
				.addOption(ApplicationCommandOptionData.builder()
						.name("ranking")
						.description("Which ranking to reset")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(true)
						.addAllChoices(server.getGames().stream().map(game ->
										(ApplicationCommandOptionChoiceData)
												ApplicationCommandOptionChoiceData.builder()
														.name(game.getName())
														.value(game.getName())
														.build())
								.toList())
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("areyousure")
						.description("This is not reversible. Are you completely sure? Enter the name of the game to proceed.")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(true)
						.build())
				.build();
	}

	public static String getShortDescription() {
		return "Reset all player ratings for a ranking.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `ranking` Which ranking to reset.\n" +
				"`Required:` `areyousure` As a precaution against accidental use, enter the name of the ranking to proceed.\n" +
				"Resets all player ratings, wins, losses, and possibly draws for a ranking. Match history is not affected.";
	}

	protected void execute() {
		Game game = server.getGame(event.getOption("ranking").get().getValue().get().asString().toLowerCase());
		String entered = event.getOption("areyousure").get().getValue().get().asString();
		if (!entered.equals(game.getName())) {
			event.reply(String.format("Aborting. The name of the ranking is \"%s\". You entered \"%s\".",
					game.getName(), entered)).subscribe();
			return;
		}

		dbService.resetAllPlayerRatings(game);
		bot.refreshLeaderboard(game).subscribe();
		event.reply(String.format("Resetting all player ratings, wins%s for %s.",
				game.isAllowDraw() ? ", losses and draws" : " and losses",
				game.getName())).subscribe();
	}
}
