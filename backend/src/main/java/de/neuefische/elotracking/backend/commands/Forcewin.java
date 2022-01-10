package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.awt.event.ActionListener;

public class Forcewin extends SlashCommand {

	private User winner;
	private User loser;

	public Forcewin(ChatInputInteractionEvent event, EloTrackingService service, DiscordBotService bot,
					TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.needsGame = true;
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

		informPlayers(eloResults);
		bot.postToResultChannel(game, match);
		event.reply(String.format("Forced a win for %s over %s.", winner.getTag(), loser.getTag())).subscribe();
	}

	private void informPlayers(double[] eloResults) {
		MessageContent winnerMessageContent = new MessageContent(
				String.format("%s has forced a win over %s. Your rating went from %s to %s.",
						event.getInteraction().getUser().getTag(), loser.getTag(),
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2])))
				.makeAllItalic();
		MessageCreateSpec winnerMessageSpec = MessageCreateSpec.builder()
				.content(winnerMessageContent.get())
				.build();
		winner.getPrivateChannel().subscribe(channel -> channel.createMessage(winnerMessageSpec).subscribe());

		MessageContent loserMessageContent = new MessageContent(
				String.format("%s has forced a loss to %s. Your rating went from %s to %s.",
						event.getInteraction().getUser().getTag(), winner.getTag(),
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3])))
				.makeAllItalic();
		MessageCreateSpec loserMessageSpec = MessageCreateSpec.builder()
				.content(loserMessageContent.get())
				.build();
		loser.getPrivateChannel().subscribe(channel -> channel.createMessage(loserMessageSpec).subscribe());
	}
}
