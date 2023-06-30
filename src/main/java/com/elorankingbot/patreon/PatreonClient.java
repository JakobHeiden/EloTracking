package com.elorankingbot.patreon;

import com.elorankingbot.commands.Patreon;
import com.elorankingbot.model.Server;
import com.elorankingbot.patreon.model.PatreonDataGsonModel;
import com.elorankingbot.service.ChannelManager;
import com.elorankingbot.service.DBService;
import com.elorankingbot.service.DiscordBotService;
import com.elorankingbot.service.Services;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@CommonsLog
@Component
public class PatreonClient {

    public enum PatreonTier {
        FREE, SUPPORTER
    }

    private String clientId, clientSecret, redirectUri;
    private final DBService dbService;
    private final DiscordBotService bot;
    private final ChannelManager channelManager;
    @Getter
    private final int supporterMinPledgeInDollars;
    private final WebClient webClient;
    private final Gson gson;
    private static final String requestAccessTokenUrlTemplate = "https://www.patreon.com/api/oauth2/token" +
            "?code=%s" +
            "&grant_type=authorization_code" +
            "&client_id=%s" +
            "&client_secret=%s" +
            "&redirect_uri=%s";

    public PatreonClient(Services services, WebClient webClient) {
        this.clientId = System.getenv("PATREON_CLIENT_ID");
        this.clientSecret = System.getenv("PATREON_CLIENT_SECRET");
        this.redirectUri = services.props.getPatreon().getRedirectUri();
        this.supporterMinPledgeInDollars = services.props.getPatreon().getSupporterMinPledgeInDollars();
        this.dbService = services.dbService;
        this.bot = services.bot;
        this.channelManager = services.channelManager;
        this.webClient = webClient;
        this.gson = new Gson();
    }

    public String processRedirect(String code, String state) {
        long userId = Long.parseLong(state.split("-")[0]);
        long guildId = Long.parseLong(state.split("-")[1]);
        Patron patron = new Patron(userId, requestAccessToken(code));
        Server server = dbService.getOrCreateServer(guildId);
        int pledgedInCents = processUpdateToPatron(patron, server);

        String userTag = bot.getUser(patron.getUserId()).getTag();
        bot.sendToOwner(String.format("New pledge by %s : %s", userTag, pledgedInCents));
        return "You have successfully linked your Patreon account to your Discord account, " + userTag + "!<br>"
                + String.format(Patreon.currentPledgeSummaryTemplate,
                centsAsDollars(pledgedInCents), "<br>",
                centsAsDollars(calculateTotalPledgedCents(server)), "<br>",
                calculatePatreonTier(server).name())
                + "<br>Run /patreon any time you update your pledge." +
                "<br>You can close this window now.";
    }

    public int processUpdateToPatron(Patron patron, Server server) {
        int pledgeInCents = requestPledgeInCents(patron.getAccessToken());
        patron.setPledgeInCents(pledgeInCents);
        dbService.savePatron(patron);
        server.getPatronIds().add(patron.getUserId());
        dbService.saveServer(server);

        server.getGames().forEach(channelManager::refreshLeaderboard);

        return pledgeInCents;
    }

    private String requestAccessToken(String code) {
        return webClient.post()
                .uri(String.format(requestAccessTokenUrlTemplate, code, clientId, clientSecret, redirectUri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .retrieve()
                .bodyToMono(String.class)
                .block()
                .split("\"access_token\": \"")[1].split("\",")[0];
    }

    private int requestPledgeInCents(String accessToken) {
        String patreonUrl = "https://www.patreon.com/api/oauth2/v2/identity?include=memberships&fields[member]=currently_entitled_amount_cents";
        String responseBody = webClient.get()
                .uri(patreonUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        PatreonDataGsonModel test = gson.fromJson(responseBody, PatreonDataGsonModel.class);
        return test.getPledgedCents();
    }

    public PatreonTier calculatePatreonTier(Server server) {
        if (calculateTotalPledgedCents(server) < supporterMinPledgeInDollars) return PatreonTier.FREE;
        else return PatreonTier.SUPPORTER;
    }

    public int calculateTotalPledgedCents(Server server) {
        int totalPledgedCents = 0;
        for (long patronId : server.getPatronIds()) {
            Patron patron = dbService.findPatron(patronId).get();
            totalPledgedCents += patron.getPledgeInCents();
        }
        return totalPledgedCents;
    }

    public String centsAsDollars(int cents) {
        return cents % 100 == 0 ?
                String.valueOf(cents / 100) :
                String.format("%.2f", cents / 100.0) + "$";
    }
}