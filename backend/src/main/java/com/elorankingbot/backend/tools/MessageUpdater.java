package com.elorankingbot.backend.tools;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditMono;

public class MessageUpdater {

	private final Message message;
	private String content;

	public MessageUpdater(long messageId, long channelId, GatewayDiscordClient client) {
		this.message = client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block();
		this.content = message.getContent();
	}

	public MessageUpdater(Message message) {
		this.message = message;
		this.content = message.getContent();
	}

	public MessageUpdater addLine(String newLine) {
		content = content + "\n" + newLine;
		return this;
	}
	public MessageUpdater makeAllNotBold() {
		content = content.replace("**", "");
		return this;
	}

	public MessageUpdater makeLastLineBold() {
		String[] lines = content.split("\n");
		lines[lines.length - 1] = "**" + lines[lines.length - 1] + "**";
		content = String.join("\n", lines);
		return this;
	}

	public MessageUpdater makeAllItalic() {
		content = "*" + content + "*";
		return this;
	}

	public MessageUpdater makeLastLineStrikeThrough() {
		String[] lines = content.split("\n");
		lines[lines.length - 1] = "~~" + lines[lines.length - 1] + "~~";
		content = String.join("\n", lines);
		return this;
	}

	public MessageEditMono update() {
		return message.edit().withContent(content);
	}
}
