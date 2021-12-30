package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Buttons;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;

public abstract class ButtonCommandForDispute extends ButtonCommand {

	protected ChallengeModel challenge;
	protected Game game;
	protected TextChannel disputeChannel;
	protected String moderatorName;
	protected Message challengerMessage;
	protected Message acceptorMessage;


	protected ButtonCommandForDispute(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);

		this.challenge = service.getChallengeByChallengerMessageId(Long.parseLong(
				event.getCustomId().split(":")[1])).get();
		this.game = service.findGameByGuildId(event.getInteraction().getGuildId().get().asLong()).get();
		this.disputeChannel = (TextChannel) event.getInteraction().getChannel().block();
		this.moderatorName = event.getInteraction().getUser().getUsername();
		this.challengerMessage = client.getMessageById(Snowflake.of(challenge.getChallengerChannelId()),
				Snowflake.of(challenge.getChallengerMessageId())).block();
		this.acceptorMessage = client.getMessageById(Snowflake.of(challenge.getAcceptorChannelId()),
				Snowflake.of(challenge.getAcceptorMessageId())).block();
	}

	protected boolean isByModeratorOrAdmin() {
		boolean result = (event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getAdminRoleId()))
				|| event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getModRoleId())));
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

	protected void addMatchSummarizeToQueue(Match match) {
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				challengerMessage.getId().asLong(), challengerMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				acceptorMessage.getId().asLong(), acceptorMessage.getChannelId().asLong(), match);
	}
}
