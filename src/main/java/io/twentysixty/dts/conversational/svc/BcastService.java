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
import com.mobiera.ms.commons.stats.api.StatEnum;

import io.twentysixty.dts.conversational.jms.MtProducer;
import io.twentysixty.dts.conversational.jms.StatProducer;
import io.twentysixty.dts.conversational.model.Broadcast;
import io.twentysixty.orchestrator.api.CmSchedule;
import io.twentysixty.orchestrator.api.RegisterResponse;
import io.twentysixty.orchestrator.api.enums.App;
import io.twentysixty.orchestrator.api.vo.CampaignVO;
import io.twentysixty.orchestrator.res.c.CampaignScheduleResource;
import io.twentysixty.orchestrator.stats.CampaignStat;
import io.twentysixty.orchestrator.stats.DtsStat;
import io.twentysixty.orchestrator.stats.OrchestratorStatClass;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.ContextualMenuSelect;
import io.twentysixty.sa.client.model.message.InvitationMessage;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuSelectMessage;
import io.twentysixty.sa.client.model.message.MessageReceiptOptions;
import io.twentysixty.sa.client.model.message.ReceiptsMessage;
import io.twentysixty.sa.client.model.message.TextMessage;
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
	@Inject StatProducer statProducer;
	@Inject Controller controller;
	@Inject CacheService cacheService;
	@Inject MtProducer mtProducer;
	
	
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




	@Transactional
	public void threadIdChecker(UUID threadId, BaseMessage message) throws Exception {
		// called when a message is received
		io.twentysixty.dts.conversational.model.Thread th = em.find(io.twentysixty.dts.conversational.model.Thread.class, threadId);
		if (th != null) {
			UUID bcastId = th.getBroadcastId();
			if (bcastId != null) {
				Broadcast bcast = em.find(Broadcast.class, bcastId);
				if (bcast != null) {
					ArrayList<StatEnum> lenum = new ArrayList<StatEnum>(1);
		        	
					if (message instanceof ReceiptsMessage) {
		        		ReceiptsMessage rm = (ReceiptsMessage) message;
		        		for (MessageReceiptOptions o: rm.getReceipts()) {
		        			switch (o.getState()) {
		        			case RECEIVED: {
		        				lenum.add(CampaignStat.RECEIVED);
		        				bcast.setReceivedCount(bcast.getReceivedCount()+1);
		        				em.merge(bcast);
		        				break;
		        			}
		        			case SUBMITTED: {
		        				lenum.add(CampaignStat.SUBMITTED);
		        				bcast.setSubmittedCount(bcast.getSubmittedCount()+1);
		        				em.merge(bcast);
		        				break;
		        			}
		        			case VIEWED: {
		        				lenum.add(CampaignStat.VIEWED);
		        				bcast.setViewedCount(bcast.getViewedCount()+1);
		        				em.merge(bcast);
		        				break;
		        			}
		        			default: {
		        				break;
		        			}
		        			}
		        		}
		            	statProducer.spool(OrchestratorStatClass.DTS.toString(), Controller.getDtsConfig().getId(), lenum, Instant.now(), 1);

		        	}
				}
			}
		}
	}


	public void runScheduledCampaign(UUID selectedConnection, UUID campaignId, UUID csId) {
		if (campaignId != null) {
			try {
				
				
				UUID broadcastId = this.getBroadcastForScheduled(campaignId, selectedConnection, false);
				
				if (broadcastId != null) {
					runCampaign(campaignId, selectedConnection, csId, broadcastId, false);
				} else {
					if (Controller.getDtsConfig().getDebug()) {
						logger.info("runScheduledCampaign: not running campaign " + campaignId + " for user " + selectedConnection);

					}
				}
				
			} catch (Exception e) {
				logger.error("error running campaign " + campaignId + " for " + selectedConnection, e);
			}
			
		}
	}
	
	
	
	@Transactional
	public void runCampaign(UUID campaignId, UUID selectedConnection, UUID csId, UUID broadcastId, boolean test) throws Exception {
		CampaignVO campaign = cacheService.getCampaignVO(campaignId);
		io.twentysixty.dts.conversational.model.Thread th = new io.twentysixty.dts.conversational.model.Thread();
		th.setBroadcastId(broadcastId);
		th.setConnectionId(selectedConnection);
		th.setId(UUID.randomUUID());
		th.setTs(Instant.now());
		mtProducer.sendMessage(TextMessage.build(selectedConnection, th.getId(), "Test broadcast"));
	}






	@Transactional
	public UUID getBroadcastForScheduled(UUID campaignId, UUID connectionId, boolean onlyIfNotViewed) {
		
		Broadcast session = null;
		Query q = this.em.createNamedQuery("Broadcast.findForConnectionCampaign");
		q.setParameter("campaignId", campaignId);
		q.setParameter("connectionId", connectionId);
		session = (Broadcast) q.getResultList().stream().findFirst().orElse(null);
		
		CampaignVO campaign = cacheService.getCampaignVO(campaignId);
		
		if (session == null) {
			if (Controller.getDtsConfig().getDebug()) {
				logger.info("getBroadcastForScheduled: connectionId: " + connectionId + " no session for campaign " + campaign.getName() + " id: " + campaign.getId());
			}
			session = new Broadcast();
			session.setCampaignId(campaignId);
			session.setConnectionId(connectionId);
			session.setViewedCount(0);
			session.setReceivedCount(0);
			session.setSubmittedCount(0);
			session.setTs(Instant.now());
			session.setManagement(campaign.getManagement());
			session.setLastSentTs(Instant.now());
			em.persist(session);
			
			
		} else {
			if ((!onlyIfNotViewed) || (session.getViewedCount() == null) || (session.getViewedCount() == 0)) {
				session.setLastSentTs(Instant.now());
				session = em.merge(session);
			} else {
				return null;
			}
			
		}
		
		return session.getId();
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
