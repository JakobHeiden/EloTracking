package com.elorankingbot.backend;

import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.service.DiscordBotService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import org.mockito.Answers;
import org.springframework.stereotype.Component;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Component
class IntegrationTestEventFactory {

	private final User ente;
	private final User ente2;
	private final DiscordBotService bot;
	long enteId;
	long ente2Id;
	long entenwieseId;

	IntegrationTestEventFactory(GatewayDiscordClient client, DiscordBotService bot, ApplicationPropertiesLoader propsLoader) {
		this.enteId = propsLoader.getOwnerId();
		this.ente2Id = propsLoader.getEnte2Id();
		this.entenwieseId = propsLoader.getEntenwieseId();
		this.ente = client.getUserById(Snowflake.of(enteId)).block();
		this.ente2 = client.getUserById(Snowflake.of(ente2Id)).block();
		this.bot = bot;
	}

	ChatInputInteractionEvent mockChallengeEvent() {
		ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class, Answers.RETURNS_DEEP_STUBS);
		when(event.getCommandName()).thenReturn("challenge");
		when(event.getInteraction().getGuildId().get().asLong()).thenReturn(entenwieseId);
		when(event.getOption("player").get().getValue().get().asUser().block()).thenReturn(ente2);
		when(event.getInteraction().getUser()).thenReturn(ente);
		return event;
	}

	ButtonInteractionEvent mockChallengeButtonEvent(
			ChallengeModel challenge, String commandString, boolean isChallengerButtonEvent) {
		ButtonInteractionEvent event = mock(ButtonInteractionEvent.class, Answers.RETURNS_DEEP_STUBS);
		if (isChallengerButtonEvent) {
			when(event.getInteraction().getChannelId().asLong()).thenReturn(challenge.getChallengerChannelId());
			when(event.getMessage().get()).thenReturn(bot.getMessageById(
					challenge.getChallengerChannelId(), challenge.getChallengerMessageId()).block());
		} else {
			when(event.getInteraction().getChannelId().asLong()).thenReturn(challenge.getAcceptorChannelId());
			when(event.getMessage().get()).thenReturn(bot.getMessageById(
					challenge.getAcceptorChannelId(), challenge.getAcceptorMessageId()).block());
		}
		when(event.getCustomId()).thenReturn(commandString + ":" + challenge.getId());
		return event;
	}
}
