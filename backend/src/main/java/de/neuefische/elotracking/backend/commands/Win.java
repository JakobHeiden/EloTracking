package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

import java.util.ArrayList;

public class Win extends ButtonInteractionCommand {

	public Win(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue,
			   GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		ChallengeModel.ReportIntegrity reportIntegrity;
		if (isChallengerCommand) reportIntegrity = challenge.setChallengerReported(ChallengeModel.ReportStatus.WIN);
		else reportIntegrity = challenge.setAcceptorReported(ChallengeModel.ReportStatus.WIN);
		service.saveChallenge(challenge);
		System.out.println(isChallengerCommand);
		Message reportedOnMessage = isChallengerCommand ?
				bot.getMessageById(otherPlayerPrivateChannelId, challenge.getAcceptorMessageId()).block()
				: bot.getMessageById(otherPlayerPrivateChannelId, challenge.getChallengerMessageId()).block();

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) {
			MessageContent reporterMessageContent = new MessageContent(reporterMessage.getContent())
					.makeAllNotBold()
					.addLine("You reported a win :arrow_up:. I'll let you know when your opponent reported as well.");
			reporterMessage.edit().withContent(reporterMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent reportedOnMessageContent = new MessageContent(reportedOnMessage.getContent())
					.addLine("Your opponent reported a win.");
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
					.addLine("You reported a win :arrow_up:. Your report matches that of your opponent. The match has been resolved:")
					.addLine(String.format("Your rating went from %s to %s", eloResults[0], eloResults[2]))
					.makeAllItalic();
			reporterMessage.edit().withContent(reporterMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent reportedOnMessageContent = new MessageContent(reportedOnMessage.getContent())
					.makeAllNotBold()
					.addLine("The result reported by your opponent matches your report. The match has been resolved:")
					.addLine(String.format("Your rating went from %s to %s", eloResults[1], eloResults[3]))
					.makeAllItalic();
			reportedOnMessage.edit().withContent(reportedOnMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();
		}
	}
}
