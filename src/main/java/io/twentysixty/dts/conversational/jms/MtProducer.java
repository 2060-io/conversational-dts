package io.twentysixty.dts.conversational.jms;


import java.time.Instant;
import java.util.ArrayList;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.mobiera.ms.commons.stats.api.StatEnum;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.dts.conversational.svc.Controller;
import io.twentysixty.orchestrator.stats.DtsStat;
import io.twentysixty.orchestrator.stats.OrchestratorStatClass;
import io.twentysixty.sa.client.jms.AbstractProducer;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.ContextualMenuUpdate;
import io.twentysixty.sa.client.model.message.InvitationMessage;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuDisplayMessage;
import io.twentysixty.sa.client.model.message.MessageReceiptOptions;
import io.twentysixty.sa.client.model.message.ReceiptsMessage;
import io.twentysixty.sa.client.model.message.TextMessage;
import io.twentysixty.sa.client.util.JsonUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;


@ApplicationScoped
public class MtProducer extends AbstractProducer<BaseMessage> {

	@Inject
    ConnectionFactory _connectionFactory;

	
	@ConfigProperty(name = "io.twentysixty.dts.conversational.jms.ex.delay")
	Long _exDelay;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.jms.mt.queue.name")
	String _queueName;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.jms.mt.producer.threads")
	Integer _threads;

	@Inject Controller controller;


	@Inject StatProducer statProducer;


	private static final Logger logger = Logger.getLogger(MtProducer.class);


    void onStart(@Observes StartupEvent ev) {
    	//logger.info("onStart: BeProducer");

    	this.setExDelay(_exDelay);
		this.setDebug(controller.isDebugEnabled());
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);

    	this.setProducerCount(_threads);

    }

    void onStop(@Observes ShutdownEvent ev) {

    	//logger.info("onStop: BeProducer");
    }


    @Override
    public void sendMessage(BaseMessage message) throws Exception {
    	
    	
    	if (Controller.getDtsConfig() != null) {
    		if(controller.isDebugEnabled()) {
        		logger.info("sendMessage: " + JsonUtil.serialize(message, false));
        	}
        	ArrayList<StatEnum> lenum = new ArrayList<StatEnum>(1);
        	lenum.add(DtsStat.SENT_MSG);
        	
        	try {
        		this.spool(message, 0);
        	} catch (Exception e) {
        		lenum.add(DtsStat.SENT_MSG_ERROR);
        		statProducer.spool(OrchestratorStatClass.DTS.toString(), Controller.getDtsConfig().getId(), lenum, Instant.now(), 1);
        		throw e;
        	}
        	
        	
        	lenum.add(DtsStat.SENT_MSG_SPOOLED);
        	
    	
    	
        	if (message instanceof TextMessage) {
        		lenum.add(DtsStat.SENT_MSG_TEXT);
        	} else if (message instanceof MenuDisplayMessage) {
        		lenum.add(DtsStat.SENT_MSG_MENU_DISPLAY);
        	} else if (message instanceof MediaMessage) {
        		lenum.add(DtsStat.SENT_MSG_MEDIA);
        	} else if (message instanceof InvitationMessage) {
        		lenum.add(DtsStat.SENT_MSG_INVITATION);
        	} else if (message instanceof ContextualMenuUpdate) {
        		lenum.add(DtsStat.SENT_MSG_CTX_MENU_UPDATE);
        	} else if (message instanceof ReceiptsMessage) {
        		ReceiptsMessage rm = (ReceiptsMessage) message;
        		for (MessageReceiptOptions o: rm.getReceipts()) {
        			switch (o.getState()) {
        			case RECEIVED: {
        				lenum.add(DtsStat.RECEIVED_MSG_RECEIVED);
        				break;
        			}
        			case CREATED: {
        				lenum.add(DtsStat.RECEIVED_MSG_CREATED);
        				break;
        			}
        			case SUBMITTED: {
        				lenum.add(DtsStat.RECEIVED_MSG_SUBMITTED);
        				break;
        			}
        			case VIEWED: {
        				lenum.add(DtsStat.RECEIVED_MSG_VIEWED);
        				break;
        			}
        			case DELETED: {
        				lenum.add(DtsStat.RECEIVED_MSG_DELETED);
        				break;
        			}
        			}
        		}
        	} else {
        		lenum.add(DtsStat.SENT_MSG_OTHERS);
        	}
        	
        	
        	
        	statProducer.spool(OrchestratorStatClass.DTS.toString(), Controller.getDtsConfig().getId(), lenum, Instant.now(), 1);
    		
    		
    		
    	} else {
    		logger.error("sendMessage: ignoring message as DTS not registered " + JsonUtil.serialize(message, false));
    	}
    	
    	
		
		
    }




}