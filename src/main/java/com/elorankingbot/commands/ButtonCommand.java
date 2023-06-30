package com.elorankingbot.commands;

import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class ButtonCommand extends Command {

	protected final ButtonInteractionEvent event;

	protected static final List none = new ArrayList<>();

	protected ButtonCommand(ButtonInteractionEvent event, Services services) {
		super(event, services);
		this.event = event;
	}
}
