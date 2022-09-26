package com.elorankingbot.backend.commands.owner;

import com.elorankingbot.backend.command.annotations.OwnerCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.ExtendedInvite;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

@OwnerCommand
public class GuildInfo extends SlashCommand {

	public GuildInfo(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
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
		try {
			long guildId = Long.parseLong(event.getOption("guildid").get().getValue().get().asString());
			Guild guild = bot.getGuildById(guildId).block();
			String reply = String.format("%s:%s:%s\n", guildId, guild.getName(), guild.getMemberCount());
			List<ApplicationCommandData> guildCommands = discordCommandService.getAllGuildCommands(guildId).block();
			for (ApplicationCommandData guildCommand : guildCommands) {
				reply += guildCommand.name() + ", ";
			}
			List<ExtendedInvite> invites = guild.getInvites().buffer().blockLast();
			if (invites != null) {
				for (ExtendedInvite invite : invites) {
					reply += "\nhttps://discord.gg/" + invite.getCode();
				}
			}
			event.reply(reply).withEphemeral(true).doOnError(super::handleExceptionCallback).subscribe();
		} catch (NumberFormatException e) {
			event.reply("That's not a number").withEphemeral(true).subscribe();
		} catch (Exception e) {
			event.reply(e.getMessage()).withEphemeral(true)
					.doOnError(super::handleExceptionCallback).subscribe();
		}
	}
}
