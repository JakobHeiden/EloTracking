package com.elorankingbot.commands.owner;

import com.elorankingbot.command.annotations.OwnerCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

@OwnerCommand
public class GuildInfo extends SlashCommand {

	public GuildInfo(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		return ApplicationCommandRequest.builder()
				.name(GuildInfo.class.getSimpleName().toLowerCase())
				.description("GuildInfo")
				.defaultPermission(true)
				.addOption(ApplicationCommandOptionData.builder()
						.name("guildid")
						.description("GuildId")
						.type(STRING.getValue())
						.required(true)
						.build())
				.build();
	}

	protected void execute() throws Exception {
		event.deferReply().subscribe();
		try {
			long guildId = Long.parseLong(event.getOption("guildid").get().getValue().get().asString());
			Guild guild = bot.getGuild(guildId).block();
			String reply = String.format("%s:%s:%s\n", guildId, guild.getName(), guild.getMemberCount());
			/* The new bot account does not have permissions for invites. TODO maybe remove at some point
			List<ExtendedInvite> invites = guild.getInvites().buffer().blockLast();
			if (invites != null) {
				for (ExtendedInvite invite : invites) {
					reply += "\nhttps://discord.gg/" + invite.getCode();
				}
			}
			 */
			event.createFollowup(reply)
					.withEphemeral(true)
					.subscribe(NO_OP, super::forwardToExceptionHandler);
		} catch (NumberFormatException e) {
			event.createFollowup("That's not a number").withEphemeral(true).subscribe();
		} catch (Exception e) {
			event.createFollowup(e.getMessage()).withEphemeral(true)
					.subscribe(NO_OP, super::forwardToExceptionHandler);
		}
	}
}
