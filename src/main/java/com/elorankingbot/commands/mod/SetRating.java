package com.elorankingbot.commands.mod;

import com.elorankingbot.FormatTools;
import com.elorankingbot.command.annotations.ModCommand;
import com.elorankingbot.command.annotations.RankingCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.Player;
import com.elorankingbot.model.PlayerGameStats;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Optional;

@ModCommand
@RankingCommand
public class SetRating extends SlashCommand {

	public SetRating(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		var requestBuilder = ApplicationCommandRequest.builder()
				.name(SetRating.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.defaultPermission(true)
				.addOption(ApplicationCommandOptionData.builder()
						.name("player").description("Choose a player")
						.type(ApplicationCommandOption.Type.USER.getValue())
						.required(true)
						.build());
		if (server.getGames().size() > 1) {
			var gameOption = ApplicationCommandOptionData.builder()
					.name("ranking")
					.description("For which ranking?")
					.type(ApplicationCommandOption.Type.STRING.getValue())
					.required(true);
			for (Game game : server.getGames()) {
				gameOption.addChoice(ApplicationCommandOptionChoiceData.builder()
						.name(game.getName()).value(game.getName()).build());
			}
			requestBuilder.addOption(gameOption.build());
		}
		requestBuilder.addOption(ApplicationCommandOptionData.builder()
						.name("mode")
						.description("Set new rating, or modify current rating?")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("set").value("set").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("modify").value("modify").build())
						.required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("points")
						.description("New rating, or how many points to add. Enter a negative value to subtract.")
						.type(ApplicationCommandOption.Type.NUMBER.getValue())
						.required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("reason").description("Give a reason. This will be relayed to the player.")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(false)
						.build());
		return requestBuilder.build();
	}

	public static String getShortDescription() {
		return "Set a player's rating. Alternatively, add or subtract rating.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `player` Choose a player.\n" +
				"`Required:` `ranking` Choose a ranking. Option will be absent if the server only has one ranking.\n" +
				"`Required:` `mode` Whether to set a new rating, or modify the current rating of the player.\n" +
				"`Required:` `points` If `mode` is `set`, the new rating for the player. If `mode` is `modify`, the " +
				"amount of points to add to the player's rating. Choose a negative value to subtract rating.\n" +
				"`Optional:` `reason` Give a reason for the rating change. This will be relayed to the player.";
	}

	protected void execute() throws Exception {
		User playerUser = event.getOption("player").get().getValue().get().asUser().block();
		if (playerUser.isBot()) {
			event.reply("Bots cannot have ratings.").subscribe();
			return;
		}
		if (playerUser.equals(activeUser) && !userIsAdmin) {
			event.reply(String.format("Only <@&%s> can set their own rating.", server.getAdminRoleId())).subscribe();
			return;
		}

		Player player = dbService.getPlayerOrGenerateIfNotPresent(guildId, event.getOption("player").get().getValue().get().asUser().block());
		boolean isSingularGame = server.getGames().size() == 1;
		Game game = isSingularGame ?
				server.getGames().get(0)
				: server.getGame(event.getOption("ranking").get().getValue().get().asString());
		boolean isSetRating = event.getOption("mode").get().getValue().get().asString().equals("set");
		double points = event.getOption("points").get().getValue().get().asDouble();
		PlayerGameStats playerGameStats = player.getOrCreatePlayerGameStats(game);
		double oldRating = playerGameStats.getRating();
		double newRating = isSetRating ? points : oldRating + points;
		playerGameStats.setRating(newRating);

		queueScheduler.updatePlayerInAllQueuesOfGame(game, player);
		matchService.updatePlayerMatches(game, player);
		matchService.updatePlayerRank(game, player, manageRoleFailedCallbackFactory());
		dbService.savePlayer(player);
		dbService.updateRankingsEntry(game, player, newRating);
		if (dbService.hasLeaderboardChanged(game, oldRating, newRating)) {
			channelManager.refreshLeaderboard(game);
		}

		event.reply(String.format("%s's %srating is now set to %s.", player.getTag(),
				isSingularGame ? "" : game.getName() + " ", FormatTools.formatRating(newRating))).subscribe();

		Optional<ApplicationCommandInteractionOption> maybeReason = event.getOption("reason");
		String reason = maybeReason.isPresent() ? " Reason given: " + maybeReason.get().getValue().get().asString() : "";
		bot.sendDM(playerUser, event, String.format("Your rating for %s has been set to %s by %s.%s",
				game.getName(), FormatTools.formatRating(newRating), activeUser.getTag(), reason));
	}
}
