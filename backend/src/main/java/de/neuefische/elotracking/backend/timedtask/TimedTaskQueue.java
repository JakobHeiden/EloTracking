package de.neuefische.elotracking.backend.timedtask;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Component
public class TimedTaskQueue {

	@Value("${number-of-time-slots}")
	private int numberOfTimeSlots;
	private Set<TimedTask>[] timeSlots;
	private int currentIndex;
	@Autowired
	private EloTrackingService service;
	@Autowired
	private DiscordBotService bot;

	public TimedTaskQueue() {
		currentIndex = 0;
	}

	@PostConstruct
	public void initTimeSlots() {
		timeSlots = new Set[numberOfTimeSlots];
		for (int i = 0; i < numberOfTimeSlots; i++) {
			timeSlots[i] = new HashSet<TimedTask>();
		}
	}

	public void addTimedTask(TimedTaskType type, int time, String relationId) {
		timeSlots[(currentIndex + time) % numberOfTimeSlots]
				.add(new TimedTask(type, relationId));
	}

	@Scheduled(fixedRate = 60000)
	public void tick() {
		try {
			for (TimedTask task : timeSlots[currentIndex]) {
				if (task.getType() == TimedTaskType.OPEN_CHALLENGE_DECAY) {
					service.decayOpenChallenge(task.getRelationId());
				}
			}

			timeSlots[currentIndex] = new HashSet<TimedTask>();
			currentIndex++;
			if (currentIndex == numberOfTimeSlots) currentIndex = 0;
		} catch (Exception e) {
			bot.sendToAdmin(String.format("Error in TimedTaskQueue::tick\n%s", e.getMessage()));
			throw e;
		}
	}
}
