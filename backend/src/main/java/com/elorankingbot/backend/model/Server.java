package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "server")
public class Server {

	@Id
	private long guildId;
	private Set<Game> games;
	private long adminRoleId;
	private long modRoleId;
	private long disputeCategoryId;
	private boolean isMarkedForDeletion;

	public Server(long guildId) {
		this.guildId = guildId;
		this.isMarkedForDeletion = false;
		this.games = new HashSet<>();
	}
}
