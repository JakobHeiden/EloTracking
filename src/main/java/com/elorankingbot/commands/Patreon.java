package com.elorankingbot.commands;

import com.elorankingbot.command.annotations.GlobalCommand;
import com.elorankingbot.command.annotations.PlayerCommand;
import com.elorankingbot.patreon.PatreonClient;
import com.elorankingbot.patreon.Patron;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PlayerCommand
@GlobalCommand
public class Patreon extends SlashCommand {

    private final String patreonCampaignUrl;
    private final PatreonClient patreonClient;
    private boolean hasLinkedPatreon, hasPledged;
    private static final String patreonAuthorizationUrlTemplate = "https://www.patreon.com/oauth2/authorize" +
            "?response_type=code" +
            "&client_id=9dkSNb9DssBnmdlFGv9oiVnEUHTAt5qohN3eT6EvZ4-PFSRMkcOx2dYMNzriLjr4" +
            "&redirect_uri=http://45.77.53.94:8080/patreon-redirect" +
            "&state=%s-%s";
    public static final String currentPledgeSummaryTemplate = "You have currently pledged %s$ per month." +
            "%sTotal pledges for this server: %s$ per month." +
            "%sCurrent server Patreon tier: %s";

    public Patreon(ChatInputInteractionEvent event, Services services) {
        super(event, services);
        patreonCampaignUrl = services.props.getPatreon().getCampaignUrl();
        patreonClient = services.patreonClient;
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
        Optional<Patron> maybePatron = dbService.findPatron(activeUserId);
        hasLinkedPatreon = maybePatron.isPresent();
        if (hasLinkedPatreon) patreonClient.processUpdateToPatron(maybePatron.get(), server);
        hasPledged = hasLinkedPatreon && maybePatron.get().getPledgeInCents() > 0;
        EmbedCreateSpec embed = hasPledged ? createAlreadyPatronEmbed(maybePatron.get().getPledgeInCents()) : createBegEmbed();
        event.reply()
                .withEmbeds(embed)
                .withComponents(createActionRow())
                .withEphemeral(true)
                .subscribe();
    }

    private EmbedCreateSpec createBegEmbed() {
        System.out.println(patreonClient.getSupporterMinPledgeInDollars());
        return EmbedCreateSpec.builder()
                .title(patreonClient.getSupporterMinPledgeInDollars() + "$ will make the bot stop begging for Patreon money. Any amount will make the developer happy!")
                .description(hasLinkedPatreon ? "To get supporter benefits, you need to make a pledge on Patreon."
                        : "To get supporter benefits, you need to both make a pledge on Patreon, " +
                        "as well as link your Discord account to your Patreon account.")
                .color(Color.SUMMER_SKY)
                .build();
    }

    private EmbedCreateSpec createAlreadyPatronEmbed(int pledgeAmountInCents) {
        return EmbedCreateSpec.builder()
                .title("Thank you for your support!")
                .description(String.format(currentPledgeSummaryTemplate,
                        patreonClient.centsAsDollars(pledgeAmountInCents), "\n",
                        patreonClient.centsAsDollars(patreonClient.calculateTotalPledgedCents(server)), "\n",
                        patreonClient.calculatePatreonTier(server).name()))
                .color(Color.SUMMER_SKY)
                .build();
    }

    private ActionRow createActionRow() {
        List<Button> buttons = new ArrayList<>(2);
        String patreonAuthorizationUrl = String.format(patreonAuthorizationUrlTemplate,
                event.getInteraction().getUser().getId().asString(),
                event.getInteraction().getGuildId().get().asString());
        if (!hasLinkedPatreon) buttons.add(Button.link(patreonAuthorizationUrl, "Link this Discord account to your Patreon account"));
        if (hasPledged) buttons.add(Button.link(patreonCampaignUrl, "Review your pledge on Patreon"));
        else buttons.add(Button.link(patreonCampaignUrl, "Make a pledge on Patreon"));
        return ActionRow.of(buttons);
    }
}
