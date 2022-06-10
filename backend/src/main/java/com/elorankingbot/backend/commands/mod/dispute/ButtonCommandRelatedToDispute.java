package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
public abstract class ButtonCommandRelatedToDispute extends ButtonCommand {

	protected String moderatorTag;
	protected final Match match;
	protected final Server server;

	protected ButtonCommandRelatedToDispute(ButtonInteractionEvent event, Services services) {
		super(event, services);
		UUID matchId = UUID.fromString(event.getCustomId().split(":")[1]);
		this.match = dbService.getMatch(matchId);
		this.server = match.getServer();
		this.moderatorTag = event.getInteraction().getUser().getTag();
	}

	@Override
	public void doExecute() {
		boolean isByAdmin = event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getAdminRoleId()));
		boolean isByMod = event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getModRoleId()));
		boolean isAdjudicatingOwnMatch = match.containsPlayer(Player.generateId(server.getGuildId(),
				event.getInteraction().getUser().getId().asLong()));
		if (!isByAdmin && !isByMod) {
			event.reply("Only a Moderator can use this.").withEphemeral(true).subscribe();
			return;
		}
		if (isByMod && isAdjudicatingOwnMatch && !isByAdmin) {
			event.reply("You cannot adjudicate your own match.").withEphemeral(true).subscribe();
			return;
		}

		log.debug(String.format("execute %s by %s on %s",
				this.getClass().getSimpleName(),
				event.getInteraction().getUser().getTag(),
				event.getInteraction().getGuild().block().getName()));
		execute();
	}

	protected Mono<Message> postToDisputeChannel(String text) {
		return event.getInteraction().getChannel().flatMap(messageChannel -> messageChannel.createMessage(text));
	}

	protected void removeButtons() {
		event.getInteraction().getMessage().get().edit()
				.withComponents(none).subscribe();
	}

}
