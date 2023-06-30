package com.elorankingbot.logging;

import discord4j.common.util.Snowflake;
import discord4j.core.object.automod.AutoModRule;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.retriever.EntityRetriever;
import lombok.extern.apachecommons.CommonsLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CommonsLog
public class CustomFallbackEntityRetrieverWithAddedCacheMissLogging implements EntityRetriever {

    private final EntityRetriever first;
    private final EntityRetriever fallback;

    public CustomFallbackEntityRetrieverWithAddedCacheMissLogging(EntityRetriever first, EntityRetriever fallback) {
        this.first = first;
        this.fallback = fallback;
    }

    public Mono<Channel> getChannelById(Snowflake channelId) {
        return this.first.getChannelById(channelId)
                .switchIfEmpty(this.fallback.getChannelById(channelId)
                        .doOnSubscribe(subscription -> log.warn("Cache miss for channel " + channelId.asString())));
    }

    public Mono<Guild> getGuildById(Snowflake guildId) {
        return this.first.getGuildById(guildId).switchIfEmpty(this.fallback.getGuildById(guildId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for guild " + guildId.asString())));
    }

    public Mono<GuildSticker> getGuildStickerById(Snowflake guildId, Snowflake stickerId) {
        return this.first.getGuildStickerById(guildId, stickerId).switchIfEmpty(this.fallback.getGuildStickerById(guildId, stickerId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for sticker " + stickerId.asString())));
    }

    public Mono<GuildEmoji> getGuildEmojiById(Snowflake guildId, Snowflake emojiId) {
        return this.first.getGuildEmojiById(guildId, emojiId).switchIfEmpty(this.fallback.getGuildEmojiById(guildId, emojiId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for emoji " + emojiId.asString())));
    }

    public Mono<Member> getMemberById(Snowflake guildId, Snowflake userId) {
        return this.first.getMemberById(guildId, userId).switchIfEmpty(this.fallback.getMemberById(guildId, userId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for member " + userId.asString())));
    }

    public Mono<Message> getMessageById(Snowflake channelId, Snowflake messageId) {
        return this.first.getMessageById(channelId, messageId).switchIfEmpty(this.fallback.getMessageById(channelId, messageId)
                .doOnSubscribe(subscription -> log.warn(String.format("Cache miss for message %s in %s",
                        messageId.asString(), channelId.asString()))));
    }

    public Mono<Role> getRoleById(Snowflake guildId, Snowflake roleId) {
        return this.first.getRoleById(guildId, roleId).switchIfEmpty(this.fallback.getRoleById(guildId, roleId)
                .doOnSubscribe(subscription -> log.warn(String.format("Cache miss for role %s on %s",
                        roleId.asString(), guildId.asString()))));
    }

    public Mono<User> getUserById(Snowflake userId) {
        return this.first.getUserById(userId).switchIfEmpty(this.fallback.getUserById(userId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for user " + userId.asString())));
    }

    public Flux<Guild> getGuilds() {
        return this.first.getGuilds().switchIfEmpty(this.fallback.getGuilds()
                .doOnSubscribe(subscription -> log.warn("Cache miss for getGuilds")));
    }

    public Mono<User> getSelf() {
        return this.first.getSelf().switchIfEmpty(this.fallback.getSelf()
                .doOnSubscribe(subscription -> log.warn("Cache miss for getSelf")));
    }

    public Mono<Member> getSelfMember(Snowflake guildId) {
        return this.first.getSelfMember(guildId).switchIfEmpty(this.fallback.getSelfMember(guildId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for getSelfMember on " + guildId.asString())));
    }

    public Flux<Member> getGuildMembers(Snowflake guildId) {
        return this.first.getGuildMembers(guildId).switchIfEmpty(this.fallback.getGuildMembers(guildId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for getSelf on " + guildId.asString())));
    }

    public Flux<GuildChannel> getGuildChannels(Snowflake guildId) {
        return this.first.getGuildChannels(guildId).switchIfEmpty(this.fallback.getGuildChannels(guildId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for getGuildChannels on " + guildId.asString())));
    }

    public Flux<Role> getGuildRoles(Snowflake guildId) {
        return this.first.getGuildRoles(guildId).switchIfEmpty(this.fallback.getGuildRoles(guildId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for getGuildRoles on " + guildId.asString())));
    }

    public Flux<GuildEmoji> getGuildEmojis(Snowflake guildId) {
        return this.first.getGuildEmojis(guildId).switchIfEmpty(this.fallback.getGuildEmojis(guildId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for getGuildEmojis on " + guildId.asString())));
    }

    public Flux<GuildSticker> getGuildStickers(Snowflake guildId) {
        return this.first.getGuildStickers(guildId).switchIfEmpty(this.fallback.getGuildStickers(guildId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for getGuildStickers on " + guildId.asString())));
    }

    public Flux<AutoModRule> getGuildAutoModRules(Snowflake guildId) {
        return this.first.getGuildAutoModRules(guildId).switchIfEmpty(this.fallback.getGuildAutoModRules(guildId)
                .doOnSubscribe(subscription -> log.warn("Cache miss for getGuildAutoModRules on " + guildId.asString())));
    }
}
