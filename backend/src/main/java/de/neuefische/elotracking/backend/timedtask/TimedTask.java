package de.neuefische.elotracking.backend.timedtask;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class TimedTask {//TODO umbauen in record?

	@Getter
	private TimedTaskType type;
	@Getter
	private int time;
	@Getter
	private String relationId;
}
