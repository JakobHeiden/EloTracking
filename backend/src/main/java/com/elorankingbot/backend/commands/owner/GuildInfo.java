package com.elorankingbot.backend.commands.owner;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

public class GuildInfo extends SlashCommand {

	public GuildInfo(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(GuildInfo.class.getSimpleName().toLowerCase())
				.description("GuildInfo")
				.defaultPermission(false)
				.addOption(ApplicationCommandOptionData.builder()
						.name("guildid")
						.description("GuildId")
						.type(STRING.getValue())
						.required(true)
						.build())
				.build();
	}

	protected void execute() {
		try {
			long guildId = Long.parseLong(event.getOption("guildid").get().getValue().get().asString());
			Guild guild = bot.getGuildById(guildId).block();
			event.reply(guild.getName()).withEphemeral(true).subscribe();
		} catch (Exception e) {
			event.reply(e.getMessage()).withEphemeral(true).subscribe();
		}
	}
}
