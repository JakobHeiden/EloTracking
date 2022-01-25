package com.elorankingbot.backend.commands.timed;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;

import java.util.Optional;

public class DecayAcceptedChallenge extends TimedCommand {

	public DecayAcceptedChallenge(EloRankingService service, DiscordBotService bot, GatewayDiscordClient client,
								  TimedTaskQueue queue, long challengeId, int time) {
		super(service, bot, client, queue, challengeId, time);
	}

	public void execute() {
		Optional<ChallengeModel> maybeChallenge = service.findChallengeById(relationId);
		if (maybeChallenge.isEmpty()) return;

		ChallengeModel challenge = maybeChallenge.get();
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
	}
}
