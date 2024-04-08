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
import io.twentysixty.sa.client.jms.ProducerInterface;
import io.twentysixty.sa.client.model.message.BaseMessage;
import io.twentysixty.sa.client.model.message.ContextualMenuSelect;
import io.twentysixty.sa.client.model.message.ContextualMenuUpdate;
import io.twentysixty.sa.client.model.message.InvitationMessage;
import io.twentysixty.sa.client.model.message.MediaMessage;
import io.twentysixty.sa.client.model.message.MenuSelectMessage;
import io.twentysixty.sa.client.model.message.MessageReceiptOptions;
import io.twentysixty.sa.client.model.message.ReceiptsMessage;
import io.twentysixty.sa.client.model.message.TextMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;


@ApplicationScoped
public class MoProducer extends AbstractProducer implements ProducerInterface {

	@Inject
    ConnectionFactory _connectionFactory;


	@ConfigProperty(name = "io.twentysixty.hologram.welcome.jms.ex.delay")
	Long _exDelay;

	@ConfigProperty(name = "io.twentysixty.hologram.welcome.jms.mo.queue.name")
	String _queueName;

	@ConfigProperty(name = "io.twentysixty.hologram.welcome.jms.mo.producer.threads")
	Integer _threads;

	@ConfigProperty(name = "io.twentysixty.hologram.welcome.debug")
	Boolean _debug;

	@ConfigProperty(name = "io.twentysixty.aircast.didcomm.service.id")
	Long didcommServiceId;

	@Inject StatProducer statProducer;



	private static final Logger logger = Logger.getLogger(MoProducer.class);


    void onStart(@Observes StartupEvent ev) {
    	logger.info("onStart: SaProducer");

    	this.setExDelay(_exDelay);
		this.setDebug(_debug);
		this.setQueueName(_queueName);
		this.setThreads(_threads);
		this.setConnectionFactory(_connectionFactory);
    	this.setProducerCount(_threads);

    }

    void onStop(@Observes ShutdownEvent ev) {
    	logger.info("onStop: SaProducer");
    }


    @Override
    public void sendMessage(BaseMessage message) throws Exception {
    	
    	ArrayList<StatEnum> lenum = new ArrayList<StatEnum>(1);
    	lenum.add(DidcommServiceStat.RECEIVED_MSG);
    	
    	try {
    		this.spool(message, 0);
    	} catch (Exception e) {
    		lenum.add(DidcommServiceStat.RECEIVED_MSG_ERROR);
    		statProducer.spool(AircastStatClass.DIDCOMM_SERVICE.toString(), didcommServiceId, lenum, Instant.now(), 1);
    		throw e;
    	}
    	
    	
    	lenum.add(DidcommServiceStat.RECEIVED_MSG_SPOOLED);
    	
	
	
    	if (message instanceof TextMessage) {
    		lenum.add(DidcommServiceStat.RECEIVED_MSG_TEXT);
    	} else if (message instanceof MenuSelectMessage) {
    		lenum.add(DidcommServiceStat.RECEIVED_MSG_MENU_SELECT_ANSWER);
    	} else if (message instanceof MediaMessage) {
    		lenum.add(DidcommServiceStat.RECEIVED_MSG_MEDIA);
    	} else if (message instanceof InvitationMessage) {
    		lenum.add(DidcommServiceStat.RECEIVED_MSG_INVITATION);
    	} else if (message instanceof ContextualMenuSelect) {
    		lenum.add(DidcommServiceStat.RECEIVED_MSG_CTX_MENU_SELECTION);
    	} else if (message instanceof ReceiptsMessage) {
    		ReceiptsMessage rm = (ReceiptsMessage) message;
    		for (MessageReceiptOptions o: rm.getReceipts()) {
    			switch (o.getState()) {
    			case RECEIVED: {
    				lenum.add(DidcommServiceStat.SENT_MSG_RECEIVED);
    				break;
    			}
    			case CREATED: {
    				lenum.add(DidcommServiceStat.SENT_MSG_CREATED);
    				break;
    			}
    			case SUBMITTED: {
    				lenum.add(DidcommServiceStat.SENT_MSG_SUBMITTED);
    				break;
    			}
    			case VIEWED: {
    				lenum.add(DidcommServiceStat.SENT_MSG_VIEWED);
    				break;
    			}
    			case DELETED: {
    				lenum.add(DidcommServiceStat.SENT_MSG_DELETED);
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