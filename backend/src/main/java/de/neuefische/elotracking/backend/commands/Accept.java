package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.PrivateChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
public class Accept extends Command {

	private boolean canExecute = true;

	public Accept(Event event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(event, service, bot, queue);
	}

	public void execute() {
		if (!super.canExecute()) return;

		ReactionAddEvent event = (ReactionAddEvent) super.event;
		Mono<Message> acceptorMessageMono = event.getMessage();

		long acceptorId = 0L;
		acceptorId = event.getUserId().asLong();
		long acceptorMessageId = event.getMessageId().asLong();
		super.guildId = service.findChallengeByAcceptorMessageId(acceptorMessageId).get().getGuildId();
		List<ChallengeModel> challenges = service.findAllChallengesByAcceptorIdAndChannelId(acceptorId, guildId);
		long challengerId = 0L;
		challengerId = service.findChallengeByAcceptorMessageId(event.getMessageId().asLong()).get().getChallengerId();

		Optional<ChallengeModel> challenge;
		challenge = getRelevantChallenge(challenges, challengerId);
		if (!canExecute) return;

		Mono<Message> challengerMessageMono = bot.getMessageById(challenge.get().getChallengerPrivateChannelId(), challenge.get().getChallengerMessageId());

		service.addNewPlayerIfPlayerNotPresent(guildId, acceptorId);
		challenge.get().setAccepted(true);// TODO Optional?
		queue.addTimedTask(TimedTask.TimedTaskType.ACCEPTED_CHALLENGE_DECAY, game.getAcceptedChallengeDecayTime(), challenge.get().getChallengerMessageId());
		service.saveChallenge(challenge.get());

		Message acceptorMessage = acceptorMessageMono.block();
		acceptorMessage.removeSelfReaction(bot.crossMark).subscribe();
		acceptorMessage.removeSelfReaction(bot.checkMark).subscribe();
		acceptorMessage.edit().withContent(makeNotBold(acceptorMessage.getContent()) + "\nYou accepted the challenge.\n" +
				makeBold("Come back after the match and let me know if you won :arrow_up: or lost :arrow_down:")).subscribe();
		acceptorMessage.addReaction(bot.arrowUp).subscribe();
		acceptorMessage.addReaction(bot.arrowDown).subscribe();

		Message challengerMessage = challengerMessageMono.block();
		challengerMessage.edit().withContent(challengerMessage.getContent()
				+ "\nThey have accept your challenge." +
				makeBold("\nCome back after the match and let me know if you won :arrow_up: or lost :arrow_down:")).subscribe();
		challengerMessage.addReaction(bot.arrowUp).subscribe();
		challengerMessage.addReaction(bot.arrowDown).subscribe();
	}

	private Optional<ChallengeModel> getRelevantChallenge(List<ChallengeModel> challenges, long challengerId) {
		Optional<ChallengeModel> challenge = challenges.stream().
				filter(chlng -> chlng.getChallengerId() == challengerId)
				.findAny();

		if (challenge.isEmpty()) {// TODO evtl weg
			addBotReply("That player has not yet challenged you");
			canExecute = false;
			return challenge;
		}
		if (challenge.get().isAccepted()) {// TODO evtl weg
			addBotReply("You already accepted that Challenge");
			canExecute = false;
			return Optional.empty();
		}

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

	private String makeNotBold(String text) {
		return text.replace("*", "");
	}

	private String makeBold(String text) {
		return "**" + text + "**";
	}
}
