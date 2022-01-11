package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.Buttons;
import com.elorankingbot.backend.command.MessageContent;
import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;

import java.util.ArrayList;

public class Win extends ButtonCommandForChallenge {

	public Win(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue,
			   GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		ChallengeModel.ReportIntegrity reportIntegrity;
		if (isChallengerCommand) reportIntegrity = challenge.setChallengerReported(ChallengeModel.ReportStatus.WIN);
		else reportIntegrity = challenge.setAcceptorReported(ChallengeModel.ReportStatus.WIN);

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) processFirstToReport();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) processHarmony();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.CONFLICT) processConflict();
		event.acknowledge().subscribe();
	}

	private void processFirstToReport() {
		service.saveChallenge(challenge);

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You reported a win :arrow_up:. I'll let you know when your opponent reports.");
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.addLine("Your opponent reported a win :arrow_up:.");
		targetMessage.edit().withContent(targetMessageContent.get()).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_AUTO_RESOLVE, game.getMatchAutoResolveTime(),
				challenge.getId(), 0L, null);
	}

	private void processHarmony() {
		Match match = new Match(guildId,
				isChallengerCommand ? challenge.getChallengerId() : challenge.getAcceptorId(),
				isChallengerCommand ? challenge.getAcceptorId() : challenge.getChallengerId(),
				false);
		double[] eloResults = service.updateRatings(match);// TODO transaction machen?
		service.saveMatch(match);
		service.deleteChallenge(challenge);

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You reported a win :arrow_up:. The match has been resolved:")
				.addLine(String.format("Your rating went from %s to %s.",
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2])))
				.makeAllItalic();
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.makeAllNotBold()
				.addLine("Your opponent reported a win :arrow_up:. The match has been resolved:")
				.addLine(String.format("Your rating went from %s to %s.",
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3])))
				.makeAllItalic();
		targetMessage.edit().withContent(targetMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		bot.postToResultChannel(game, match);

		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), match);
	}

	private void processConflict() {
		service.saveChallenge(challenge);

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You reported a win :arrow_up:. Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, or file a dispute.")
				.makeLastLineBold();
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(createActionRow(targetMessage)).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.addLine("Your opponent reported a win :arrow_up:. Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, " +
						"or file a dispute.")
				.makeLastLineBold();
		targetMessage.edit().withContent(targetMessageContent.get())
				.withComponents(createActionRow(parentMessage)).subscribe();
	}

	private static ActionRow createActionRow(Message otherMessage) {
		return ActionRow.of(
				Buttons.redo(otherMessage.getChannelId().asLong()),
				Buttons.cancelOnConflict(otherMessage.getChannelId().asLong()),
				Buttons.redoOrCancelOnConflict(otherMessage.getChannelId().asLong()),
				Buttons.dispute(otherMessage.getChannelId().asLong()));
	}
}