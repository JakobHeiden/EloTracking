package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Emojis;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;

import static de.neuefische.elotracking.backend.command.MessageContent.*;

import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
public class Accept extends EmojiCommand {

	public Accept(ReactionAddEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(event, service, bot, queue);
	}

	public void execute() {
		long acceptorId = event.getUserId().asLong();
		long challengerId = service.getChallengeByAcceptorMessageId(event.getMessageId().asLong()).get().getChallengerId();
		List<ChallengeModel> challenges = service.findAllChallengesByAcceptorIdAndChannelId(acceptorId, guildId);

		ChallengeModel challenge = getRelevantChallenge(challenges, challengerId).get();

		service.addNewPlayerIfPlayerNotPresent(guildId, acceptorId);
		challenge.setAccepted(true);// TODO Optional?
		queue.addTimedTask(TimedTask.TimedTaskType.ACCEPTED_CHALLENGE_DECAY, game.getAcceptedChallengeDecayTime(), challenge.getChallengerMessageId());
		service.saveChallenge(challenge);

		Message acceptorMessage = event.getMessage().block();
		acceptorMessage.removeSelfReaction(Emojis.crossMark).subscribe();
		acceptorMessage.removeSelfReaction(Emojis.checkMark).subscribe();
		MessageContent acceptorMessageContent = new MessageContent(acceptorMessage.getContent())
				.makeAllNotBold()
				.addNewLine("You have accepted the challenge.")
				.addNewLine("Come back after the match and let me know if you won :arrow_up: or lost :arrow_down:")
				.makeLastLineBold();
		acceptorMessage.edit().withContent(acceptorMessageContent.get()).subscribe();
		acceptorMessage.addReaction(Emojis.arrowUp).subscribe();
		acceptorMessage.addReaction(Emojis.arrowDown).subscribe();

		Message challengerMessage = bot.getMessageById(challenge.getChallengerPrivateChannelId(), challenge.getChallengerMessageId()).block();
		System.out.println(challengerMessage.getId().asLong());
		MessageContent challengerMessageContent = new MessageContent(challengerMessage.getContent())
				.addNewLine("They have accepted your challenge.")
				.addNewLine("Come back after the match and let me know if you won :arrow_up: or lost :arrow_down:")
				.makeLastLineBold();
		challengerMessage.edit().withContent(challengerMessageContent.get()).subscribe();
		challengerMessage.addReaction(Emojis.arrowUp).subscribe();
		challengerMessage.addReaction(Emojis.arrowDown).subscribe();
	}

	private Optional<ChallengeModel> getRelevantChallenge(List<ChallengeModel> challenges, long challengerId) {
		Optional<ChallengeModel> challenge = challenges.stream().
				filter(chlng -> chlng.getChallengerId() == challengerId)
				.findAny();

		return challenge;
	}

	private String getChallengerNames(List<ChallengeModel> challenges) {
		if (challenges.isEmpty()) return "";

		String returnString = "";
		for (ChallengeModel challenge : challenges) {//TODO make requests run parralel
			returnString += String.format("%s, ", bot.getPlayerName(challenge.getChallengerId()));
		}
		return returnString.substring(0, returnString.length() - 2);
	}


}
