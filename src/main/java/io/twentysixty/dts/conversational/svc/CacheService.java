package io.twentysixty.dts.conversational.svc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.graalvm.collections.Pair;
import org.jboss.logging.Logger;

import io.twentysixty.orchestrator.api.CmSchedule;
import io.twentysixty.orchestrator.api.vo.CampaignVO;
import io.twentysixty.orchestrator.res.c.CampaignResource;
import io.twentysixty.orchestrator.res.c.CampaignScheduleResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;



@ApplicationScoped
public class CacheService {

	@RestClient
	@Inject
	CampaignScheduleResource csResource;

	@RestClient
	@Inject
	CampaignResource campaignResource;

	@ConfigProperty(name = "io.twentysixty.orchestrator.cache.lifetime")
	Integer cacheLifetime;

	@Inject Controller controller;
	
	private static Logger logger = Logger.getLogger(CacheService.class);

	
	private static HashMap<UUID, Pair<Long, CampaignVO>> campaignCache = new HashMap<UUID, Pair<Long, CampaignVO>>();
	
	
	public CampaignVO getCampaignVO(UUID id) {
		Pair<Long, CampaignVO> pair = null;
		
		synchronized (campaignCache) {
			pair = campaignCache.get(id);
			if ((pair == null) || ( (System.currentTimeMillis() - pair.getLeft()) > cacheLifetime )) {
				
				try {
					Response response = campaignResource.getCampaign(id);
					CampaignVO campaign = response.readEntity(CampaignVO.class);
					if (campaign != null) {
						pair = Pair.create(System.currentTimeMillis(), campaign);
						campaignCache.put(id, pair);
					}
				} catch (Exception e) {
					campaignCache.remove(id);
					logger.error("getCampaignVO", e);				}
				
			}
			
		}
		
		return pair.getRight();
		
	}
	
	

	private static List<io.twentysixty.orchestrator.api.CmSchedule> cachedSchedules = null;
	private static Long cachedSchedulesTs = null;
	private static Object cachedSchedulesLockObj = new Object();
	
	public List<CmSchedule> getEnabledSchedules() {
		
		synchronized(cachedSchedulesLockObj) {
			if ((cachedSchedulesTs == null) || ( (System.currentTimeMillis() - cachedSchedulesTs) > cacheLifetime )) {
				
				try {
					Response response = csResource.getSchedules(Controller.getDtsConfig().getId(), true);
					cachedSchedules = (List<CmSchedule> ) response.readEntity(new GenericType<List<CmSchedule>>() {});
					
					
					
					
				} catch (Exception e) {
					logger.error("getSchedules: " + e.getMessage(), e);
					cachedSchedules = new ArrayList<CmSchedule>(0);
				}
				
				
			}
		}
		
		
		return cachedSchedules;
	}

}
