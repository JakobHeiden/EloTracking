package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Buttons;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;

import java.util.ArrayList;

public class Lose extends ButtonCommand {

	public Lose(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue,
				GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		ChallengeModel.ReportIntegrity reportIntegrity;
		if (isChallengerCommand) reportIntegrity = challenge.setChallengerReported(ChallengeModel.ReportStatus.LOSE);
		else reportIntegrity = challenge.setAcceptorReported(ChallengeModel.ReportStatus.LOSE);

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) processFirstToReport();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) processHarmony();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.CONFLICT) processConflict();
		event.acknowledge().subscribe();
	}

	private void processFirstToReport() {
		service.saveChallenge(challenge);

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You reported a loss :arrow_down:. I'll let you know when your opponent reports.");
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.addLine("Your opponent reported a loss :arrow_down:.");
		targetMessage.edit().withContent(targetMessageContent.get()).subscribe();
	}

	private void processHarmony() {
		Match match = new Match(guildId,
				isChallengerCommand ? challenge.getAcceptorId() : challenge.getChallengerId(),
				isChallengerCommand ? challenge.getChallengerId() : challenge.getAcceptorId(),
				false);
		double[] eloResults = service.updateRatings(match);// TODO transaction machen?
		service.saveMatch(match);
		service.deleteChallenge(challenge);

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You reported a loss :arrow_down:. The match has been resolved:")
				.addLine(String.format("Your rating went from %s to %s", eloResults[1], eloResults[3]))
				.makeAllItalic();
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.makeAllNotBold()
				.addLine("Your opponent reported a loss :arrow_down:. The match has been resolved:")
				.addLine(String.format("Your rating went from %s to %s", eloResults[0], eloResults[2]))
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
				.addLine("You reported a loss :arrow_down:. Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, " +
						"or file a dispute.")
				.makeLastLineBold();
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(ActionRow.of(
						Buttons.redo(targetMessage.getChannelId().asLong()),
						Buttons.cancelOnConflict(targetMessage.getChannelId().asLong()),
						Buttons.redoOrCancelOnConflict(targetMessage.getChannelId().asLong()),
						Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.addLine("Your opponent reported a loss :arrow_down:. " +
						"Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, " +
						"or file a dispute.")
				.makeLastLineBold();
		targetMessage.edit().withContent(targetMessageContent.get())
				.withComponents(ActionRow.of(
						Buttons.redo(targetMessage.getChannelId().asLong()),
						Buttons.cancelOnConflict(targetMessage.getChannelId().asLong()),
						Buttons.redoOrCancelOnConflict(targetMessage.getChannelId().asLong()),
						Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();
	}
}
