package de.neuefische.elotracking.backend.command;

import discord4j.core.object.entity.Message;

public class Throw  extends Command {

	public Throw(Message msg) {
		super(msg);
	}

	@Override
	public void execute() {
		throw new RuntimeException("oh no");
	}
}
