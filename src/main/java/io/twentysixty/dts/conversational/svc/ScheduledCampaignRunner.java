package io.twentysixty.dts.conversational.svc;


import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.graalvm.collections.Pair;
import org.jboss.logging.Logger;


import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.twentysixty.dts.conversational.jms.StatProducer;
import io.twentysixty.orchestrator.api.CmSchedule;
import io.twentysixty.orchestrator.api.util.JsonUtil;

@ApplicationScoped
public class ScheduledCampaignRunner {

	
	@Inject
	Controller controller;
	
	@Inject
	BcastService bcastService;
	
	
	@ConfigProperty(name = "io.twentysixty.dts.bcast.scheduled.threads")
	Integer threads;
	
	@ConfigProperty(name = "io.twentysixty.dts.bcast.scheduled.sleep")
	Long sleep;
	
	@ConfigProperty(name = "io.twentysixty.dts.bcast.scheduled.qty")
	Integer qty;
	
	@Inject StatProducer statProducer;
	
	
	@Inject
	CacheService cacheService;
	
	
	private Map<UUID,Object> lockObjs = new HashMap<UUID,Object>();
	
	private static final Logger logger = Logger.getLogger(ScheduledCampaignRunner.class);
	private Map<UUID,Boolean> runnings = new HashMap<UUID,Boolean>();
	private Map<UUID,Boolean> starteds = new HashMap<UUID,Boolean>();

    
    
    void onStart(@Observes StartupEvent ev) {
    	//scheduler.submit(this);
    	
    	for (int i=0; i<threads;i++) {
			logger.info("onStart: starting scheduled campaign runner #" + i);
			UUID uuid = UUID.randomUUID();
			starteds.put(uuid, true);
			launchRunner(uuid);
			
		}
    	
    }

    void onStop(@Observes ShutdownEvent ev) {
    	
    	for (UUID uuid: lockObjs.keySet()) {
    		stopRunner(uuid);
    	}
    	
    }
 
    
    private static Object shutdownLockObj = new Object();
    
