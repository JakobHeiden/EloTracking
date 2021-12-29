package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Buttons;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;

import java.util.ArrayList;

public class Ruleaswin extends ButtonCommandForDispute {

	private String moderatorName;
	private long winnerId;
	private long loserId;
	private double[] eloResults;
	private Match match;
	private Message challengerMessage;
	private Message acceptorMessage;

	public Ruleaswin(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (!event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getAdminRoleId()))
				&& !event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getModRoleId()))) {
			event.acknowledge().subscribe();
			return;
		}

		boolean isChallengerWin = event.getCustomId().split(":")[2].equals("true");
		winnerId = isChallengerWin ? challenge.getChallengerId() : challenge.getAcceptorId();
		loserId = isChallengerWin ? challenge.getAcceptorId() : challenge.getChallengerId();
		moderatorName = event.getInteraction().getUser().getUsername();

		match = new Match(challenge.getGuildId(), winnerId, loserId, false);
		eloResults = service.updateRatings(match);
		service.saveMatch(match);
		service.deleteChallenge(challenge);

		postToDisputeChannel();
		bot.postToResultChannel(game, match);
		postToChallengerAndAcceptorChannels();
		addMatchSummarizeToQueue();
	}

	private void postToDisputeChannel() {
		event.getInteraction().getMessage().get().edit()
				.withComponents(none).subscribe();
		disputeChannel.createMessage(String.format(
				"%s has ruled the match a win :arrow_up: for <@%s> and a loss :arrow_down: for <@%s>",
				moderatorName, winnerId, loserId))
				.withComponents(ActionRow.of(
						Buttons.closeChannelNow(),
						Buttons.closeChannelLater()
				)).subscribe();
	}

	private void postToChallengerAndAcceptorChannels() {
		challengerMessage = client.getMessageById(Snowflake.of(challenge.getChallengerChannelId()),
				Snowflake.of(challenge.getChallengerMessageId())).block();
		acceptorMessage = client.getMessageById(Snowflake.of(challenge.getAcceptorChannelId()),
				Snowflake.of(challenge.getAcceptorMessageId())).block();

		MessageContent challengerMessageContent = new MessageContent(challengerMessage.getContent())
				.addLine(String.format("%s has ruled this as a win :arrow_up: for you.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s", eloResults[0], eloResults[2]))
				.makeAllItalic();
		challengerMessage.edit().withContent(challengerMessageContent.get())
				.withComponents(none).subscribe();

		MessageContent acceptorMessageContent = new MessageContent(acceptorMessage.getContent())
				.addLine(String.format("%s has ruled this as a loss :arrow_down: for you.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s", eloResults[1], eloResults[3]))
				.makeAllItalic();
		acceptorMessage.edit().withContent(acceptorMessageContent.get())
				.withComponents(none).subscribe();
	}

	private void addMatchSummarizeToQueue() {
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				challengerMessage.getId().asLong(), challengerMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				acceptorMessage.getId().asLong(), acceptorMessage.getChannelId().asLong(), match);
	}
}
