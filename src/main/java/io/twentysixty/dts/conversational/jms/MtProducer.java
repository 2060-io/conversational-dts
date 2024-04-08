package io.twentysixty.dts.conversational.jms;


import java.time.Instant;
import java.util.ArrayList;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.mobiera.ms.commons.stats.api.StatEnum;
import com.mobiera.ms.mno.aircast.stats.AircastStatClass;
import com.mobiera.ms.mno.aircast.stats.DidcommServiceStat;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.twentysixty.sa.client.jms.AbstractProducer;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.ContextualMenuUpdate;
import io.twentysixty.sa.client.model.message.InvitationMessage;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuDisplayMessage;
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
public class MtProducer extends AbstractProducer {

	@Inject
    ConnectionFactory _connectionFactory;


	@ConfigProperty(name = "io.twentysixty.hologram.welcome.jms.ex.delay")
	Long _exDelay;

	@ConfigProperty(name = "io.twentysixty.hologram.welcome.jms.mt.queue.name")
	String _queueName;

	@ConfigProperty(name = "io.twentysixty.hologram.welcome.jms.mt.producer.threads")
	Integer _threads;

	@ConfigProperty(name = "io.twentysixty.hologram.welcome.debug")
	Boolean _debug;


	@ConfigProperty(name = "io.twentysixty.aircast.didcomm.service.id")
	Long didcommServiceId;

	@Inject StatProducer statProducer;


	private static final Logger logger = Logger.getLogger(MtProducer.class);


    void onStart(@Observes StartupEvent ev) {
    	logger.info("onStart: BeProducer");

    	this.setExDelay(_exDelay);
		this.setDebug(_debug);
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);

    	this.setProducerCount(_threads);

    }

    void onStop(@Observes ShutdownEvent ev) {

    	logger.info("onStop: BeProducer");
    }


    @Override
    public void sendMessage(BaseMessage message) throws Exception {
    	if(_debug) {
    		logger.info("sendMessage: " + JsonUtil.serialize(message, false));
    	}
    	ArrayList<StatEnum> lenum = new ArrayList<StatEnum>(1);
    	lenum.add(DidcommServiceStat.SENT_MSG);
    	
    	try {
    		this.spool(message, 0);
    	} catch (Exception e) {
    		lenum.add(DidcommServiceStat.SENT_MSG_ERROR);
    		statProducer.spool(AircastStatClass.DIDCOMM_SERVICE.toString(), didcommServiceId, lenum, Instant.now(), 1);
    		throw e;
    	}
    	
    	
    	lenum.add(DidcommServiceStat.SENT_MSG_SPOOLED);
    	
	
	
    	if (message instanceof TextMessage) {
    		lenum.add(DidcommServiceStat.SENT_MSG_TEXT);
    	} else if (message instanceof MenuDisplayMessage) {
    		lenum.add(DidcommServiceStat.SENT_MSG_MENU_DISPLAY);
    	} else if (message instanceof MediaMessage) {
    		lenum.add(DidcommServiceStat.SENT_MSG_MEDIA);
    	} else if (message instanceof InvitationMessage) {
    		lenum.add(DidcommServiceStat.SENT_MSG_INVITATION);
    	} else if (message instanceof ContextualMenuUpdate) {
    		lenum.add(DidcommServiceStat.SENT_MSG_CTX_MENU_UPDATE);
    	} else if (message instanceof ReceiptsMessage) {
    		ReceiptsMessage rm = (ReceiptsMessage) message;
    		for (MessageReceiptOptions o: rm.getReceipts()) {
    			switch (o.getState()) {
    			case RECEIVED: {
    				lenum.add(DidcommServiceStat.RECEIVED_MSG_RECEIVED);
    				break;
    			}
    			case CREATED: {
    				lenum.add(DidcommServiceStat.RECEIVED_MSG_CREATED);
    				break;
    			}
    			case SUBMITTED: {
    				lenum.add(DidcommServiceStat.RECEIVED_MSG_SUBMITTED);
    				break;
    			}
    			case VIEWED: {
    				lenum.add(DidcommServiceStat.RECEIVED_MSG_VIEWED);
    				break;
    			}
    			case DELETED: {
    				lenum.add(DidcommServiceStat.RECEIVED_MSG_DELETED);
    				break;
    			}
    			}
    		}
    	} else {
    		lenum.add(DidcommServiceStat.SENT_MSG_OTHERS);
    	}
    	
    	
    	
    	statProducer.spool(AircastStatClass.DIDCOMM_SERVICE.toString(), didcommServiceId, lenum, Instant.now(), 1);
		
		
		
		
		
    }




}