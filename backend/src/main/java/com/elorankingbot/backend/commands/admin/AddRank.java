package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.slf4j.Slf4j;

import static discord4j.core.object.command.ApplicationCommandOption.Type.*;

@Slf4j
@AdminCommand
public class AddRank extends SlashCommand {

	public AddRank(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		var requestBuilder = ApplicationCommandRequest.builder()
				.name("addrank")
				.description(getShortDescription())
				.defaultPermission(false);
		if (server.getGames().size() > 1) {
			requestBuilder.addOption(ApplicationCommandOptionData.builder()
					.name("ranking")
					.description("Add a rank to which ranking?")
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
		return requestBuilder.addOption(ApplicationCommandOptionData.builder()
						.name("role")
						.description("Which role to assign as a rank?")
						.type(ROLE.getValue())
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("rating")
						.description("The minimum rating to acquire the rank")
						.type(INTEGER.getValue())
						.required(true).build())
				.build();
	}

	public static String getShortDescription() {
		return "Assign a role as a rank, to be automatically assigned based on player rating.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `ranking` Add a rank to this ranking. " +
				"This option won't be present if the server only has one ranking.\n" +
				"`Required:` `role` The role to make into a rank.\n" +
				"`Required:` `rating` The minimum rating to attain the new rank.\n" +
				"If a player qualifies for more than one rank, only the highest rank will apply.\n" +
				"Note that using the same roles as ranks in different rankings will lead to unspecified behavior and is " +
				"recommended against.";// TODO programmatisch ausschliessen. ausserdem ausschliessen dass die gleiche role in 1 ranking mehrmals verwendent wird
	}

	public void execute() {
		int rating;
		try {
			rating = Math.toIntExact(event.getOption("rating").get().getValue().get().asLong());
		} catch (ArithmeticException e) {
			event.reply("Selected rating is too large.").subscribe();
			return;
		}
		Role role = event.getOption("role").get().getValue().get().asRole().block();
		if (role.isEveryone()) {
			event.reply("Cannot make @everyone a rank.").subscribe();
			return;
		}
		Game game = null;
		if (server.getGames().size() > 1) {
			game = server.getGame(event.getOption("ranking").get().getValue().get().asString());
		}
		if (server.getGames().size() == 1) {
			game = server.getGames().get(0);
		}
		if (server.getGames().size() == 0) {
			String errorMessage = "Error: no ranking found";
			event.reply(errorMessage).subscribe();
			log.error(errorMessage);// TODO was sinnvolleres
			return;
		}

		game.getRequiredRatingToRankId().put(rating, role.getId().asLong());
		dbService.saveServer(server);
		for (Player player : dbService.findAllPlayersForServer(server)) {
			bot.updatePlayerRank(game, player);
		}
		event.reply(String.format("%s will now automatically be assigned to any player of %s who reaches %s rating.",
				role.getName(), game.getName(), rating)).subscribe();
	}
}