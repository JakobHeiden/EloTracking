package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.MessageContent;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class Forcewin extends SlashCommand {

	private User winner;
	private User loser;
	private String reason;

	public Forcewin(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot,
					TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.needsModRole = true;
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("forcewin")
				.description("Force a win of one player over another")
				.addOption(ApplicationCommandOptionData.builder()
						.name("winner").description("The player that gets a win")
						.type(ApplicationCommandOption.Type.USER.getValue()).required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("loser").description("The player that gets a loss")
						.type(ApplicationCommandOption.Type.USER.getValue()).required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("reason").description("Give a reason. This will be relayed to the players involved.")
						.type(ApplicationCommandOption.Type.STRING.getValue()).required(false)
						.build())
				.defaultPermission(false)
				.build();
	}

	public void execute() {
		if (!super.canExecute()) return;
		winner = event.getOption("winner").get().getValue().get().asUser().block();
		loser = event.getOption("loser").get().getValue().get().asUser().block();
		if (winner.isBot() || loser.isBot()) {
			event.reply("Cannot force a match involving a bot.").subscribe();
			return;
		}
		if (winner.equals(loser)) {
			event.reply("Winner and loser must not be the same player.").subscribe();
			return;
		}

		Match match = new Match(guildId, winner.getId().asLong(), loser.getId().asLong(), false);
		double[] eloResults = service.updateRatings(match);
		service.saveMatch(match);

		reason = event.getOption("reason").isPresent() ?
				String.format(" Reason given: \"%s\"", event.getOption("reason").get().getValue().get().asString())
				: "";
		informPlayers(eloResults);
		bot.postToResultChannel(game, match);
		event.reply(String.format("Forced a win for %s over %s.%s", winner.getTag(), loser.getTag(), reason)).subscribe();
	}

	private void informPlayers(double[] eloResults) {
		MessageContent winnerMessageContent = new MessageContent(
				String.format("%s has forced a win over %s. Your rating went from %s to %s.%s",
						event.getInteraction().getUser().getTag(), loser.getTag(),
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2]),
						reason))
				.makeAllItalic();
		MessageCreateSpec winnerMessageSpec = MessageCreateSpec.builder()
				.content(winnerMessageContent.get())
				.build();
		winner.getPrivateChannel().subscribe(channel -> channel.createMessage(winnerMessageSpec).subscribe());

		MessageContent loserMessageContent = new MessageContent(
				String.format("%s has forced a loss to %s. Your rating went from %s to %s.%s",
						event.getInteraction().getUser().getTag(), winner.getTag(),
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3]),
						reason))
				.makeAllItalic();
		MessageCreateSpec loserMessageSpec = MessageCreateSpec.builder()
				.content(loserMessageContent.get())
				.build();
		loser.getPrivateChannel().subscribe(channel -> channel.createMessage(loserMessageSpec).subscribe());
	}
}