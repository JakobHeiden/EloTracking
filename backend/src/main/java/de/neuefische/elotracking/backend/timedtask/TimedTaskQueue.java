package de.neuefische.elotracking.backend.timedtask;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.service.TimedTaskService;
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
	private final DiscordBotService bot;
	private final TimedTaskService timedTaskService;

	public TimedTaskQueue(EloTrackingService service, @Lazy DiscordBotService bot, TimedTaskService timedTaskService) {
		this.bot = bot;
		this.timedTaskService = timedTaskService;
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
						timedTaskService.timedDecayOpenChallenge(id, time);
						break;
					case ACCEPTED_CHALLENGE_DECAY:
						timedTaskService.timedDecayAcceptedChallenge(id, time);
						break;
					case MATCH_AUTO_RESOLVE:
						timedTaskService.timedAutoResolveMatch(id, time);
						break;
					case MATCH_SUMMARIZE:
						timedTaskService.timedSummarizeMatch(id, task.otherId(), task.value());
						break;
					case MESSAGE_DELETE:
						timedTaskService.timedDeleteMessage(id, task.otherId());
						break;
					case CHANNEL_DELETE:
						timedTaskService.timedDeleteChannel(id);
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
