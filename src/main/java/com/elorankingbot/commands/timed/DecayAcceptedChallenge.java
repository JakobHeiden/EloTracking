package com.elorankingbot.commands.timed;

import com.elorankingbot.service.Services;

public class DecayAcceptedChallenge extends TimedCommand {

	public DecayAcceptedChallenge(Services services, long challengeId, int time) {
		super(services, challengeId, time);;
	}

	public void execute() {
		/*
		if (challenge == null) return;
		if (challenge.hasAReport()) return;

		service.deleteChallengeById(relationId);
		Optional<Game> maybeGame = service.findGameByGuildId(challenge.getGuildId());
		if (maybeGame.isEmpty()) return;

		new MessageUpdater(challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), client)
				.makeAllNotBold()
				.addLine(String.format("This match has expired after not getting reports within %s minutes.", time))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), client)
				.makeAllNotBold()
				.addLine(String.format("This challenge has expired after not getting reports within %s minutes.", time))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();

		int timer = service.findGameByGuildId(challenge.getGuildId()).get().getMessageCleanupTime();
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, timer,
				challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, timer,
				challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), null);

		 */
	}
}
