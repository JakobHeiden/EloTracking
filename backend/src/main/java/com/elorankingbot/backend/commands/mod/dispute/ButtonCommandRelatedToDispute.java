package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.Buttons;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.channel.TextChannel;

import java.util.UUID;

public abstract class ButtonCommandRelatedToDispute extends ButtonCommand {

	protected String moderatorName;
	protected final Match match;
	protected final Server server;

	protected ButtonCommandRelatedToDispute(ButtonInteractionEvent event, Services services) {
		super(event, services);
		UUID matchId = UUID.fromString(event.getCustomId().split(":")[1]);
		this.match = dbservice.getMatch(matchId);
		this.server = match.getServer();
		this.moderatorName = event.getInteraction().getUser().getTag();
		/*
		this.guildId = match.getGame().getServer().getGuildId();
		this.game = dbservice.findGameByGuildId(guildId).get();
		this.disputeChannel = (TextChannel) event.getInteraction().getChannel().block();

		 */
	}

	protected boolean isByModeratorOrAdminDoReply() {
		boolean result = event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getAdminRoleId()))
				|| event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getModRoleId()));
		if (!result) event.reply("Only a Moderator can use this.").withEphemeral(true).subscribe();
		return result;
	}

	protected void postToDisputeChannelAndUpdateButtons(String text) {
		event.getInteraction().getMessage().get().edit()
				.withComponents(none).subscribe();
		event.getInteraction().getChannel().subscribe(messageChannel ->
				messageChannel.createMessage(text)
						.withComponents(ActionRow.of(
								Buttons.closeChannelNow(),
								Buttons.closeChannelLater()
						)).subscribe());
	}

	protected void addMatchSummarizeToQueue(MatchResult matchResult) {
//		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
//				challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), match);
//		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
//				challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), match);
	}

	/*
	protected void updateChallengerMessageIdAndSaveChallenge(Message message) {
		challenge.setChallengerMessageId(message.getId().asLong());
		dbservice.saveChallenge(challenge);
	}

	protected void updateAcceptorMessageIdAndSaveChallenge(Message message) {
		challenge.setAcceptorMessageId(message.getId().asLong());
		dbservice.saveChallenge(challenge);
	}

	 */
}
