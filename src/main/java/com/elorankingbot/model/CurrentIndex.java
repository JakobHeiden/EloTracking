package com.elorankingbot.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "currentindex")
public class CurrentIndex {

	@Id
	private int id = 1;
	private int value;

	public CurrentIndex(int value) {
		this.value = value;
	}
}
