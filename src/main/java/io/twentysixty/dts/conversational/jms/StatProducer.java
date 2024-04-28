package io.twentysixty.dts.conversational.jms;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import io.twentysixty.dts.conversational.svc.Controller;
import io.twentysixty.sa.client.jms.AbstractProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;


@ApplicationScoped
public class StatProducer extends AbstractProducer<StatEvent> {

	@Inject
    ConnectionFactory connectionFactory;

	
	@ConfigProperty(name = "io.twentysixty.dts.conversational.jms.ex.delay")
	Long _exDelay;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.jms.stats.queue.name")
	String _queueName;

	@ConfigProperty(name = "io.twentysixty.dts.conversational.jms.stats.producer.threads")
	Integer _threads;

	@Inject Controller controller;

	@Inject
    ConnectionFactory _connectionFactory;

	
	private static Object contextLockObj = new Object();
	private static final Logger logger = Logger.getLogger(StatProducer.class);
	
	int id = 0;
    
    
   
    
    void onStart(@Observes StartupEvent ev) {
    	logger.info("onStart: queue name " + _queueName);
    	 	//logger.info("onStart: BeProducer");

        	this.setExDelay(_exDelay);
    		this.setDebug(controller.isDebugEnabled());
    		this.setQueueName(_queueName);
    		this.setThreads(_threads);
    		this.setConnectionFactory(_connectionFactory);

        	this.setProducerCount(_threads);

        
    }

    void onStop(@Observes ShutdownEvent ev) {
    	
    }
 
    public void spool(String statClass, 
    		UUID entityId, 
    		List<StatEnum> enums, 
    		Instant ts, 
    		Integer increment) throws Exception {
    	
    	
    	List<StatEnum> statEnums = new ArrayList<StatEnum>(enums.size());
    	for (StatEnum se: enums) {
    		statEnums.add(CommonStatEnum.build(se.getIndex(), se.getValue()));
    	}
    	
    	StatEvent event = new StatEvent();
    	event.setEntityId(entityId.toString());
    	event.setEnums(statEnums);
    	event.setIncrement(increment);
    	event.setTs(ts);
    	event.setStatClass(statClass);
    	this.spool(event, 0);
    }
    
    public void spool(
    		String statClass, 
    		UUID entityId, 
    		StatEnum statEnum, 
    		Instant ts, 
    		int increment) throws Exception {
    	
    	StatEvent event = new StatEvent();
    	event.setEntityId(entityId.toString());
    	List<StatEnum> statEnums = new ArrayList<StatEnum>(1);
    	statEnums.add(CommonStatEnum.build(statEnum.getIndex(), statEnum.getValue()));
    	event.setEnums(statEnums);
    	event.setIncrement(increment);
    	event.setTs(ts);
    	event.setStatClass(statClass);
    	this.spool(event, 0);
    }
    
    
   
}