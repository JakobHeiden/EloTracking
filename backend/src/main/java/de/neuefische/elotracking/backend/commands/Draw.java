package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Emojis;
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
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;

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

		Message reportedOnMessage = isChallengerCommand ?
				bot.getMessageById(otherPlayerPrivateChannelId, challenge.getAcceptorMessageId()).block()
				: bot.getMessageById(otherPlayerPrivateChannelId, challenge.getChallengerMessageId()).block();

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) {
			MessageContent reporterMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You reported a draw :left_right_arrow:. I'll let you know when your opponent reports.");
			parentMessage.edit().withContent(reporterMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent reportedOnMessageContent = new MessageContent(reportedOnMessage.getContent())
					.addLine("Your opponent reported a draw :left_right_arrow:.");
			reportedOnMessage.edit().withContent(reportedOnMessageContent.get()).subscribe();
			return;
		}

		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) {
			Match match = new Match(guildId, challenge.getChallengerId(), challenge.getAcceptorId(), true);
			double[] eloResults = service.updateRatings(match);// TODO transaction machen?
			service.saveMatch(match);
			service.deleteChallenge(challenge);

			MessageContent reporterMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You reported a draw :left_right_arrow:. The match has been resolved:")
					.addLine(String.format("Your rating went from %s to %s", eloResults[0], eloResults[2]))
					.makeAllItalic();
			parentMessage.edit().withContent(reporterMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent reportedOnMessageContent = new MessageContent(reportedOnMessage.getContent())
					.makeAllNotBold()
					.addLine("Your opponent reported a draw :left_right_arrow:. The match has been resolved:")
					.addLine(String.format("Your rating went from %s to %s", eloResults[1], eloResults[3]))
					.makeAllItalic();
			reportedOnMessage.edit().withContent(reportedOnMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			bot.postToResultChannel(game, match);

			queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMatchSummarizeTime(),
					parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), match);
			queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMatchSummarizeTime(),
					reportedOnMessage.getId().asLong(), reportedOnMessage.getChannelId().asLong(), match);
			return;
		}

		if (reportIntegrity == ChallengeModel.ReportIntegrity.CONFLICT) {
			MessageContent reporterMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You reported a draw :left_right_arrow:.")
					.addLine("Your report and that of your opponent is in conflict.")
					.addLine("You can call for a redo :leftwards_arrow_with_hook: of the reporting, " +
							"or file a dispute :exclamation:.")
					.makeLastLineBold();
			parentMessage.edit().withContent(reporterMessageContent.get())
					.withComponents(ActionRow.of(
							Button.primary("redo:" + reportedOnMessage.getChannelId().asString(),
									Emojis.redoArrow, "Call for redo"),
							Button.secondary("dispute:" + reportedOnMessage.getChannelId().asString(),
									Emojis.exclamation, "File a dispute"))).subscribe();

			MessageContent reportedOnMessageContent = new MessageContent(reportedOnMessage.getContent())
					.addLine("Your opponent reported a draw :left_right_arrow:.")
					.addLine("Your report and that of your opponent is in conflict.")
					.addLine("You can call for a redo :leftwards_arrow_with_hook: of the reporting, " +
							"or file a dispute :exclamation:.")
					.makeLastLineBold();
			reportedOnMessage.edit().withContent(reportedOnMessageContent.get())
					.withComponents(ActionRow.of(
							Button.primary("redo:" + reportedOnMessage.getChannelId().asString(),
									Emojis.redoArrow, "Call for a redo"),
							Button.secondary("dispute:" + reportedOnMessage.getChannelId().asString(),
									Emojis.exclamation, "File a dispute"))).subscribe();

			// I have no idea why this is necessary here but not in the other cases
			event.acknowledge().subscribe();
		}
	}
}
