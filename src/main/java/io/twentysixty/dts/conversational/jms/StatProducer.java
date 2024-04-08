package io.twentysixty.dts.conversational.jms;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.graalvm.collections.Pair;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mobiera.commons.util.JsonUtil;
import com.mobiera.ms.commons.stats.api.CommonStatEnum;
import com.mobiera.ms.commons.stats.api.StatEnum;
import com.mobiera.ms.commons.stats.api.StatEvent;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;


@ApplicationScoped
public class StatProducer extends AbstractProducer {

	@Inject
    ConnectionFactory connectionFactory;

	
	@ConfigProperty(name = "io.twentysixty.hologram.welcome.jms.ex.delay")
	Long _exDelay;

	@ConfigProperty(name = "io.twentysixty.hologram.welcome.jms.stats.queue.name")
	String _queueName;

	@ConfigProperty(name = "io.twentysixty.hologram.welcome.jms.stats.producer.threads")
	Integer _threads;

	@ConfigProperty(name = "io.twentysixty.hologram.welcome.debug")
	Boolean _debug;

	
	
	private static Object contextLockObj = new Object();
	private static final Logger logger = Logger.getLogger(StatProducer.class);
	
	int id = 0;
    
    
    public boolean isDebugEnabled() {
		return _debug;
	}
    
    
    void onStart(@Observes StartupEvent ev) {
    	logger.info("onStart: queue name " + _queueName);
    	this.setProducerCount(_threads);
    }

    void onStop(@Observes ShutdownEvent ev) {
    	
    }
 
    public void spool(String statClass, 
    		Long smppAccountId, 
    		List<StatEnum> enums, 
    		Instant ts, 
    		Integer increment) throws JMSException {
    	
    	
    	List<StatEnum> statEnums = new ArrayList<StatEnum>(enums.size());
    	for (StatEnum se: enums) {
    		statEnums.add(CommonStatEnum.build(se.getIndex(), se.getValue()));
    	}
    	
    	StatEvent event = new StatEvent();
    	event.setEntityId(smppAccountId.toString());
    	event.setEnums(statEnums);
    	event.setIncrement(increment);
    	event.setTs(ts);
    	event.setStatClass(statClass);
    	this.spool(event);
    }
    
    public void spool(
    		String statClass, 
    		Long smppAccountId, 
    		StatEnum statEnum, 
    		Instant ts, 
    		int increment) throws JMSException {
    	
    	StatEvent event = new StatEvent();
    	event.setEntityId(smppAccountId.toString());
    	List<StatEnum> statEnums = new ArrayList<StatEnum>(1);
    	statEnums.add(CommonStatEnum.build(statEnum.getIndex(), statEnum.getValue()));
    	event.setEnums(statEnums);
    	event.setIncrement(increment);
    	event.setTs(ts);
    	event.setStatClass(statClass);
    	this.spool(event);
    }
    
    
    public void spool(StatEvent event) throws JMSException {
    	this.spool(event, 0);
    }
    public void spool(StatEvent event, int attempt) throws JMSException {
    	
    	JMSProducer producer = null;
    	JMSContext context = null;
    	boolean retry = false;
    	
    	try {
    		
    		Pair<Integer, Pair<JMSContext, JMSProducer>> jms = getProducer(connectionFactory, isDebugEnabled());
    		
    		producer = jms.getRight().getRight();
    		context = jms.getRight().getLeft();
    		id = jms.getLeft();
    		
        	
        	ObjectMessage message = context.createObjectMessage(event);
        	
        	
        	
        	synchronized (producer) {
        		Queue queue = this.getQueue(context);
        		producer.send(queue, message);
            	message.acknowledge();
        	}
        	
        	if (isDebugEnabled()) {
        		try {
    				logger.info("spool: stat event spooled to " + _queueName + ":" + JsonUtil.serialize(event, false));
    			} catch (JsonProcessingException e) {
    				logger.error("", e);
    			}
        	}
        	
        	
    	} catch (Exception e) {

    		this.purgeAllProducers();
   			logger.error("error", e);
 			attempt++;
 			if (attempt<_threads) {
 				logger.info("spool: will retry attempt #" + attempt);
 				retry = true;
 			} else {
 				throw (e);
 			}
    	}
    	
    	if (retry) this.spool(event, attempt);
    }

    private Queue queue = null;
	private Queue getQueue(JMSContext context) {
		if (queue == null) {
			
			queue = context.createQueue(_queueName);
		}
		return queue;
	}
}