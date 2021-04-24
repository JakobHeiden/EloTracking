package de.neuefische.elotracking.backend.command;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;

import java.util.HashSet;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageTestFactory {

    public Message createMockMessage() {
        Message mock = mock(Message.class);
        Snowflake mention = Snowflake.of(UUID.randomUUID().toString());
        when(mock.getUserMentionIds()).thenReturn(new HashSet<Snowflake>());
        return mock;
    }


}