	public void stopRunner(UUID uuid) {
		
		synchronized(starteds) {
			starteds.put(uuid, false);
		}
		
		
		Object lockObj = lockObjs.get(uuid);
		
		if (lockObj != null) {
			synchronized (lockObj) {
				lockObj.notifyAll();
			}
		}
		
		while (true) {
			
			Boolean running = null;
			synchronized (runnings) {
				running = runnings.get(uuid);
			}
			if (!running) {
				break;
			
			}
			synchronized (shutdownLockObj) {
				try {
					shutdownLockObj.wait(100);
				} catch (InterruptedException e) {
					
				}
				synchronized (lockObj) {
					lockObj.notifyAll();
				}
			}
		}
		
		
		logger.info("stopConsumer: stopped: " + uuid);
		
	}
	
	
	Uni<Void> startRunner(UUID uuid) {


		Object lockObj = new Object();
		synchronized (lockObjs) {
			lockObjs.put(uuid, lockObj);
		}
		synchronized (runnings) {
			runnings.put(uuid, true);
		}


		long now = System.currentTimeMillis();
		synchronized (lockObj) {
			try {
				lockObj.wait(10000l);
			} catch (InterruptedException e) {

			}
		}

		Float qu = null;
		int i = 0;

		while (true) {
			Boolean started = null;
			synchronized(starteds) { 
				started = starteds.get(uuid);

			}
			if ((started == null) || (!started)) {
				break;
			}


			logger.info("startRunner: running " + uuid);

			try  {

				now = System.currentTimeMillis();
				if (controller.getDtsConfig() != null) {
					try {
						qu = bcastService.getQueueUsage();
					} catch (Exception e) {
						logger.error("startRunner: unable to get queue usage "+ uuid, e);
					}


					if (qu != null) {
						
							if (qu <0.5f) {
								if (Controller.getDtsConfig().getDebug()) {
									logger.info("startRunner: loop1 uuid " + uuid + " qu: " + qu);
								}
								
								
								
								Pair<CmSchedule, Integer> ss = bcastService.selectCampaignToRun(qty);
								
								if (Controller.getDtsConfig().getDebug()) {
									try {
										logger.info("startRunner: " + JsonUtil.serialize(ss, false));
									} catch (Exception e) {
										
									}
								}
								
								if (ss != null) {
									if (Controller.getDtsConfig().getDebug()) {
										logger.info("startRunner: loop2 uuid " + uuid + " qu: " + qu);
									}
									List<UUID> selectedConnections = bcastService.lockAndGetConnections(ss.getRight());
									
									if (Controller.getDtsConfig().getDebug()) {
										logger.info("startRunner: loop3 uuid " + uuid + " qu: " + qu);
									}
									if ((selectedConnections != null) && (selectedConnections.size()>0)) {
										
										for (UUID selectedConnection: selectedConnections) {
											if (Controller.getDtsConfig().getDebug()) {
												logger.info("startRunner: runScheduled " + selectedConnection + " uuid " + uuid + " qu: " + qu + " " + i);
											}
											try {
												bcastService.runScheduledCampaign(selectedConnection, ss.getLeft().getCampaignFk(), ss.getLeft().getCsId());
												
												
											} catch (Exception e) {
												logger.warn("startRunner: runScheduled " + selectedConnection + " uuid " + uuid + " error " + e.getMessage(), e);
											}
										}
										
										if (selectedConnections.size()!=qty) {
											synchronized (lockObj) {
												try {
													lockObj.wait(sleep);
												} catch (InterruptedException e) {

												}
											}
										} 
										
									} else {

										if (Controller.getDtsConfig().getDebug()) {
											logger.info("startRunner: nothing to run sleeping... uuid " + uuid );
										}
										synchronized (lockObj) {
											try {
												lockObj.wait(sleep);
											} catch (InterruptedException e) {

											}
										}

										if (Controller.getDtsConfig().getDebug()) {
											logger.info("startRunner: nothing to run, slept uuid " + uuid + " EOS");
										}
									}
									
								} else {

									if (Controller.getDtsConfig().getDebug()) {
										logger.info("startRunner: nothing to run sleeping... uuid " + uuid );
									}
									synchronized (lockObj) {
										try {
											lockObj.wait(sleep);
										} catch (InterruptedException e) {

										}
									}

									if (Controller.getDtsConfig().getDebug()) {
										logger.info("startRunner: nothing to run, slept uuid " + uuid + " EOS");
									}
								}
							} else {
								if (Controller.getDtsConfig().getDebug()) {
									logger.info("startRunner: queue overloaded: " + qu + " uuid " + uuid);
								}
								synchronized (lockObj) {
									try {
										lockObj.wait(sleep);
									} catch (InterruptedException e) {

									}
								}

								if (Controller.getDtsConfig().getDebug()) {
									logger.info("startRunner: queue overloaded, slept " + uuid + " EOS");
								}
							}
						
					} else {
						if (Controller.getDtsConfig().getDebug()) {
							logger.info("startRunner: cannot get queue usage, sleeping... uuid " + uuid);
						}
						synchronized (lockObj) {
							try {
								lockObj.wait(sleep);
							} catch (InterruptedException e) {

							}
						}

						if (Controller.getDtsConfig().getDebug()) {
							logger.info("startRunner: cannot get queue usage, sleeping... uuid: #" + uuid + " EOS");
						}
					}

				} else {
					logger.warn("startRunner: waiting for controller to be ready " + uuid);

					synchronized (lockObj) {
						try {
							lockObj.wait(sleep);
						} catch (InterruptedException e) {

						}
					}
				}




			} catch (Exception e) {
				logger.error("", e);
				synchronized (lockObj) {
					try {
						lockObj.wait(sleep);
					} catch (InterruptedException e1) {
					}
				}
			}
		}
		synchronized (runnings) {
			runnings.put(uuid, false);
		}

		logger.info("startRunner: exiting for UUID " + uuid);
		return Uni.createFrom().voidItem();
	}

    
    public void launchRunner(UUID uuid) {
		Uni.createFrom().item(uuid).emitOn(Infrastructure.getDefaultWorkerPool()).subscribe().with(
                this::startRunner, Throwable::printStackTrace
        );
	}

	
    
    
   
}