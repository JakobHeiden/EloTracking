package com.elorankingbot.backend.timedtask;

import com.elorankingbot.backend.commands.timed.AutoResolveMatch;
import com.elorankingbot.backend.commands.timed.DecayAcceptedChallenge;
import com.elorankingbot.backend.commands.timed.DecayOpenChallenge;
import com.elorankingbot.backend.dao.TimeSlotDao;
import com.elorankingbot.backend.dao.TimedTaskQueueCurrentIndexDao;
import com.elorankingbot.backend.model.CurrentIndex;
import com.elorankingbot.backend.model.TimeSlot;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.GatewayDiscordClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TimedTaskQueue {

	@Getter
	private final int numberOfTimeSlots;
	@Getter
	private int currentIndex;
	private final Services services;
	private final DBService service;
	private final DiscordBotService bot;
	private final GatewayDiscordClient client;
	private final TimedTaskService timedTaskService;
	private final TimeSlotDao timeSlotDao;
	private final TimedTaskQueueCurrentIndexDao timedTaskQueueCurrentIndexDao;
	private boolean doRunQueue;

	public TimedTaskQueue(Services services,
						  TimeSlotDao timeSlotDao, TimedTaskQueueCurrentIndexDao timedTaskQueueCurrentIndexDao) {
		this.services = services;
		this.service = services.dbService;
		this.bot = services.bot;
		this.client = services.client;
		this.timedTaskService = services.timedTaskService;
		this.timeSlotDao = timeSlotDao;
		this.timedTaskQueueCurrentIndexDao = timedTaskQueueCurrentIndexDao;
		this.numberOfTimeSlots = services.props.getNumberOfTimeSlots();
		this.doRunQueue = services.props.isDoRunQueue();

		if (!timedTaskQueueCurrentIndexDao.existsById(1)) {
			currentIndex = 0;
		} else {
			currentIndex = timedTaskQueueCurrentIndexDao.findById(1).get().getValue();
		}
	}

	public void addTimedTask(TimedTask.TimedTaskType type, int delay, long relationId, long otherId, Object value) {
		if (!doRunQueue) return;

		int targetTimeSlotIndex = (currentIndex + delay) % numberOfTimeSlots;
		log.debug(String.format("adding timed task for %s of type %s with timer %s to slot %s",
				relationId, type.name(), delay, targetTimeSlotIndex));
		Optional<TimeSlot> maybeTimeSlot = timeSlotDao.findById(targetTimeSlotIndex);
		Set<TimedTask> timedTasks = maybeTimeSlot.isPresent() ? maybeTimeSlot.get().getTimedTasks() : new HashSet<>();
		timedTasks.add(new TimedTask(type, delay, relationId, otherId, value));
		timeSlotDao.save(new TimeSlot(targetTimeSlotIndex, timedTasks));
	}

	private void processTimedTask(TimedTask task) {
		long id = task.relationId();
		long otherId = task.otherId();
		int duration = task.duration();
		log.debug(String.format("executing %s %s after %s", task.type().name(), id, duration));
		switch (task.type()) {
			case OPEN_CHALLENGE_DECAY:
				new DecayOpenChallenge(services, id, duration).execute();
				break;
			case ACCEPTED_CHALLENGE_DECAY:
				new DecayAcceptedChallenge(services, id, duration).execute();
				break;
			case MATCH_AUTO_RESOLVE:
				new AutoResolveMatch(services, id, duration).execute();
				break;
			case MATCH_SUMMARIZE:
				timedTaskService.summarizeMatch(id, otherId, task.value());
				break;
			case MESSAGE_DELETE:
				timedTaskService.deleteMessage(id, otherId);
				break;
			case CHANNEL_DELETE:
				timedTaskService.deleteChannel(id);
				break;
			case PLAYER_UNBAN:
				timedTaskService.unbanPlayer(id, otherId, duration);
				break;
		}
	}

	@Scheduled(fixedRate = 60000)
	public void tick() {
		if (!doRunQueue) return;

		log.debug("tick " + currentIndex);
		try {
			if (currentIndex == 0) {// TODO 1 mal im jahr vllt zu selten?
				timedTaskService.deleteGamesMarkedForDeletion();
				timedTaskService.markGamesForDeletion();
			}

			Optional<TimeSlot> maybeTimeSlot = timeSlotDao.findById(currentIndex);
			if (maybeTimeSlot.isPresent()) {
				for (TimedTask task : maybeTimeSlot.get().getTimedTasks()) {
					processTimedTask(task);
				}
				timeSlotDao.delete(maybeTimeSlot.get());
			}
		} catch (Exception e) {
			bot.sendToOwner(String.format("Error in TimedTaskQueue::tick\n%s", e.getMessage()));
			e.printStackTrace();
		}
		currentIndex++;
		if (currentIndex >= numberOfTimeSlots) currentIndex = 0;
		timedTaskQueueCurrentIndexDao.save(new CurrentIndex(currentIndex));
	}

	public int getRemainingDuration(int index) {
		return index > currentIndex ?
				index - currentIndex
				: numberOfTimeSlots + index - currentIndex;
	}
}
