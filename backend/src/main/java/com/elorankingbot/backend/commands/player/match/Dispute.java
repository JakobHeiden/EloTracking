package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.channel.TextChannel;

public class Dispute extends ButtonCommandRelatedToMatch {

	private TextChannel disputeChannel;
	private String parentMessageContent;
	private String targetMessageContent;
	private String challengerTag;
	private String acceptorTag;

	public Dispute(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		/*
		challenge.setDispute(true);
		dbservice.saveChallenge(challenge);

		challengerTag = challenge.getChallengerTag();
		acceptorTag = challenge.getAcceptorTag();

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
		Message resentTargetMessage = new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.addLine(String.format("Your opponent filed a dispute :exclamation:. For resolution, please go to <#%s>.",
						disputeChannel.getId().asString()))
				.resend()
				.withComponents(none).block();
		super.updateAndSaveChallenge(resentTargetMessage);
		targetMessageContent = targetMessage.getContent();
	}

	private void createDisputeChannel() {
		disputeChannel = client.getGuildById(Snowflake.of(game.getGuildId())).block()
				.createTextChannel(String.format("%s vs %s", challengerTag, acceptorTag))
				//.withParentId(Snowflake.of(game.getDisputeCategoryId()))
				.withPermissionOverwrites(
						PermissionOverwrite.forMember(
								Snowflake.of(challenge.getChallengerUserId()),
								PermissionSet.of(Permission.VIEW_CHANNEL),
								PermissionSet.none()),
						PermissionOverwrite.forMember(
								Snowflake.of(challenge.getAcceptorUserId()),
								PermissionSet.of(Permission.VIEW_CHANNEL),
								PermissionSet.none()))
				.block();
	}

	private void createDisputeMessage() {
		disputeChannel.createMessage(String.format("Welcome everyone. Please use this channel to sort out your dispute. " +
						"Only Moderators and affected parties can view this channel. " +
						"Note that the Buttons in this channel can only be used by <@&%s>."))//, game.getModRoleId()))
				.withEmbeds(EmbedCreateSpec.builder()
						.addField(EmbedCreateFields.Field.of(
								"My message with " + challengerTag,
								isChallengerCommand ? parentMessageContent : targetMessageContent,
								true))
						.addField(EmbedCreateFields.Field.of(
								"My message with " + acceptorTag,
								isChallengerCommand ? targetMessageContent : parentMessageContent,
								true))
						.build())
				//.withComponents(createActionRow(game.isAllowDraw()))
				.subscribe();

		 */
	}

	private ActionRow createActionRow(boolean allowDraw) {
		/*
		if (allowDraw) return ActionRow.of(
				Buttons.ruleAsWin(challenge.getId(), true, challengerTag),
				Buttons.ruleAsWin(challenge.getId(), false, acceptorTag),
				Buttons.ruleAsDraw(challenge.getId()),
				Buttons.ruleAsCancel(challenge.getId()));
		else return ActionRow.of(
				Buttons.ruleAsWin(challenge.getId(), true, challengerTag),
				Buttons.ruleAsWin(challenge.getId(), false, acceptorTag),
				Buttons.ruleAsCancel(challenge.getId()));

		 */
		return null;
	}
}
