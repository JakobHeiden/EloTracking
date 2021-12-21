package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Emojis;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;

public class Lose extends ButtonInteractionCommand {

	public Lose(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(event, service, bot, queue);
	}

	public void execute() {
		ChallengeModel.ReportIntegrity reportIntegrity;
		if (isChallengerCommand) reportIntegrity = challenge.setChallengerReported(ChallengeModel.ReportStatus.LOSE);
		else reportIntegrity = challenge.setAcceptorReported(ChallengeModel.ReportStatus.LOSE);
		service.saveChallenge(challenge);
		Message reportedOnMessage = isChallengerCommand ?
				bot.getMessageById(Long.parseLong(event.getCustomId().split(":")[1]), challenge.getAcceptorMessageId()).block()
				: bot.getMessageById(Long.parseLong(event.getCustomId().split(":")[1]), challenge.getChallengerMessageId()).block();

		removeSelfReactions(reporterMessage, Emojis.arrowUp, Emojis.arrowDown, Emojis.leftRightArrow, Emojis.crossMark);

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) {
			MessageContent reporterMessageContent = new MessageContent(reporterMessage.getContent())
					.makeAllNotBold()
					.addNewLine("You reported a loss. I'll let you know when your opponent reported as well.");
			reporterMessage.edit().withContent(reporterMessageContent.get()).subscribe();

			MessageContent reportedOnMessageContent = new MessageContent(reportedOnMessage.getContent())
					.addNewLine("Your opponent reported a loss.");
			reportedOnMessage.edit().withContent(reportedOnMessageContent.get()).subscribe();
		}

		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) {
			Match match = new Match(guildId,
					isChallengerCommand ? challenge.getChallengerId() : challenge.getAcceptorId(),
					isChallengerCommand ? challenge.getAcceptorId() : challenge.getChallengerId(),
					false);
			double[] eloResults = service.updateRatings(match);// TODO transaction machen?
			service.saveMatch(match);
			service.deleteChallenge(challenge);

			MessageContent reporterMessageContent = new MessageContent(reporterMessage.getContent())
					.makeAllNotBold()
					.addNewLine("You reported a win. Your report matches that of your opponent. The match has been resolved:")
					.addNewLine(String.format("Your rating went from %s to %s", eloResults[0], eloResults[2]));
			reporterMessage.edit().withContent(reporterMessageContent.get()).subscribe();

			MessageContent reportedOnMessageContent = new MessageContent(reportedOnMessage.getContent())
					.makeAllNotBold()
					.addNewLine("The result reported by your opponent matches yours. The match has been resolved:")
					.addNewLine(String.format("Your rating went from %s to %s", eloResults[1], eloResults[3]));
			reportedOnMessage.edit().withContent(reportedOnMessageContent.get()).subscribe();
		}

	}
}
