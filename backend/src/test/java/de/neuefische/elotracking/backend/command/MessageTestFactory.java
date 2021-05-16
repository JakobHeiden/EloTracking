package de.neuefische.elotracking.backend.command;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageTestFactory {

    public Message createMockMessage(String text) {
        Message mock = mock(Message.class);
        //text
        when(mock.getContent()).thenReturn(text);
        //channelid
        when(mock.getChannelId()).thenReturn(SnowflakeTestFactory.createRandomSnowflake());
        //mentions
        String[] words = text.split(" ");
        Set<Snowflake> mentionIds = Arrays.stream(words)
                .filter(word -> word.startsWith("@"))
                .map(mentionString -> String.valueOf(Math.abs(mentionString.hashCode())))
                .map(Snowflake::of)
                .collect(Collectors.toSet());
        when(mock.getUserMentionIds()).thenReturn(mentionIds);

        return mock;
    }
}
