package de.neuefische.elotracking.backend.timedtask;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class TimedTask {

	@Getter
	private TimedTaskType type;
	@Getter
	private String relationId;
}
