package de.neuefische.elotracking.backend.timedtask;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Component
public class TimedTaskQueue {

	private int numberOfTimeSlots;
	private Set<TimedTask>[] timeSlots;
	private int currentIndex;
	private EloTrackingService service;
	private DiscordBotService bot;

	public TimedTaskQueue(EloTrackingService service, @Lazy DiscordBotService bot) {
		this.service = service;
		this.bot = bot;
		this.currentIndex = 0;
		this.numberOfTimeSlots = service.getPropertiesLoader().getNumberOfTimeSlots();
	}

	@PostConstruct
	public void initTimeSlots() {
		timeSlots = new Set[numberOfTimeSlots];
		for (int i = 0; i < numberOfTimeSlots; i++) {
			timeSlots[i] = new HashSet<TimedTask>();
		}
	}

	public void addTimedTask(TimedTask.TimedTaskType type, int time, long relationId, long otherId, Object value) {
		timeSlots[(currentIndex + time) % numberOfTimeSlots]
				.add(new TimedTask(type, time, relationId, otherId, value));
	}

	@Scheduled(fixedRate = 60000)
	public void tick() {
		try {
			for (TimedTask task : timeSlots[currentIndex]) {
				long id = task.relationId();
				int time = task.time();
				switch (task.type()) {
					case OPEN_CHALLENGE_DECAY:
						service.timedDecayOpenChallenge(id, time);
						break;
					case ACCEPTED_CHALLENGE_DECAY:
						service.timedDecayAcceptedChallenge(id, time);
						break;
					case MATCH_AUTO_RESOLVE:
						service.timedAutoResolveMatch(id, time);
						break;
					case MATCH_SUMMARIZE:
						service.timedSummarizeMatch(id, task.otherId(), task.value());
						break;
					case DELETE_MESSAGE:
						service.timedDeleteMessage(id, task.otherId());
						break;
				}
			}

			timeSlots[currentIndex] = new HashSet<TimedTask>();
			currentIndex++;
			if (currentIndex == numberOfTimeSlots) currentIndex = 0;
		} catch (Exception e) {
			bot.sendToOwner(String.format("Error in TimedTaskQueue::tick\n%s", e.getMessage()));
			throw e;
		}
	}
}
