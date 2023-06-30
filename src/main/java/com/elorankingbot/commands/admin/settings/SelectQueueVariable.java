package com.elorankingbot.commands.admin.settings;

import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.commands.SelectMenuCommand;
import com.elorankingbot.model.MatchFinderQueue;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;

import java.util.List;

@AdminCommand
public class SelectQueueVariable extends SelectMenuCommand {

    public static final String customId = SelectQueueVariable.class.getSimpleName().toLowerCase();

    public SelectQueueVariable(SelectMenuInteractionEvent event, Services services) {
        super(event, services);
    }

    static ActionRow menu(MatchFinderQueue queue) {
        List<SelectMenu.Option> queueOptions = List.of(
                menuOption(queue, "Name"),
                menuOption(queue, "K"),
                menuOption(queue, "max-rating-spread"),
                menuOption(queue, "rating-elasticity"));
        return ActionRow.of(SelectMenu.of(SelectQueueVariable.customId, queueOptions)
                .withPlaceholder("Select a setting to edit"));
    }

    private static SelectMenu.Option menuOption(MatchFinderQueue queue, String variable) {
        return SelectMenu.Option.of(variable, String.format("%s,%s:%s", queue.getGame().getName(), queue.getName(), variable));
    }

    protected void execute() throws Exception {
        QueueVariableModal.present(server, event);
    }
}
