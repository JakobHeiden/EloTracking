package com.elorankingbot.backend.patreon;

import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.patreon.PatreonAPI;
import com.patreon.PatreonOAuth;
import com.patreon.resources.User;
import com.patreon.resources.Pledge;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

import java.util.List;

@CommonsLog
@Component
public class PatreonClient {

    private PatreonOAuth oauthClient;
    private final boolean isDev;
    private final DiscordBotService bot;

    public PatreonClient(Services services) {
            String clientId = services.props.getPatreon().getClientId();
            String clientSecret = services.props.getPatreon().getClientSecret();
            String redirectUri = services.props.getPatreon().getRedirectUri();
            oauthClient = new PatreonOAuth(clientId, clientSecret, redirectUri);
            isDev = services.props.isUseDevBotToken();
            bot = services.bot;
    }

    public void doStuff(String code) {
        if (!isDev) {
            bot.sendToOwner(code);
            return;
        }

        try {
            PatreonOAuth.TokensResponse tokens = oauthClient.getTokens(code);
            //Store the refresh TokensResponse in your data store
            String accessToken = tokens.getAccessToken();

            PatreonAPI apiClient = new PatreonAPI(accessToken);
            JSONAPIDocument<User> userResponse = apiClient.fetchUser();

            User user = userResponse.get();
            log.info(user.getFullName());
            List<Pledge> pledges = user.getPledges();
            log.info("numPledges = " + pledges.size());
            if (pledges != null && pledges.size() > 0) {
                Pledge pledge = pledges.get(0);
                log.info(pledge.getAmountCents());
            }
        } catch (Exception e) {
            log.error("catch " + e.getMessage());
        }
// You should save the user's PatreonOAuth.TokensResponse in your database
// (for refreshing their Patreon data whenever you like),
// along with any relevant user info or pledge info you want to store.
    }
}
