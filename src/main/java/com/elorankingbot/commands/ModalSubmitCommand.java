package com.elorankingbot.commands;

import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;

public abstract class ModalSubmitCommand extends Command {

    protected final ModalSubmitInteractionEvent event;

    protected ModalSubmitCommand(ModalSubmitInteractionEvent event, Services services) {
        super(event, services);
        this.event = event;
        System.out.println(event.getCustomId());// TODO!
    }
}
