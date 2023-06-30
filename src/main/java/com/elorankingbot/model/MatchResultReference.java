package com.elorankingbot.model;

import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Document(collection = "matchresultreference")
@AllArgsConstructor(onConstructor=@__({@PersistenceConstructor}))
public class MatchResultReference {

	@Id
	private final long resultMessageId;
	@Indexed
	private final long matchMessageId;
	private final long resultChannelId, matchChannelId;
	private final UUID matchResultId;

	public MatchResultReference(Message resultMessage, Message matchMessage, UUID matchResultId) {
		this.resultMessageId = resultMessage.getId().asLong();
		this.resultChannelId = resultMessage.getChannelId().asLong();
		this.matchMessageId = matchMessage.getId().asLong();
		this.matchChannelId = matchMessage.getChannelId().asLong();
		this.matchResultId = matchResultId;
	}

	public MatchResultReference(Message resultMessage, UUID matchResultId) {
		this.resultMessageId = resultMessage.getId().asLong();
		this.resultChannelId = resultMessage.getChannelId().asLong();
		this.matchMessageId = 0L;
		this.matchChannelId = 0L;
		this.matchResultId = matchResultId;
	}
}
