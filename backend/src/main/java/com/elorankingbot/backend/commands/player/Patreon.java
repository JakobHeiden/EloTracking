package com.elorankingbot.backend.commands.player;

import com.elorankingbot.backend.command.annotations.PlayerCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.Services;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.patreon.PatreonAPI;
import com.patreon.PatreonOAuth;
import com.patreon.resources.Pledge;
import com.patreon.resources.User;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;

import java.util.List;

@PlayerCommand
public class Patreon extends SlashCommand {

    private final String patreonUrl;

    public Patreon(ChatInputInteractionEvent event, Services services) {
        super(event, services);
        patreonUrl = services.props.getPatreon().getUrl();
    }

    public static ApplicationCommandRequest getRequest() {
        return ApplicationCommandRequest.builder()
                .name("patreon")
                .description(getShortDescription())
                .build();
    }

    public static String getShortDescription() {
        return "Display information about supporting the developer on Patreon.";
    }

    public static String getLongDescription() {
        return getShortDescription();
    }

    protected void execute() throws Exception {


        event.reply()
                .withEmbeds(createEmbed())
                .withComponents(createActionRow())
                .withEphemeral(true)
                .subscribe();
    }

    private EmbedCreateSpec createEmbed() {
        return EmbedCreateSpec.builder()
                .title("3$ will make the bot stop begging for Patreon money. Any amount will make the developer happy!")
                .color(Color.SUMMER_SKY)
                .build();
    }

    private ActionRow createActionRow() {
        return ActionRow.of(Button.link(patreonUrl, "Donate on Patreon"), linkAuth());
    }

    private static Button linkAuth() {
        String url = "https://www.patreon.com/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=9dkSNb9DssBnmdlFGv9oiVnEUHTAt5qohN3eT6EvZ4-PFSRMkcOx2dYMNzriLjr4" +
                "&redirect_uri=http://45.77.53.94:8080/patreon-redirect" +
                "&state=none";
        return Button.link(url, "placeholder");
    }

}
