package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.Buttons;
import com.elorankingbot.backend.tools.EmbedBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Dispute extends ButtonCommandRelatedToMatch {

	private TextChannel disputeChannel;

	public Dispute(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		match.setDispute(true);
		dbService.saveMatch(match);

		createDisputeChannel();
		updateMatchMessages();
		createDisputeMessage();
		event.acknowledge().subscribe();
	}

	private void createDisputeChannel() {
		List<PermissionOverwrite> permissionOverwrites = new ArrayList<>(match.getNumPlayers());
		match.getPlayers().forEach(player -> permissionOverwrites.add(PermissionOverwrite.forMember(
				Snowflake.of(player.getUserId()),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none())));
		Category disputeCategory = (Category) bot.getChannelById(server.getDisputeCategoryId()).block();
		permissionOverwrites.addAll(disputeCategory.getPermissionOverwrites());
		try {
			disputeChannel = client.getGuildById(Snowflake.of(game.getGuildId())).block()
					.createTextChannel("Open Dispute")
					.withParentId(Snowflake.of(server.getDisputeCategoryId()))
					.withPermissionOverwrites(permissionOverwrites)
					.block();
		} catch (ClientException e) {
			createDisputeCategory();
			createDisputeChannel();
		}
	}

	private Category createDisputeCategory() {
		Guild guild = bot.getGuildById(guildId).block();
		Category disputeCategory = guild.createCategory("elo disputes").withPermissionOverwrites(
				PermissionOverwrite.forRole(guild.getId(), PermissionSet.none(),
						PermissionSet.of(Permission.VIEW_CHANNEL)),
				PermissionOverwrite.forRole(Snowflake.of(server.getAdminRoleId()), PermissionSet.of(Permission.VIEW_CHANNEL),
						PermissionSet.none()),
				PermissionOverwrite.forRole(Snowflake.of(server.getModRoleId()), PermissionSet.of(Permission.VIEW_CHANNEL),
						PermissionSet.none())).block();
		server.setDisputeCategoryId(disputeCategory.getId().asLong());
		dbService.saveServer(server);// TODO! ?
		return disputeCategory;
	}

	private void updateMatchMessages() {
		for (Player player : match.getPlayers()) {
			bot.getPlayerMessage(player, match)
					.subscribe(message -> {
						boolean isActiveUserMessage = player.getUserId() == activeUserId;
						String embedTitle = String.format("%s filed a dispute. For resolution please follow the link above",
								isActiveUserMessage ? "You" : activeUser.getTag());
						EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(embedTitle, match, player.getTag());
						message.edit().withContent(disputeChannel.getMention())// TODO! die mention in einem feld im embed unterbringen
								.withEmbeds(embedCreateSpec).withComponents(none).subscribe();
					});
		}
	}


	private void createDisputeMessage() {
		String embedTitle = String.format("%s filed a dispute",	activeUser.getTag());
		EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(embedTitle, match, null);
		disputeChannel.createMessage(String.format("Welcome everyone. Only Moderators and affected players can view " +
						"this channel. Please state your view of the conflict so a moderator can resolve it. " +
						"Note that the Buttons in this channel can only be used by <@&%s>.", server.getModRoleId()))
				.withEmbeds(embedCreateSpec)
				.withComponents(createActionRow())
				.subscribe();
	}

	private ActionRow createActionRow() {
		int numTeams = match.getTeams().size();
		UUID matchId = match.getId();
		List<ActionComponent> buttons = new ArrayList<>(numTeams);
		for (int i = 0; i < numTeams; i++) {
			buttons.add(Buttons.ruleAsWin(matchId, i));
		}
		if (game.isAllowDraw())	buttons.add(Buttons.ruleAsDraw(matchId));
		buttons.add(Buttons.ruleAsCancel(matchId));
		return ActionRow.of(buttons);
	}
}
