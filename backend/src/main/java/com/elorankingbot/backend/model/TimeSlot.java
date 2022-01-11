package com.elorankingbot.backend.model;

import com.elorankingbot.backend.timedtask.TimedTask;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@Document(collection = "timeslot")
public class TimeSlot {

	@Id
	private final int index;
	private final Set<TimedTask> timedTasks;
}
