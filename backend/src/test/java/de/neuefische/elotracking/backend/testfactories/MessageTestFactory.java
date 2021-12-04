package de.neuefische.elotracking.backend.testfactories;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageTestFactory {

    public static Message createMock(String text, Snowflake author) {

        User authorUser = mock(User.class);
        when(authorUser.getId()).thenReturn(author);

        Message mock = mock(Message.class);
        when(mock.getAuthor()).thenReturn(Optional.of(authorUser));
        when(mock.getChannelId()).thenReturn(Snowflake.of(SnowflakeTestFactory.CHANNEL_ID));
        when(mock.getContent()).thenReturn(text);

        String[] words = text.split(" ");
        Set<Snowflake> mentionIds = Arrays.stream(words)
                .filter(word -> word.startsWith("@"))
                .map(word -> word.substring(1))
                .map(word -> Snowflake.of(word))
                .collect(Collectors.toSet());
        when(mock.getUserMentionIds()).thenReturn(mentionIds);

        return mock;
    }
}