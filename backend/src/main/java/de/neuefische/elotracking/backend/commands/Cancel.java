package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Buttons;
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

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) {
			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You called for a cancel :negative_squared_cross_mark:. " +
							"I'll let you know when your opponent reacts.");
			parentMessage.edit().withContent(parentMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
					.addLine("Your opponent called for a cancel :negative_squared_cross_mark:.");
			targetMessage.edit().withContent(targetMessageContent.get()).subscribe();
			return;
		}

		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) {
			service.deleteChallenge(challenge);

			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You called for a cancel :negative_squared_cross_mark:. " +
							"The challenge has been canceled.")
					.makeAllItalic();
			parentMessage.edit().withContent(parentMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
					.makeAllNotBold()
					.addLine("Your opponent called for a cancel :negative_squared_cross_mark:. " +
							"The challenge has been canceled.")
					.makeAllItalic();
			targetMessage.edit().withContent(targetMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			queue.addTimedTask(TimedTask.TimedTaskType.DELETE_MESSAGE, game.getDeleteMessageTime(),
					parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), null);
			queue.addTimedTask(TimedTask.TimedTaskType.DELETE_MESSAGE, game.getDeleteMessageTime(),
					targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), null);
			return;
		}

		if (reportIntegrity == ChallengeModel.ReportIntegrity.CONFLICT) {
			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You called for a cancel :negative_squared_cross_mark:.")
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
					.addLine("Your opponent called for a cancel :negative_squared_cross_mark:.")
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
