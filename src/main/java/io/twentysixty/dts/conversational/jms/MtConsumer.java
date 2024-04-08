package io.twentysixty.dts.conversational.jms;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.dts.conversational.svc.Controller;
import io.twentysixty.sa.client.jms.AbstractConsumer;
import io.twentysixty.sa.client.jms.ConsumerInterface;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.res.c.MessageResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

@ApplicationScoped
public class MtConsumer extends AbstractConsumer<BaseMessage> implements ConsumerInterface<BaseMessage> {

	@RestClient
	@Inject
	MessageResource messageResource;


	@Inject
    ConnectionFactory _connectionFactory;


	@ConfigProperty(name = "io.twentysixty.dts.conversational.jms.ex.delay")
	Long _exDelay;


	@ConfigProperty(name = "io.twentysixty.dts.conversational.jms.mt.queue.name")
	String _queueName;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.jms.mt.consumer.threads")
	Integer _threads;

	@Inject Controller controller;

	private static final Logger logger = Logger.getLogger(MtConsumer.class);



	void onStart(@Observes StartupEvent ev) {

		//logger.info("onStart: Backend Consumer [MtConsumer] queueName: " + _queueName);

		this.setExDelay(_exDelay);
		this.setDebug(controller.isDebugEnabled());
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);
		//super._onStart();

    }

    void onStop(@Observes ShutdownEvent ev) {

    	//logger.info("onStop: Backend Consumer [MtConsumer]");

    	if (!this.isStopped()) {
    		super._onStop();
    	}
    	

    }

    @Override
	public void receiveMessage(BaseMessage message) throws Exception {

		messageResource.sendMessage(message);

	}

    private Object controlerLockObj = new Object();
    private boolean started = false;
    private boolean stopped = true;
    
    public void start() {
    	
    	logger.info("start: starting Backend Consumers [MtConsumer]");
    	
    	
    	synchronized (controlerLockObj) {
    		try {
    			started = true;
    			super._onStart();
    			stopped = false;
    		} catch (Exception e) {
    			logger.error("start: ", e);
    		}
    	}
    	
    }
    
    public void stop() {
    	
    	logger.info("stop: stopping Backend Consumers [MtConsumer]");
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

	

}
