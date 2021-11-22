package de.neuefische.elotracking.backend.timer;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TimedTaskQueue {

	private final int numberOfTimeSlots = 24*60;
	private final Set<TimedTask>[] timeSlots;
	private int currentIndex;
	@Autowired
	private EloTrackingService service;

	public TimedTaskQueue() {
		currentIndex = 0;
		timeSlots = new Set[numberOfTimeSlots];
		for (int i = 0; i < numberOfTimeSlots; i++) {
			timeSlots[i] = new HashSet<TimedTask>();
		}
	}

	public void addChallenge(ChallengeModel challenge, String channelId) {
		timeSlots[(currentIndex + 3) % numberOfTimeSlots]
				.add(new TimedTask(channelId, TimedTaskType.CHALLENGE_DECAY, challenge.getId()));
	}

	@Scheduled(fixedRate = 60000)
	public void tick() {
		for (TimedTask task: timeSlots[currentIndex]) {
			if (task.getType() == TimedTaskType.CHALLENGE_DECAY) {
				service.decayChallenge(task.getChannelId(), task.getRelationId());
			}
		}

		timeSlots[currentIndex] = new HashSet<TimedTask>();
		currentIndex++;
		if (currentIndex == numberOfTimeSlots) currentIndex = 0;
	}
}
