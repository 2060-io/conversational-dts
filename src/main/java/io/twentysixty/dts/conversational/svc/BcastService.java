package io.twentysixty.dts.conversational.svc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.graalvm.collections.Pair;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mobiera.commons.enums.EntityState;
import com.mobiera.commons.util.JsonUtil;

import io.twentysixty.orchestrator.api.CmSchedule;
import io.twentysixty.orchestrator.api.RegisterResponse;
import io.twentysixty.orchestrator.api.enums.App;
import io.twentysixty.orchestrator.api.vo.CampaignVO;
import io.twentysixty.orchestrator.res.c.CampaignScheduleResource;
import io.twentysixty.sa.res.c.v1.MessageResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class BcastService {

	private static Logger logger = Logger.getLogger(BcastService.class);

	@Inject EntityManager em;

	@Inject Controller controller;
	@Inject CacheService cacheService;
	
	
	
	private Random random = new Random();
	
	
	
	private static  String broadcastSelectQuery =
			"UPDATE session AS s1 SET nextBcTs=:nextBcTs WHERE s1.connectionId IN "
					+ "( SELECT connectionId FROM session AS s2 where s2.nextBcTs<:now AND s2.nextBcTs>:minNextBcTs "
					+ " LIMIT QTY FOR UPDATE SKIP LOCKED ) RETURNING s1.connectionId";

	@Transactional
	public List<UUID> lockAndGetConnections(Integer qty) {



		String query = broadcastSelectQuery.replaceFirst("QTY",  qty.toString());


		Query q = em.createNativeQuery(query);
		Instant now = Instant.now();


		// set +1h just in case sending generates exception, will be adjusted later
		q.setParameter("nextBcTs", Instant.now().plusSeconds(3600l));
		q.setParameter("now", now);
		q.setParameter("minNextBcTs", Instant.now().minusSeconds(86400l * 365));

		List<UUID>res = q.getResultList();
		if (controller.isDebugEnabled()) {
			if (res.size() >0) {
				logger.info("lockAndGetConnections: " + query + " " + res);
			} else {
				logger.info("lockAndGetConnections: " + query + " no result" );
			}
		}


		return res;
	}
	
	
	
	
	
	
	private CmSchedule getRandomScheduledCampaign() {
		
		
		
		List<CmSchedule> schedules = cacheService.getEnabledSchedules();
		CmSchedule chosenSchedule = null;
		
		if ((schedules != null) && (schedules.size()>0)) {
			chosenSchedule = schedules.get(random.nextInt(schedules.size()));
			
			if (Controller.getDtsConfig().getDebug()) {
				try {
					logger.info("getRandomScheduledCampaign: selected: " + JsonUtil.serialize(chosenSchedule, false) );
				} catch (JsonProcessingException e) {
					
				}
			}
			
		}
		return chosenSchedule;
		
	}

	
	public Pair<CmSchedule, Integer> selectCampaignToRun(int maxAllowedPerRun) {
		
		
		//if (!campaignService.timeToRunScheduled()) return null;
		
		CmSchedule schedule = this.getRandomScheduledCampaign();
		
		if (Controller.getDtsConfig().getDebug()) {
			try {
				logger.info("buildScheduledCampaignSimSelector: schedule: " + JsonUtil.serialize(schedule, false));
			} catch (JsonProcessingException e) {
				
			}
		}
		
		
		if (schedule != null) {
			CampaignVO campaign = cacheService.getCampaignVO(schedule.getCampaignFk());

			
			if ((campaign != null) && (campaign.getState().equals(EntityState.ENABLED))) {
				

				int missing = schedule.getMissing();
				int total = schedule.getMissing();
				int instances = schedule.getRegisteredCmInstances();
				float percentDone = ((float)missing) / ((float)total);

				// 100,000 10 10
				// => 1000

				// 1,000 10 10
				// => 10

				int maxPerRun = (int)(((float)total) / ((float)instances)); // / (cacheLifetime.floatValue()));

				if (percentDone>0.975) {
					maxPerRun = maxPerRun / 8;
					if (maxPerRun == 0) {
						maxPerRun = 1;
					}
				} else if (percentDone>0.95) {
					maxPerRun = maxPerRun / 6;
					if (maxPerRun == 0) {
						maxPerRun = 1;
					}
				} else if (percentDone>0.90) {
					maxPerRun = maxPerRun / 5;
					if (maxPerRun == 0) {
						maxPerRun = 1;
					}
				} else if (percentDone>0.80) {
					maxPerRun = maxPerRun / 4;
					if (maxPerRun == 0) {
						maxPerRun = 1;
					}
				} else if (percentDone>0.70) {
					maxPerRun = maxPerRun / 3;
					if (maxPerRun == 0) {
						maxPerRun = 1;
					}
				} else if (percentDone>0.60) {
					maxPerRun = maxPerRun / 2;
					if (maxPerRun == 0) {
						maxPerRun = 1;
					}
				} 

				if (maxPerRun > maxAllowedPerRun) {
					maxPerRun = maxAllowedPerRun;
				}
				return Pair.create(schedule, maxPerRun);
				
			}


		}

		return null;
	}






	public Float getQueueUsage() {
		return 0f;
	}






	public void runScheduledCampaign(UUID selectedConnection, UUID campaignFk, UUID csId) {
		// TODO Auto-generated method stub
		
	}
	
	/*
	public boolean timeToRunScheduled() {
		String runWhen = parameterService.getAsString(ParameterName.STRING_SCHEDULED_ALLOWED_HOURS);
		String tz = parameterService.getAsString(ParameterName.STRING_NODE_TIMEZONE);
		
		OffsetDateTime odt = OffsetDateTime.now(ZoneId.of(tz));
		String hour = odt.format(formatter);
		if (isDebugEnabled()) {
			logger.info("timeToRunScheduled: hour: " + hour + " config: " + runWhen);
		}
		
		if (runWhen.contains(hour)) {
			if (isDebugEnabled()) {
				logger.info("timeToRunScheduled: hour: " + hour + " config: " + runWhen + " : TRUE");
			}
			return true;
		}
		if (isDebugEnabled()) {
			logger.info("timeToRunScheduled: hour: " + hour + " config: " + runWhen + " : FALSE");
		}
		return false;
	}
*/
}
