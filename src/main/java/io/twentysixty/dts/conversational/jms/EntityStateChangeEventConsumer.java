package io.twentysixty.dts.conversational.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.mobiera.commons.ApiConstants;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.dts.conversational.svc.Controller;
import io.twentysixty.orchestrator.api.EntityStateChangeEvent;
import io.twentysixty.sa.client.jms.AbstractConsumer;
import io.twentysixty.sa.client.jms.ConsumerInterface;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.res.c.v1.MessageResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

@ApplicationScoped
public class EntityStateChangeEventConsumer extends AbstractConsumer<EntityStateChangeEvent> implements ConsumerInterface<EntityStateChangeEvent> {

	@RestClient
	@Inject
	MessageResource messageResource;


	@Inject
    ConnectionFactory _connectionFactory;


	@ConfigProperty(name = "io.twentysixty.dts.jms.ex.delay")
	Long _exDelay;

	
	@ConfigProperty(name = "io.twentysixty.dts.jms.event.consumer.threads")
	Integer _threads;

	@Inject Controller controller;

	private static final Logger logger = Logger.getLogger(EntityStateChangeEventConsumer.class);



	void onStart(@Observes StartupEvent ev) {

		//logger.info("onStart: EntityStateChangeEventConsumer");

		
    }

    void onStop(@Observes ShutdownEvent ev) {

    	//logger.info("onStop: EntityStateChangeEventConsumer");

    	if (!this.isStopped()) {
    		super._onStop();
    	}
    	

    }

    @Override
	public void receiveMessage(EntityStateChangeEvent message) throws Exception {

		controller.entityStateChangeEventReceived(message);

	}

    private Object controlerLockObj = new Object();
    private boolean started = false;
    private boolean stopped = true;
    
    public void start() {
    	logger.info("start: starting Orchestrator Event Consumer [EntityStateChangeEventConsumer]");
    	
    	synchronized (controlerLockObj) {
    		try {
    			started = true;
    			this.setExDelay(_exDelay);
    			this.setDebug(controller.isDebugEnabled());
    			this.setQueueName(Controller.getRegistryId().toString());
    			this.setThreads(_threads);
    			this.setConnectionFactory(_connectionFactory);
    			super._onStart();

    			
    			stopped = false;
    		} catch (Exception e) {
    			logger.error("start: ", e);
    		}
    	}
    	
    }
    
    public void stop() {
    	logger.info("stop: stopping Orchestrator Event Consumer [EntityStateChangeEventConsumer]");
    	synchronized (controlerLockObj) {
    		try {
    			stopped = true;
    			super._onStop();
    			started = false;
    		} catch (Exception e) {
    			logger.error("start: ", e);
    		}
    	}
    	
    }

	public boolean isStarted() {
		
		
		synchronized (controlerLockObj) {
			try {
				return started;
			} catch (Exception e) {
    			logger.error("isStarted: ", e);
    		}
		}
		return false;
	}

	

	public boolean isStopped() {
		synchronized (controlerLockObj) {
			try {
				return stopped;
			} catch (Exception e) {
    			logger.error("isStarted: ", e);
    		}
		}
		return false;
	}

	@Override
	public String getMessageSelector() {
		return EntityStateChangeEvent.INSTANCE_ID_NAME + "='" + Controller.getInstanceid() + "'";
	}

}
