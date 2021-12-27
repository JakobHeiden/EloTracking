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

public class Draw extends ButtonCommand {

	public Draw(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot,
				TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		ChallengeModel.ReportIntegrity reportIntegrity;
		if (isChallengerCommand) reportIntegrity = challenge.setChallengerReported(ChallengeModel.ReportStatus.DRAW);
		else reportIntegrity = challenge.setAcceptorReported(ChallengeModel.ReportStatus.DRAW);
		service.saveChallenge(challenge);

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) {
			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You reported a draw :left_right_arrow:. I'll let you know when your opponent reports.");
			parentMessage.edit().withContent(parentMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
					.addLine("Your opponent reported a draw :left_right_arrow:.");
			targetMessage.edit().withContent(targetMessageContent.get()).subscribe();
			return;
		}

		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) {
			Match match = new Match(guildId, challenge.getChallengerId(), challenge.getAcceptorId(), true);
			double[] eloResults = service.updateRatings(match);// TODO transaction machen?
			service.saveMatch(match);
			service.deleteChallenge(challenge);

			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You reported a draw :left_right_arrow:. The match has been resolved:")
					.addLine(String.format("Your rating went from %s to %s", eloResults[0], eloResults[2]))
					.makeAllItalic();
			parentMessage.edit().withContent(parentMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
					.makeAllNotBold()
					.addLine("Your opponent reported a draw :left_right_arrow:. The match has been resolved:")
					.addLine(String.format("Your rating went from %s to %s", eloResults[1], eloResults[3]))
					.makeAllItalic();
			targetMessage.edit().withContent(targetMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			bot.postToResultChannel(game, match);

			queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
					parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), match);
			queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
					targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), match);
			return;
		}

		if (reportIntegrity == ChallengeModel.ReportIntegrity.CONFLICT) {
			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You reported a draw :left_right_arrow:.")
					.addLine("Your report and that of your opponent is in conflict.")
					.addLine("You can call for a redo :leftwards_arrow_with_hook: of the reporting, " +
							"and/or call for a cancel, or file a dispute :exclamation:.")
					.makeLastLineBold();
			parentMessage.edit().withContent(parentMessageContent.get())
					.withComponents(ActionRow.of(
							Buttons.redo(targetMessage.getChannelId().asLong()),
							Buttons.cancelOnConflict(targetMessage.getChannelId().asLong()),
							Buttons.redoOrCancelOnConflict(targetMessage.getChannelId().asLong()),
							Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();

			MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
					.addLine("Your opponent reported a draw :left_right_arrow:.")
					.addLine("Your report and that of your opponent is in conflict.")
					.addLine("You can call for a redo :leftwards_arrow_with_hook: of the reporting, " +
							"and/or call for a cancel, or file a dispute :exclamation:.")
					.makeLastLineBold();
			targetMessage.edit().withContent(targetMessageContent.get())
					.withComponents(ActionRow.of(
							Buttons.redo(targetMessage.getChannelId().asLong()),
							Buttons.cancelOnConflict(targetMessage.getChannelId().asLong()),
							Buttons.redoOrCancelOnConflict(targetMessage.getChannelId().asLong()),
							Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();

			// I have no idea why this is necessary here but not in the other cases
			event.acknowledge().subscribe();
		}
	}
}
