package io.twentysixty.dts.conversational.jms;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.dts.conversational.svc.BcastService;
import io.twentysixty.dts.conversational.svc.Controller;
import io.twentysixty.dts.conversational.svc.MessagingService;
import io.twentysixty.orchestrator.stats.DtsStat;
import io.twentysixty.sa.client.jms.AbstractConsumer;
import io.twentysixty.sa.client.jms.ConsumerInterface;
import io.twentysixty.sa.client.model.event.ConnectionStateUpdated;
import io.twentysixty.sa.client.model.event.Event;
import io.twentysixty.sa.client.model.event.MessageReceived;
import io.twentysixty.sa.client.model.event.MessageState;
import io.twentysixty.sa.client.model.event.MessageStateUpdated;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.ContextualMenuSelect;
import io.twentysixty.sa.client.model.message.InvitationMessage;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuSelectMessage;
import io.twentysixty.sa.client.model.message.MessageReceiptOptions;
import io.twentysixty.sa.client.model.message.ReceiptsMessage;
import io.twentysixty.sa.client.model.message.TextMessage;
import io.twentysixty.sa.client.util.JsonUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

@ApplicationScoped
public class MoConsumer extends AbstractConsumer<Event> implements ConsumerInterface<Event> {

	@Inject MessagingService gaiaService;
	@Inject BcastService bcastService;
	@Inject MessagingService messagingService;

	
	@Inject
    ConnectionFactory _connectionFactory;


	@ConfigProperty(name = "io.twentysixty.dts.jms.ex.delay")
	Long _exDelay;


	@ConfigProperty(name = "io.twentysixty.dts.jms.mo.queue.name")
	String _queueName;

	@ConfigProperty(name = "io.twentysixty.dts.jms.mo.consumer.threads")
	Integer _threads;

	
	private static final Logger logger = Logger.getLogger(MoConsumer.class);

	@Inject MtProducer mtProducer;

	@Inject Controller controller;
	
	void onStart(@Observes StartupEvent ev) {

		//logger.info("onStart: Service Agent Consumer [MoConsumer] queueName: " + _queueName);

		this.setExDelay(_exDelay);
		this.setDebug(controller.isDebugEnabled());
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);
		//super._onStart();

    }

    void onStop(@Observes ShutdownEvent ev) {

    	//logger.info("onStop: Service Agent Consumer [MoConsumer]");

    	//super._onStop();

    	if (!this.isStopped()) {
    		super._onStop();
    	}
    	
    }
    
   
    @Override
	public void receiveMessage(Event event) throws Exception {

    	
    	if (event instanceof MessageReceived) {
    		BaseMessage message = ((MessageReceived) event).getMessage();
    		
    		gaiaService.userInput(message);
    		
    		List<MessageReceiptOptions> receipts = new ArrayList<>();

    		

    		MessageReceiptOptions viewed = new MessageReceiptOptions();
    		viewed.setMessageId(message.getId());
    		viewed.setTimestamp(Instant.now());
    		viewed.setState(MessageState.VIEWED);
    		receipts.add(viewed);

    		ReceiptsMessage r = new ReceiptsMessage();
    		r.setConnectionId(message.getConnectionId());
    		r.setReceipts(receipts);

    		
    		
    		try {
    			mtProducer.sendMessage(r);
    		} catch (Exception e) {
    			logger.error("", e);
    		}

    	} else if (event instanceof MessageStateUpdated) {
    		MessageStateUpdated msu = (MessageStateUpdated) event;
    		
    		
    		bcastService.messageStateChanged(msu);
        	
    		
    	} else if (event instanceof ConnectionStateUpdated) {
    		ConnectionStateUpdated csu = (ConnectionStateUpdated) event;
    		
    		switch (csu.getState()) {
    		case COMPLETED: {
    			messagingService.newConnection(csu);
    			break;
    		}
    		case TERMINATED: {
    			messagingService.deleteConnection(csu);
    			break;
    		}
    		default: {
    			logger.warn("receiveMessage: ignoring message (not implemented) " + JsonUtil.serialize(csu, false));
    		}
    		}
    		
    	}
    	
    	
    	
    	

		if (controller.isDebugEnabled()) {
			try {
				logger.info("receiveMessage: event:" + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}

	}


    private Object controlerLockObj = new Object();
    private boolean started = false;
    private boolean stopped = true;
    
    public void start() {
    	logger.info("start: starting Service Agent Consumers [MoConsumer]");
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
    	logger.info("stop: stopping Service Agent Consumers [MoConsumer]");
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
			return started;
		}
	}

	

	public boolean isStopped() {
		synchronized (controlerLockObj) {
			return stopped;
		}
		
	}

}
