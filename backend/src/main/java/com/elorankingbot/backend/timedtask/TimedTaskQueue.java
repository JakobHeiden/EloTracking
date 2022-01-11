package com.elorankingbot.backend.timedtask;

import com.elorankingbot.backend.dao.TimeSlotDao;
import com.elorankingbot.backend.dao.TimedTaskQueueCurrentIndexDao;
import com.elorankingbot.backend.model.CurrentIndex;
import com.elorankingbot.backend.model.TimeSlot;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.service.TimedTaskService;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class TimedTaskQueue {

	private int numberOfTimeSlots;
	private int currentIndex;
	private final DiscordBotService bot;
	private final TimedTaskService timedTaskService;
	private final TimeSlotDao timeSlotDao;
	private final TimedTaskQueueCurrentIndexDao timedTaskQueueCurrentIndexDao;

	public TimedTaskQueue(EloRankingService service, @Lazy DiscordBotService bot,
						  TimedTaskService timedTaskService, TimeSlotDao timeSlotDao,
						  TimedTaskQueueCurrentIndexDao timedTaskQueueCurrentIndexDao) {
		this.bot = bot;
		this.timedTaskService = timedTaskService;
		this.timeSlotDao = timeSlotDao;
		this.timedTaskQueueCurrentIndexDao = timedTaskQueueCurrentIndexDao;
		this.numberOfTimeSlots = service.getPropertiesLoader().getNumberOfTimeSlots();

		if (!timedTaskQueueCurrentIndexDao.existsById(1)) {
			currentIndex = 0;
		} else {
			currentIndex = timedTaskQueueCurrentIndexDao.findById(1).get().getValue();
		}
	}

	public void addTimedTask(TimedTask.TimedTaskType type, int delay, long relationId, long otherId, Object value) {
		Optional<TimeSlot> maybeTimeSlot =
				timeSlotDao.findById(Integer.valueOf((currentIndex + delay) % numberOfTimeSlots));
		Set<TimedTask> timedTasks = maybeTimeSlot.isPresent() ? maybeTimeSlot.get().getTimedTasks() : new HashSet<>();
		timedTasks.add(new TimedTask(type, delay, relationId, otherId, value));
		timeSlotDao.save(new TimeSlot(currentIndex + delay, timedTasks));
	}

	@Scheduled(fixedRate = 60000)
	public void tick() {
		try {
			Optional<TimeSlot> maybeTimeSlot = timeSlotDao.findById(currentIndex);
			if (maybeTimeSlot.isPresent()) {
				for (TimedTask task : maybeTimeSlot.get().getTimedTasks()) {
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

				timeSlotDao.delete(maybeTimeSlot.get());
			}

			if (currentIndex == 0) {
				timedTaskService.deleteGamesMarkedForDeletion();
				timedTaskService.markGamesForDeletion();
			}

			currentIndex++;
			if (currentIndex == numberOfTimeSlots) currentIndex = 0;
			timedTaskQueueCurrentIndexDao.save(new CurrentIndex(currentIndex));
		} catch (Exception e) {
			bot.sendToOwner(String.format("Error in TimedTaskQueue::tick\n%s", e.getMessage()));
			throw e;
		}
	}
}
