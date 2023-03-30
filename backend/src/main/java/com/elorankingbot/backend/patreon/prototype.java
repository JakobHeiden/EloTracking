package com.elorankingbot.backend.patreon;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.patreon.PatreonAPI;
import com.patreon.PatreonOAuth;
import com.patreon.resources.User;
import com.patreon.resources.Pledge;

import lombok.extern.apachecommons.CommonsLog;

import java.io.IOException;
import java.util.List;

@CommonsLog
public class prototype {

    public static String doStuff(String code) {
        try {
            log.info("doStuff");
            String clientId = "9dkSNb9DssBnmdlFGv9oiVnEUHTAt5qohN3eT6EvZ4-PFSRMkcOx2dYMNzriLjr4";
            String clientSecret = "xHRqmhMGuwLSnIkPVCaDMfm4SDjPjQqehA07HVUqzwMvANg1Qsbok1R5VKpwMtnG";
            String redirectUri = "http://45.77.53.94:8080/patreon-redirect";

            PatreonOAuth oauthClient = new PatreonOAuth(clientId, clientSecret, redirectUri);
            PatreonOAuth.TokensResponse tokens = oauthClient.getTokens(code);
            //Store the refresh TokensResponse in your data store
            String accessToken = tokens.getAccessToken();

            PatreonAPI apiClient = new PatreonAPI(accessToken);
            JSONAPIDocument<User> userResponse = null;
            userResponse = apiClient.fetchUser();

            User user = userResponse.get();
            log.info(user.getFullName());
            List<Pledge> pledges = user.getPledges();
            log.info("numPledges = " + pledges.size());
            if (pledges != null && pledges.size() > 0) {
                Pledge pledge = pledges.get(0);
                log.info(pledge.getAmountCents());
            }
            return String.valueOf(pledges.size());
        } catch (Exception e) {
            log.error("catch " + e.getMessage());
            return "catch " + e.getMessage();
        }
// You should save the user's PatreonOAuth.TokensResponse in your database
// (for refreshing their Patreon data whenever you like),
// along with any relevant user info or pledge info you want to store.
    }
}
