package com.elorankingbot.backend.timedtask;

import com.elorankingbot.backend.dao.TimeSlotDao;
import com.elorankingbot.backend.dao.TimedTaskQueueCurrentIndexDao;
import com.elorankingbot.backend.model.CurrentIndex;
import com.elorankingbot.backend.model.TimeSlot;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TimedTaskQueue {

	private final int numberOfTimeSlots;
	private int currentIndex;
	private final DiscordBotService bot;
	private final TimedTaskService timedTaskService;
	private final TimeSlotDao timeSlotDao;
	private final TimedTaskQueueCurrentIndexDao timedTaskQueueCurrentIndexDao;
	private boolean runQueue;

	public TimedTaskQueue(EloRankingService service, @Lazy DiscordBotService bot,
						  TimedTaskService timedTaskService, TimeSlotDao timeSlotDao,
						  TimedTaskQueueCurrentIndexDao timedTaskQueueCurrentIndexDao) {
		this.bot = bot;
		this.timedTaskService = timedTaskService;
		this.timeSlotDao = timeSlotDao;
		this.timedTaskQueueCurrentIndexDao = timedTaskQueueCurrentIndexDao;
		this.numberOfTimeSlots = service.getPropertiesLoader().getNumberOfTimeSlots();
		this.runQueue = service.getPropertiesLoader().isRunQueue();

		if (!timedTaskQueueCurrentIndexDao.existsById(1)) {
			currentIndex = 0;
		} else {
			currentIndex = timedTaskQueueCurrentIndexDao.findById(1).get().getValue();
		}
	}

	public void addTimedTask(TimedTask.TimedTaskType type, int delay, long relationId, long otherId, Object value) {
		if (!runQueue) return;

		int targetTimeSlotIndex = (currentIndex + delay) % numberOfTimeSlots;
		log.debug(String.format("adding timed task for %s of type %s with timer %s to slot %s",
				relationId, type.name(), delay, targetTimeSlotIndex));
		Optional<TimeSlot> maybeTimeSlot = timeSlotDao.findById(targetTimeSlotIndex);
		Set<TimedTask> timedTasks = maybeTimeSlot.isPresent() ? maybeTimeSlot.get().getTimedTasks() : new HashSet<>();
		timedTasks.add(new TimedTask(type, delay, relationId, otherId, value));
		timeSlotDao.save(new TimeSlot(targetTimeSlotIndex, timedTasks));
	}

	@Scheduled(fixedRate = 60000)
	public void tick() {
		if (!runQueue) return;

		log.debug("tick " + currentIndex);
		try {
			Optional<TimeSlot> maybeTimeSlot = timeSlotDao.findById(currentIndex);
			if (maybeTimeSlot.isPresent()) {
				for (TimedTask task : maybeTimeSlot.get().getTimedTasks()) {
					processTimedTask(task);
				}
				timeSlotDao.delete(maybeTimeSlot.get());
			}

			if (currentIndex == 0) {
				timedTaskService.deleteGamesMarkedForDeletion();
				timedTaskService.markGamesForDeletion();
			}

			currentIndex++;
			if (currentIndex >= numberOfTimeSlots) currentIndex = 0;
			timedTaskQueueCurrentIndexDao.save(new CurrentIndex(currentIndex));
		} catch (Exception e) {
			bot.sendToOwner(String.format("Error in TimedTaskQueue::tick\n%s", e.getMessage()));
			e.printStackTrace();
		}
	}

	private void processTimedTask(TimedTask task) {
		long id = task.relationId();
		int time = task.time();
		log.debug(String.format("executing %s %s after %s", task.type().name(), id, time));
		switch (task.type()) {
			case OPEN_CHALLENGE_DECAY:
				timedTaskService.decayOpenChallenge(id, time);
				break;
			case ACCEPTED_CHALLENGE_DECAY:
				timedTaskService.decayAcceptedChallenge(id, time);
				break;
			case MATCH_AUTO_RESOLVE:
				timedTaskService.autoResolveMatch(id, time);
				break;
			case MATCH_SUMMARIZE:
				timedTaskService.summarizeMatch(id, task.otherId(), task.value());
				break;
			case MESSAGE_DELETE:
				timedTaskService.deleteMessage(id, task.otherId());
				break;
			case CHANNEL_DELETE:
				timedTaskService.deleteChannel(id);
				break;
		}
	}
}
