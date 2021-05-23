package de.neuefische.elotracking.backend.command;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageTestFactory {

    public static Message createMock(String text, Snowflake author) {
        Message mock = mock(Message.class);

        User authorUser = mock(User.class);
        when(authorUser.getId()).thenReturn(author);
        when(mock.getAuthor()).thenReturn(Optional.of(authorUser));

        //when(mock.getContent()).thenReturn(text);

        when(mock.getChannelId()).thenReturn(Snowflake.of(SnowflakeTestFactory.CHANNELID));

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
