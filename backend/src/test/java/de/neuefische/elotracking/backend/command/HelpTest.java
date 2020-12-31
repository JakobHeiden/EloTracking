package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HelpTest {
    //arrange
    final DiscordBot bot = mock(DiscordBot.class);
    final EloTrackingService service = mock(EloTrackingService.class);
    final Message msg = mock(Message.class);
    final Channel channel = mock(Channel.class);
    final Command accept = new Accept(bot, service, msg, channel);

    @Test
    @DisplayName("")
    public void testsomething() {}
    //act

    //assert

}
