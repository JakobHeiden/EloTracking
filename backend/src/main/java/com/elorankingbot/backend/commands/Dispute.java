package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.tools.Buttons;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.tools.MessageUpdater;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

public class Dispute extends ButtonCommandRelatedToChallenge {

	private TextChannel disputeChannel;
	private String parentMessageContent;
	private String targetMessageContent;
	private String challengerName;
	private String acceptorName;

	public Dispute(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot,
				   TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		challenge.setDispute(true);
		service.saveChallenge(challenge);

		challengerName = bot.getPlayerName(challenge.getChallengerId());
		acceptorName = bot.getPlayerName(challenge.getAcceptorId());

		createDisputeChannel();
		editChallengeMessages();
		createDisputeMessage();
		event.acknowledge().subscribe();
	}

	private void editChallengeMessages() {
		parentMessageContent = new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine(String.format("You filed a dispute :exclamation:. For resolution, please go to <#%s>.",
						disputeChannel.getId().asString()))
				.update()
				.withComponents(none).block()
				.getContent();
		targetMessageContent = new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.addLine(String.format("Your opponent filed a dispute :exclamation:. For resolution, please go to <#%s>.",
						disputeChannel.getId().asString()))
				.update()
				.withComponents(none).block()
				.getContent();
	}

	private void createDisputeChannel() {
		disputeChannel = client.getGuildById(Snowflake.of(game.getGuildId())).block()
				.createTextChannel(String.format("%s vs %s", challengerName, acceptorName))
				.withParentId(Snowflake.of(game.getDisputeCategoryId()))
				.withPermissionOverwrites(
						PermissionOverwrite.forMember(
								Snowflake.of(challenge.getChallengerId()),
								PermissionSet.of(Permission.VIEW_CHANNEL),
								PermissionSet.none()),
						PermissionOverwrite.forMember(
								Snowflake.of(challenge.getAcceptorId()),
								PermissionSet.of(Permission.VIEW_CHANNEL),
								PermissionSet.none()))
				.block();
	}

	private void createDisputeMessage() {
		disputeChannel.createMessage(String.format("Welcome everyone. Please use this channel to sort out your dispute. " +
						"Only Moderators and affected parties can view this channel. " +
						"Note that the Buttons in this channel can only be used by <@&%s>.", game.getModRoleId()))
				.withEmbeds(EmbedCreateSpec.builder()
						.addField(EmbedCreateFields.Field.of(
								"My message with " + challengerName,
								isChallengerCommand ? parentMessageContent : targetMessageContent,
								true))
						.addField(EmbedCreateFields.Field.of(
								"My message with " + acceptorName,
								isChallengerCommand ? targetMessageContent : parentMessageContent,
								true))
						.build())
				.withComponents(createActionRow(game.isAllowDraw()))
				.subscribe();
	}

	private ActionRow createActionRow(boolean allowDraw) {
		if (allowDraw) return ActionRow.of(
				Buttons.ruleAsWin(challenge.getChallengerMessageId(), true, challengerName,
						challenge.getChallengerChannelId(), challenge.getAcceptorChannelId()),
				Buttons.ruleAsWin(challenge.getChallengerMessageId(), false, acceptorName,
						challenge.getChallengerChannelId(), challenge.getAcceptorChannelId()),
				Buttons.ruleAsDraw(challenge.getChallengerMessageId()),
				Buttons.ruleAsCancel(challenge.getChallengerMessageId()));
		else return ActionRow.of(
				Buttons.ruleAsWin(challenge.getChallengerMessageId(), true, challengerName,
						challenge.getChallengerChannelId(), challenge.getAcceptorChannelId()),
				Buttons.ruleAsWin(challenge.getChallengerMessageId(), false, acceptorName,
						challenge.getChallengerChannelId(), challenge.getAcceptorChannelId()),
				Buttons.ruleAsCancel(challenge.getChallengerMessageId()));
	}
}
