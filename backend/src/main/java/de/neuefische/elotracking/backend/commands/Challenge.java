package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.commandparser.Emojis;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class Challenge extends Command {

	private final ApplicationCommandInteractionEvent event;

	public Challenge(Event event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(event, service, bot, queue);
		this.event = (ApplicationCommandInteractionEvent) event;
	}

	public static String getDescription() {
		return "!ch[allenge] @player - challenge another player to a match";
	}

	public void execute() {
		super.checkForGame();

		long challengerId = event.getInteraction().getUser().getId().asLong();
		long acceptorId = 0L;
		if (event instanceof ChatInputInteractionEvent) {
			acceptorId = ((ChatInputInteractionEvent) event).getOption("player").get().getValue().get().asUser().block().getId().asLong();
		} else if (event instanceof UserInteractionEvent) {
			acceptorId = ((UserInteractionEvent) event).getTargetId().asLong();
		}
		log.warn(String.valueOf(acceptorId));

		if (challengerId == acceptorId) {
			addBotReply("You cannot challenge yourself");// TODO anders ausschliessen?
			return;
		}
		if (service.challengeExistsByParticipants(guildId, challengerId, acceptorId)) {
			addBotReply("challenge already exists");
			return;
		}

		Mono<Message> messageToAcceptorMono = bot.sendToUser(acceptorId, String.format(
				"**You have been challenged by <@%s>. Accept?**", challengerId));
		Mono<Message> messageToChallengerMono = bot.sendToUser(challengerId, String.format(
				"You have challenged <@%s> to a match. I'll let you know when they react.", acceptorId));
		Message messageToAcceptor = messageToAcceptorMono.block();// TODO das geht sicherlich besser
		messageToAcceptor.addReaction(Emojis.checkMark).subscribe();
		messageToAcceptor.addReaction(Emojis.crossMark).subscribe();
		Message messageToChallenger = messageToChallengerMono.block();

		service.addNewPlayerIfPlayerNotPresent(guildId, challengerId);
		ChallengeModel challenge = new ChallengeModel(guildId, messageToChallenger.getId().asLong(),
				messageToChallenger.getChannel().block().getId().asLong(), messageToAcceptor.getId().asLong(), challengerId, acceptorId);

		System.out.println(challenge.getChallengerMessageId());
		System.out.println(challenge.getAcceptorMessageId());

		queue.addTimedTask(TimedTask.TimedTaskType.OPEN_CHALLENGE_DECAY, game.getOpenChallengeDecayTime(), guildId);
		service.saveChallenge(challenge);
		addBotReply(String.format("Challenge is registered. I have sent you and %s a message.", bot.getPlayerName(acceptorId)));
	}
}
