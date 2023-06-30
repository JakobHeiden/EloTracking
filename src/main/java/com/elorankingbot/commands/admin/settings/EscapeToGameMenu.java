package com.elorankingbot.commands.admin.settings;

import com.elorankingbot.commands.ButtonCommand;
import com.elorankingbot.components.Emojis;
import com.elorankingbot.model.Game;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;

public class EscapeToGameMenu extends ButtonCommand {

    public static final String customId = EscapeToGameMenu.class.getSimpleName().toLowerCase();

    public EscapeToGameMenu(ButtonInteractionEvent event, Services services) {
        super(event, services);
    }

    static Button button(Game game) {
        return Button.secondary(customId + ":" + game.getName(), Emojis.redoArrow,
                String.format("Back to %s menu", game.getName()));
    }

    public void execute() {
        Game game = server.getGame(event.getCustomId().split(":")[1]);
        event.getMessage().get().edit()
                .withEmbeds(SelectServerVariableOrGame.gameSettingsEmbed(game))
                .withComponents(SelectGameVariableOrQueue.menu(game), ActionRow.of(Exit.button(), EscapeToMainMenu.button())).subscribe();
        acknowledgeEvent();
    }
}
