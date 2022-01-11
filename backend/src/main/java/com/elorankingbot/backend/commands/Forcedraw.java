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

public class Forcedraw extends SlashCommand {

	private User player1;
	private User player2;
	private String reason;

	public Forcedraw(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot,
					 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.needsModRole = true;
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("forcedraw")
				.description("Force a draw for two players")
				.addOption(ApplicationCommandOptionData.builder()
						.name("player1").description("The first player")
						.type(ApplicationCommandOption.Type.USER.getValue()).required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("player2").description("The second player")
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
		player1 = event.getOption("player1").get().getValue().get().asUser().block();
		player2 = event.getOption("player2").get().getValue().get().asUser().block();
		if (player1.isBot() || player2.isBot()) {
			event.reply("Cannot force a match involving a bot.").subscribe();
			return;
		}
		if (player1.equals(player2)) {
			event.reply("Player1 and player2 must not be the same player.").subscribe();
			return;
		}

		Match match = new Match(guildId, player1.getId().asLong(), player2.getId().asLong(), true);
		double[] eloResults = service.updateRatings(match);
		service.saveMatch(match);

		reason = event.getOption("reason").isPresent() ?
				String.format(" Reason given: \"%s\"", event.getOption("reason").get().getValue().get().asString())
				: "";
		informPlayers(eloResults);
		bot.postToResultChannel(game, match);
		event.reply(String.format("Forced a draw between %s and %s.%s", player1.getTag(), player2.getTag(), reason)).subscribe();
	}

	private void informPlayers(double[] eloResults) {
		MessageContent player1MessageContent = new MessageContent(
				String.format("%s has forced a draw with %s. Your rating went from %s to %s.%s",
						event.getInteraction().getUser().getTag(), player2.getTag(),
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2]),
						reason))
				.makeAllItalic();
		MessageCreateSpec player1MessageSpec = MessageCreateSpec.builder()
				.content(player1MessageContent.get())
				.build();
		player1.getPrivateChannel().subscribe(channel -> channel.createMessage(player1MessageSpec).subscribe());

		MessageContent player2MessageContent = new MessageContent(
				String.format("%s has forced a loss to %s. Your rating went from %s to %s.%s",
						event.getInteraction().getUser().getTag(), player1.getTag(),
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3]),
						reason))
				.makeAllItalic();
		MessageCreateSpec player2MessageSpec = MessageCreateSpec.builder()
				.content(player2MessageContent.get())
				.build();
		player2.getPrivateChannel().subscribe(channel -> channel.createMessage(player2MessageSpec).subscribe());
	}
}