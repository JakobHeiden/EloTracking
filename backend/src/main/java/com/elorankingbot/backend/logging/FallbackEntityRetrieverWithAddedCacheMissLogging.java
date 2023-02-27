package com.elorankingbot.backend.logging;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.retriever.EntityRetriever;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FallbackEntityRetrieverWithAddedCacheMissLogging implements EntityRetriever {
    private final EntityRetriever first;
    private final EntityRetriever fallback;

    public FallbackEntityRetrieverWithAddedCacheMissLogging(EntityRetriever first, EntityRetriever fallback) {
        this.first = first;
        this.fallback = fallback;
    }

    public Mono<Channel> getChannelById(Snowflake channelId) {
        return this.first.getChannelById(channelId).doOnSubscribe(subscription -> System.out.println("store"))
                .switchIfEmpty(this.fallback.getChannelById(channelId)
                .doOnSubscribe(subscription -> System.out.println("REST")));
    }

    public Mono<Guild> getGuildById(Snowflake guildId) {
        return this.first.getGuildById(guildId).switchIfEmpty(this.fallback.getGuildById(guildId));
    }

    public Mono<GuildSticker> getGuildStickerById(Snowflake guildId, Snowflake stickerId) {
        return this.first.getGuildStickerById(guildId, stickerId).switchIfEmpty(this.fallback.getGuildStickerById(guildId, stickerId));
    }

    public Mono<GuildEmoji> getGuildEmojiById(Snowflake guildId, Snowflake emojiId) {
        return this.first.getGuildEmojiById(guildId, emojiId).switchIfEmpty(this.fallback.getGuildEmojiById(guildId, emojiId));
    }

    public Mono<Member> getMemberById(Snowflake guildId, Snowflake userId) {
        return this.first.getMemberById(guildId, userId).switchIfEmpty(this.fallback.getMemberById(guildId, userId));
    }

    public Mono<Message> getMessageById(Snowflake channelId, Snowflake messageId) {
        return this.first.getMessageById(channelId, messageId).switchIfEmpty(this.fallback.getMessageById(channelId, messageId));
    }

    public Mono<Role> getRoleById(Snowflake guildId, Snowflake roleId) {
        return this.first.getRoleById(guildId, roleId).switchIfEmpty(this.fallback.getRoleById(guildId, roleId));
    }

    public Mono<User> getUserById(Snowflake userId) {
        return this.first.getUserById(userId).switchIfEmpty(this.fallback.getUserById(userId));
    }

    public Flux<Guild> getGuilds() {
        return this.first.getGuilds().switchIfEmpty(this.fallback.getGuilds());
    }

    public Mono<User> getSelf() {
        return this.first.getSelf().switchIfEmpty(this.fallback.getSelf());
    }

    public Mono<Member> getSelfMember(Snowflake guildId) {
        return this.first.getSelfMember(guildId).switchIfEmpty(this.fallback.getSelfMember(guildId));
    }

    public Flux<Member> getGuildMembers(Snowflake guildId) {
        return this.first.getGuildMembers(guildId).switchIfEmpty(this.fallback.getGuildMembers(guildId));
    }

    public Flux<GuildChannel> getGuildChannels(Snowflake guildId) {
        return this.first.getGuildChannels(guildId).switchIfEmpty(this.fallback.getGuildChannels(guildId));
    }

    public Flux<Role> getGuildRoles(Snowflake guildId) {
        return this.first.getGuildRoles(guildId).switchIfEmpty(this.fallback.getGuildRoles(guildId));
    }

    public Flux<GuildEmoji> getGuildEmojis(Snowflake guildId) {
        return this.first.getGuildEmojis(guildId).switchIfEmpty(this.fallback.getGuildEmojis(guildId));
    }

    public Flux<GuildSticker> getGuildStickers(Snowflake guildId) {
        return this.first.getGuildStickers(guildId).switchIfEmpty(this.fallback.getGuildStickers(guildId));
    }
}
