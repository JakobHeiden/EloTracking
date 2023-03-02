package com.elorankingbot.backend.commands.owner;

import com.elorankingbot.backend.command.annotations.OwnerCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.http.client.ClientException;

import java.util.*;

@OwnerCommand
public class AllGuilds extends SlashCommand {

    public AllGuilds(ChatInputInteractionEvent event, Services services) {
        super(event, services);
    }

    public static ApplicationCommandRequest getRequest() {
        return ApplicationCommandRequest.builder()
                .name(AllGuilds.class.getSimpleName().toLowerCase())
                .description(AllGuilds.class.getSimpleName())
                .defaultPermission(true)
                .build();
    }

    protected void execute() throws Exception {
        event.deferReply().withEphemeral(true).subscribe();
        try {
            StringBuilder reply = new StringBuilder();
            int numGuilds = 0;
            for (Server server : dbService.findAllServers()) {
                try {
                    Guild guild = bot.getGuild(server).block();
                    numGuilds++;
                    reply.append(String.format("%s - %s:%s\n", guild.getMemberCount(), guild.getId().asString(), guild.getName()));
                    if (reply.length() > 1800) {// 2000 is limit
                        event.createFollowup(reply.toString()).subscribe();
                        reply = new StringBuilder();
                    }
                } catch (ClientException ignored) {// guilds that the server has lost access to end up here
                }
            }
            event.createFollowup(reply.append("Total number of guilds: " + numGuilds).toString()).subscribe();
        } catch (Exception e) {
            e.printStackTrace();
            event.createFollowup(e.getMessage()).subscribe(NO_OP, super::forwardToExceptionHandler);
        }
    }
}
