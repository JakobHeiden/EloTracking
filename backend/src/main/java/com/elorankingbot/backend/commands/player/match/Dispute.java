package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.components.Buttons;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EmbedBuilder;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.TextChannelEditMono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.elorankingbot.backend.service.DiscordBotService.*;

public class Dispute extends ButtonCommandRelatedToMatch {

	private TextChannel matchChannel;
	private TextChannel disputeChannel;

	public Dispute(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		if (!activeUserIsInvolvedInMatch() || match.isDispute()) {
			event.acknowledge().subscribe();
			return;
		}

		event.getInteraction().getMessage().get().edit().withComponents(none).subscribe();
		match.setDispute(true);
		dbService.saveMatch(match);
		matchChannel = (TextChannel) event.getInteraction().getChannel().block();
		disputeChannel = bot.createDisputeChannel(match).block();
		sendDisputeLinkMessage();
		createDisputeMessage();
		event.acknowledge().subscribe();
	}

	public static TextChannelEditMono makeMatchChannelVisibleToMods(DiscordBotService bot, TextChannel channel, Match match) {
		Server server = match.getServer();
		List<PermissionOverwrite> permissionOverwrites = bot.excludePublic(server);
		match.getPlayers().forEach(player -> permissionOverwrites.add(allowPlayerView(player)));
		return channel.edit().withPermissionOverwrites(permissionOverwrites);
	}

	private void sendDisputeLinkMessage() {
		EmbedCreateSpec embed = EmbedCreateSpec.builder()
				.title(String.format("%s filed a dispute. For resolution please follow the link:",
						activeUser.getTag()))
				.description(disputeChannel.getMention()).build();
		matchChannel.createMessage(embed).subscribe(message -> message.pin().subscribe());
	}

	private void createDisputeMessage() {
		String embedTitle = "The reporting at the moment the dispute was filed:";
		EmbedCreateSpec embed = EmbedBuilder.createMatchEmbed(embedTitle, match);
		disputeChannel.createMessage(String.format("""
								Welcome %s. %s filed a dispute.
								Only <@&%s> and affected players can view this channel.
								Please state your view of the conflict so a moderator can resolve it.
								The original match channel can be found here: <#%s>
								Note that the Buttons on this message can only be used by moderators.
								""",
						match.getAllMentions(),
						activeUser.getTag(),
						server.getModRoleId(),
						match.getChannelId()))
				.withEmbeds(embed)
				.withComponents(createActionRow(match))
				.subscribe();
	}

	public static ActionRow createActionRow(Match match) {
		int numTeams = match.getTeams().size();
		UUID matchId = match.getId();
		List<ActionComponent> buttons = new ArrayList<>(numTeams);
		for (int i = 0; i < numTeams; i++) {
			buttons.add(Buttons.ruleAsWin(matchId, i));
		}
		if (match.getGame().isAllowDraw()) buttons.add(Buttons.ruleAsDraw(matchId));
		buttons.add(Buttons.ruleAsCancel(matchId));
		return ActionRow.of(buttons);
	}
}
