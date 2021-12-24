package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Emojis;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
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

public class Cancel extends ButtonCommand {

	public Cancel(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot,
				  TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		ChallengeModel.ReportIntegrity reportIntegrity;
		if (isChallengerCommand) reportIntegrity = challenge.setChallengerReported(ChallengeModel.ReportStatus.CANCEL);
		else reportIntegrity = challenge.setAcceptorReported(ChallengeModel.ReportStatus.CANCEL);
		service.saveChallenge(challenge);

		Message reportedOnMessage = isChallengerCommand ?
				bot.getMessageById(otherPlayerPrivateChannelId, challenge.getAcceptorMessageId()).block()
				: bot.getMessageById(otherPlayerPrivateChannelId, challenge.getChallengerMessageId()).block();

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) {
			MessageContent reporterMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You called for a cancel :negative_squared_cross_mark:. " +
							"I'll let you know when your opponent reacts.");
			parentMessage.edit().withContent(reporterMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent reportedOnMessageContent = new MessageContent(reportedOnMessage.getContent())
					.addLine("Your opponent called for a cancel :negative_squared_cross_mark:.");
			reportedOnMessage.edit().withContent(reportedOnMessageContent.get()).subscribe();
			return;
		}

		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) {
			service.deleteChallenge(challenge);

			MessageContent reporterMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You called for a cancel :negative_squared_cross_mark:. " +
							"The challenge has been canceled.")
					.makeAllItalic();
			parentMessage.edit().withContent(reporterMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent reportedOnMessageContent = new MessageContent(reportedOnMessage.getContent())
					.makeAllNotBold()
					.addLine("Your opponent called for a cancel :negative_squared_cross_mark:. " +
							"The challenge has been canceled.")
					.makeAllItalic();
			reportedOnMessage.edit().withContent(reportedOnMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			queue.addTimedTask(TimedTask.TimedTaskType.DELETE_MESSAGE, game.getDeleteMessageTime(),
					parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), null);
			queue.addTimedTask(TimedTask.TimedTaskType.DELETE_MESSAGE, game.getDeleteMessageTime(),
					reportedOnMessage.getId().asLong(), reportedOnMessage.getChannelId().asLong(), null);
			return;
		}

		if (reportIntegrity == ChallengeModel.ReportIntegrity.CONFLICT) {
			MessageContent reporterMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You called for a cancel :negative_squared_cross_mark:.")
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
					.addLine("Your opponent called for a cancel :negative_squared_cross_mark:.")
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
