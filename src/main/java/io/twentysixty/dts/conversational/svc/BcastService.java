package io.twentysixty.dts.conversational.svc;

import java.time.Duration;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mobiera.commons.enums.EntityState;
import com.mobiera.commons.util.JsonUtil;
import com.mobiera.ms.commons.stats.api.StatEnum;

import io.twentysixty.dts.conversational.jms.MtProducer;
import io.twentysixty.dts.conversational.jms.StatProducer;
import io.twentysixty.dts.conversational.model.Broadcast;
import io.twentysixty.dts.conversational.model.Event;
import io.twentysixty.dts.conversational.model.Connection;
import io.twentysixty.orchestrator.api.CmSchedule;
import io.twentysixty.orchestrator.api.RegisterResponse;
import io.twentysixty.orchestrator.api.enums.App;
import io.twentysixty.orchestrator.api.vo.CampaignVO;
import io.twentysixty.orchestrator.res.c.CampaignScheduleResource;
import io.twentysixty.orchestrator.stats.CampaignStat;
import io.twentysixty.orchestrator.stats.DtsStat;
import io.twentysixty.orchestrator.stats.OrchestratorStatClass;
import io.twentysixty.sa.client.model.event.MessageStateUpdated;
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
	
	@ConfigProperty(name = "io.twentysixty.orchestrator.bcast.scheduled.maxeachnhours")
	Integer maxeachnhours;

	
	
	
	private Random random = new Random();
	
	
	
	private static  String broadcastSelectQuery =
			"UPDATE connection AS s1 SET nextBcTs=:nextBcTs WHERE s1.id IN "
					+ "( SELECT id FROM connection AS s2 where s2.nextBcTs<:now AND s2.nextBcTs>:minNextBcTs and s2.deletedTs IS NULL"
					+ " LIMIT QTY FOR UPDATE SKIP LOCKED ) RETURNING s1.id";

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
	public void messageStateChanged(MessageStateUpdated msu) throws Exception {
		// called when a message is received
		
		UUID id = msu.getMessageId();
		
		if (id != null) {
			logger.info("messageStateChanged: " + JsonUtil.serialize(msu, false));
			Event th = em.find(Event.class, id);
			if (th != null) {
				Broadcast bcast = th.getBroadcast();
				
				if (bcast != null) {
					
						ArrayList<StatEnum> lenum = new ArrayList<StatEnum>(1);
						
						switch (msu.getState()) {
	        			case RECEIVED: {
	        				logger.info("messageStateChanged: RECEIVED: " + JsonUtil.serialize(msu, false));
	        				
	        				bcast.setSubmittedReceived(bcast.getSubmittedReceived()+1);
	        				if (bcast.getSubmittedReceived()>=bcast.getMessageCount()) {
	        					lenum.add(CampaignStat.RECEIVED);
	        				}
	        				th.setSubmittedReceived(true);
	        				break;
	        			}
	        			case SUBMITTED: {
	        				logger.info("messageStateChanged: SUBMITTED: " + JsonUtil.serialize(msu, false));

	        				bcast.setSubmitted(bcast.getSubmitted()+1);
	        				if (bcast.getSubmitted()>=bcast.getMessageCount()) {
	        					lenum.add(CampaignStat.SUBMITTED);
	        				}
	        				
	        				th.setSubmitted(true);
	        				
	        				break;
	        			}
	        			case VIEWED: {
	        				
	        				logger.info("messageStateChanged: VIEWED: " + JsonUtil.serialize(msu, false));

	        				bcast.setSubmittedViewed(bcast.getSubmittedViewed()+1);
	        				if (bcast.getSubmittedViewed()>=bcast.getMessageCount()) {
	        					lenum.add(CampaignStat.VIEWED);
	        				}
	        				th.setSubmittedViewed(true);
	        				
	        				break;
	        			}
	        			default: {
	        				break;
	        			}
	        			}
						
		            	statProducer.spool(OrchestratorStatClass.DTS.toString(), Controller.getDtsConfig().getId(), lenum, Instant.now(), 1);

				}
			}
		}
		
	}


	public void runScheduledCampaign(UUID selectedConnection, UUID campaignId, UUID csId) {
		if (campaignId != null) {
			try {
				
				
				UUID broadcastId = this.getBroadcastForScheduled(campaignId, selectedConnection, true);
				
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
		Instant now = Instant.now();
		
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		List<BaseMessage> messages = om.readValue(campaign.getYaml(), new TypeReference<List<BaseMessage>>() {});
				
		Connection connection = em.find(Connection.class, selectedConnection);
		Broadcast bcast = em.find(Broadcast.class, broadcastId);
		bcast.setSubmittedReceived(0);
		bcast.setSubmittedViewed(0);
		
		
		for (BaseMessage m: messages) {
			
			io.twentysixty.dts.conversational.model.Event ev = new io.twentysixty.dts.conversational.model.Event();
			ev.setBroadcast(bcast);
			ev.setConnection(connection);
			ev.setId(UUID.randomUUID());
			ev.setThreadId(UUID.randomUUID());
			ev.setTs(now);
			ev.setSubmitted(false);
			ev.setSubmittedReceived(false);
			ev.setSubmittedViewed(false);
			em.persist(ev);
			
			m.setConnectionId(selectedConnection);
			m.setThreadId(ev.getThreadId());
			m.setId(ev.getId());
			logger.info("runCampaign: sending message " + JsonUtil.serialize(messages, false));
			mtProducer.sendMessage(m);
			
		}
		
		bcast.setSubmitted(messages.size());
		
		connection.setLastBcTs(now);
		connection.setNextBcTs(Instant.now().plus(Duration.ofHours(maxeachnhours)));
		if (connection.getSentBcasts() == null) {
			connection.setSentBcasts(1);
		} else {
			connection.setSentBcasts(connection.getSentBcasts()+1);
		}
		em.merge(connection);
		
	}






	@Transactional
	public UUID getBroadcastForScheduled(UUID campaignId, UUID connectionId, boolean onlyIfNotViewed) {
		
		Connection connection = em.find(Connection.class, connectionId);
		
		Broadcast bcast = null;
		Query q = this.em.createNamedQuery("Broadcast.findForConnectionCampaign");
		q.setParameter("campaignId", campaignId);
		q.setParameter("connection", connection);
		bcast = (Broadcast) q.getResultList().stream().findFirst().orElse(null);
		
		
		CampaignVO campaign = cacheService.getCampaignVO(campaignId);
		
		if (bcast == null) {
			if (Controller.getDtsConfig().getDebug()) {
				logger.info("getBroadcastForScheduled: connectionId: " + connectionId + " no session for campaign " + campaign.getName() + " id: " + campaign.getId());
			}
			bcast = new Broadcast();
			bcast.setId(UUID.randomUUID());
			bcast.setCampaignId(campaignId);
			bcast.setConnection(connection);
			bcast.setSubmittedViewed(0);
			bcast.setSubmittedReceived(0);
			bcast.setSubmitted(0);
			bcast.setTs(Instant.now());
			bcast.setManagement(campaign.getManagement());
			bcast.setLastSentTs(Instant.now());
			em.persist(bcast);
			
			
		} else {
			if ((!onlyIfNotViewed) || (bcast.getSubmittedReceived() == null) || (bcast.getSubmittedReceived() == 0)) {
				bcast.setLastSentTs(Instant.now());
				bcast = em.merge(bcast);
			} else {
				return null;
			}
			
		}
		
		return bcast.getId();
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
