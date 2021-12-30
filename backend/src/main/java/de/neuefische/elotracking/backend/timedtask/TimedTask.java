package de.neuefische.elotracking.backend.timedtask;

public record TimedTask (
		TimedTaskType type,
		int time,
		long relationId,
		long otherId,
		Object value) {

	public enum TimedTaskType {
		OPEN_CHALLENGE_DECAY,
		ACCEPTED_CHALLENGE_DECAY,
		MATCH_AUTO_RESOLVE,
		MATCH_SUMMARIZE,
		MESSAGE_DELETE
	}
}
