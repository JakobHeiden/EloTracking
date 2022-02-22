package com.elorankingbot.backend.commands.dispute;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.Buttons;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;

public abstract class ButtonCommandRelatedToDispute extends ButtonCommand {

	protected TextChannel disputeChannel;
	protected String moderatorName;
	protected Message challengerMessage;
	protected Message acceptorMessage;
	protected final ChallengeModel challenge;
	protected final long guildId;
	protected final Game game;

	protected ButtonCommandRelatedToDispute(ButtonInteractionEvent event, Services services) {
		super(event, services);
		this.challenge = null;//service.findChallengeById(Long.parseLong(event.getCustomId().split(":")[1])).get();
		this.guildId = 0;// challenge.getGuildId();
		this.game = service.findGameByGuildId(guildId).get();
		this.disputeChannel = (TextChannel) event.getInteraction().getChannel().block();
		this.moderatorName = event.getInteraction().getUser().getUsername();
		this.challengerMessage = client.getMessageById(Snowflake.of(challenge.getChallengerChannelId()),
				Snowflake.of(challenge.getChallengerMessageId())).block();
		this.acceptorMessage = client.getMessageById(Snowflake.of(challenge.getAcceptorChannelId()),
				Snowflake.of(challenge.getAcceptorMessageId())).block();
	}

	protected boolean isByModeratorOrAdmin() {
		boolean result = true;// (event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getAdminRoleId()))
//				|| event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getModRoleId())));
		if (!result) event.reply("Only a Moderator can use this.").withEphemeral(true).subscribe();
		return result;
	}

	protected void postToDisputeChannel(String text) {
		event.getInteraction().getMessage().get().edit()
				.withComponents(none).subscribe();
		disputeChannel.createMessage(text)
				.withComponents(ActionRow.of(
						Buttons.closeChannelNow(),
						Buttons.closeChannelLater()
				)).subscribe();
	}

	protected void addMatchSummarizeToQueue(MatchResult matchResult) {
//		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
//				challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), match);
//		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
//				challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), match);
	}

	protected void updateChallengerMessageIdAndSaveChallenge(Message message) {
		challenge.setChallengerMessageId(message.getId().asLong());
		service.saveChallenge(challenge);
	}

	protected void updateAcceptorMessageIdAndSaveChallenge(Message message) {
		challenge.setAcceptorMessageId(message.getId().asLong());
		service.saveChallenge(challenge);
	}
}
