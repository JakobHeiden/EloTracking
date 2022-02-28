package com.elorankingbot.backend.commands.mod;

import com.elorankingbot.backend.command.ModCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import static com.elorankingbot.backend.service.EloRankingService.formatRating;

@ModCommand
public class Rating extends SlashCommand {

	public Rating(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("rating")
				.description("Manipulate a player's rating")
				.addOption(ApplicationCommandOptionData.builder()
						.name("player").description("Manipulate this player's rating")
						.type(ApplicationCommandOption.Type.USER.getValue())
						.required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("mode").description("Set a new rating, or add/subtract points?")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(true)
						.addChoice(ApplicationCommandOptionChoiceData.builder().name("set").value("set").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder().name("add/subtract").value("add/subtract").build())
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("rating").description("The new rating, or the amount of points to add. Use a negative integer to subtract")
						.type(ApplicationCommandOption.Type.INTEGER.getValue())
						.required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("reason").description("Give a reason. This will be relayed to the player")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(false)
						.build())
				.defaultPermission(false)
				.build();
	}

	public void execute() {
		/*
		User playerUser = event.getOption("player").get().getValue().get().asUser().block();
		if (playerUser.isBot()) {
			event.reply("Bots don't have ratings.").subscribe();
			return;
		}

		boolean isSetNewRating = event.getOption("mode").get().getValue().get().asString().equals("set");
		int rating = Integer.parseInt(event.getOption("rating").get().getValue().get().getRaw());

		service.addNewPlayerIfPlayerNotPresent(guildId, playerUser.getId().asLong());
		Player player = service.findPlayerByGuildIdAndUserId(guildId, playerUser.getId().asLong()).get();
		double oldRating = player.getRating();
		double newRating = isSetNewRating ? rating : oldRating + rating;
		//player.setRating(newRating);
		service.savePlayer(player);
		//bot.updateLeaderboard(game);

		String reasonGiven = event.getOption("reason").isPresent() ?
				String.format(" Reason given: \"%s\"", event.getOption("reason").get().getValue().get().asString())
				: "";
		event.reply(String.format("%s's rating has been set to %s, from %s.%s",
				playerUser.getTag(), formatRating(newRating), formatRating(oldRating), reasonGiven)).subscribe();

		String playerMessageContent = String.format("%s has set your rating to %s, from %s.%s",
				event.getInteraction().getUser().getTag(), formatRating(newRating), formatRating(oldRating), reasonGiven);
		MessageCreateSpec playerMessageSpec = MessageCreateSpec.builder().content(playerMessageContent).build();
		playerUser.getPrivateChannel().subscribe(channel -> channel.createMessage(playerMessageSpec).subscribe());

		 */
	}
}
