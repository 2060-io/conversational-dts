package io.twentysixty.dts.conversational.svc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.twentysixty.dts.conversational.jms.MoConsumer;
import io.twentysixty.dts.conversational.jms.MtConsumer;
import io.twentysixty.orchestrator.api.EntityStateChangeEvent;
import io.twentysixty.orchestrator.api.RegisterResponse;
import io.twentysixty.orchestrator.api.enums.App;
import io.twentysixty.orchestrator.api.enums.EntityType;
import io.twentysixty.orchestrator.api.util.JsonUtil;
import io.twentysixty.orchestrator.api.vo.ConversationalServiceVO;
import io.twentysixty.orchestrator.res.c.ConversationalServiceResource;
import io.twentysixty.orchestrator.res.c.RegisterResource;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

@Singleton
public class Controller {

	private Object registerLock = new Object();
	
	private static final Logger logger = Logger.getLogger("Controller");

	private static boolean startedRegister = true;
	
	private static final UUID instanceId = UUID.randomUUID();
	private static UUID registryId = null;
	
	private static Instant expireRegisterTs = Instant.now();
	
	private static ConversationalServiceVO dtsConfig = null;
	
	private static final long minimumRegisterWaitTimeMs = 5000l;
	
	@ConfigProperty(name = "io.twentysixty.dts.orchestrator")
	Boolean orchestrator;

	@ConfigProperty(name = "io.twentysixty.dts.entity_id")
	UUID entityId;

	
	@RestClient
	@Inject
	RegisterResource registerResource;
	
	@RestClient
	@Inject
	ConversationalServiceResource conversationalServiceResource;
	
	@Inject
	MtConsumer mtConsumer;
	@Inject
	MoConsumer moConsumer;
	
	private static ExecutorService executor = Executors.newCachedThreadPool();
	
	void onStart(@Observes StartupEvent ev) {
		
		
		this.startRegisterTask();
		
    }
	
	void onStop(@Observes ShutdownEvent ev) {               
    
		this.stopRegisterTask();
	}

	
	
	
	public void startRegisterTask() {
		Uni.createFrom().item(UUID::randomUUID).emitOn(executor).subscribe().with(
                t -> {
					try {
						startRegisterTask(t);
					} catch (Exception e) {
						logger.error("startRegisterTask: cannot run startRegisterTask", e);
					}
				}, Throwable::printStackTrace
        );
	}

	private Uni<Void> startRegisterTask(UUID uuid) throws Exception {
		
		logger.info("startRegisterTask: " + uuid + " starting");
		
		startedRegister = true;

		logger.info("startRegisterTask: " + uuid + " building route maps");
		

		while(startedRegister) {
			boolean startStop = false;
			
			try {
				
				if (orchestrator) {
					
					try {
						Response response = registerResource.register(App.CONVERSATIONAL_SERVICE, entityId, instanceId);
						
						if (response.getStatus()<300) {
							RegisterResponse rr = (RegisterResponse) response.readEntity(RegisterResponse.class);
							registryId = rr.getRegistryId();
							expireRegisterTs = rr.getExpireTs();
						}
					} catch (Exception e) {
						logger.error("registerTask: cannot register: ", e);
					}
					
					if ((dtsConfig == null) && (expireRegisterTs != null)) {
						// null config, getting
						
						try {
							Response getMyServiceResponse = conversationalServiceResource.get(entityId);
							
							if (getMyServiceResponse.getStatus()<300) {
								dtsConfig = (ConversationalServiceVO) getMyServiceResponse.readEntity(ConversationalServiceVO.class);
								startStop = true;
							}
							
						} catch (Exception e) {
							expireRegisterTs = null;
							logger.error("registerTask: unable to get myService config: ", e);
						}
						
					}
					
				} else {
					// standalone
					
					
					
				}
				if (startStop) {
					startStop();
				}
				long toWait = minimumRegisterWaitTimeMs;
				
				if (expireRegisterTs != null) {
					toWait = (expireRegisterTs.toEpochMilli() - Instant.now().toEpochMilli())/2;
					if (toWait <minimumRegisterWaitTimeMs) toWait = minimumRegisterWaitTimeMs;
				}
				
				synchronized (registerLock) {
					try {
						registerLock.wait(toWait);
					} catch (Exception e) {
						logger.error("registerTask: wait interrupted");
					}
				}
				
			} catch (Exception e) {
				logger.error("registerTask:", e);
				
				synchronized (registerLock) {
					try {
						registerLock.wait(minimumRegisterWaitTimeMs);
					} catch (Exception e1) {
						logger.error("registerTask: wait interrupted");
					}
				}
			}
		}
		
		
		logger.info("registerTask: exiting " + uuid);
		
		
		
		return Uni.createFrom().voidItem();
    
		
	}
	
	public void stopRegisterTask() {
		startedRegister = false;
		synchronized (registerLock) {
			registerLock.notifyAll();
		}
		logger.warn("startRegisterTask: notified");
	}

	public static UUID getRegistryId() {
		return registryId;
	}

	public static ConversationalServiceVO getDtsConfig() {
		return dtsConfig;
	}
	
	public void serviceStatusEventReceived(EntityStateChangeEvent event) throws InterruptedException {
		
		if (isDebugEnabled()) {
			try {
				logger.info("serviceStatusEventReceived: notification received " + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				
			}
		}
		
		boolean startStop = false;
		
		if (event.getApp().equals(App.CONVERSATIONAL_SERVICE)) {
			if (event.getAppEntityId().equals(getDtsConfig().getId())) {
				if (event.getAppInstanceId().equals(instanceId)) {
					// really for me
					
					if (event.getChangedEntityId().equals(getDtsConfig().getId())) {
						if (event.getChangedEntityType().equals(EntityType.CONVERSATIONAL_SERVICE)) {
							// my entity
							
							try {
								Response getMyServiceResponse = conversationalServiceResource.get(entityId);
								if (getMyServiceResponse.getStatus()<300) {
									
									dtsConfig = (ConversationalServiceVO) getMyServiceResponse.readEntity(ConversationalServiceVO.class);
									startStop = true;
								}
								
							} catch (Exception e) {
								dtsConfig = null;
								logger.error("registerTask: unable to get Conversational Service config: ", e);
							}
							
							
						}
					}
					
				}
			}
		}
		
		if (startStop) {
			startStop();
		}
		
		
		
	}
	
	private static Object controlerLockObj = new Object();
	
	private void startStop() {

		
		synchronized (controlerLockObj) {
			try {
				
				switch (getDtsConfig().getState()) {
				case ENABLED:
				case PAUSED:
				case TESTING:
				case REFUSED:
				case CERTIFIED:
				case PENDING:
				{
					if (!mtConsumer.isStarted()) {
						mtConsumer.start();
					}
					if (!moConsumer.isStarted()) {
						moConsumer.start();
					}
					
					
					break;
				}
				case DISABLED:
				case EDITING:
				case ARCHIVED:
				
				{
					
					logger.info("serviceStatusEventReceived: notification received, stopping Conversational Service " + getDtsConfig().getName());
					
					if (!mtConsumer.isStopped()) {
						mtConsumer.stop();
					}
					if (!moConsumer.isStopped()) {
						moConsumer.stop();
					}
					break;
				}
				
				
				}
			} catch (Exception e) {
				
			}
		}
		
	}
	

	public boolean isDebugEnabled() {
		if (getDtsConfig() == null) return true;
		
		else return getDtsConfig().getDebug();
	}
}
