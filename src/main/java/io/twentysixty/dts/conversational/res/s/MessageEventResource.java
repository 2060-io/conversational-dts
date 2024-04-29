package io.twentysixty.dts.conversational.res.s;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.twentysixty.dts.conversational.jms.MoProducer;
import io.twentysixty.dts.conversational.jms.MtProducer;
import io.twentysixty.dts.conversational.jms.StatProducer;
import io.twentysixty.dts.conversational.svc.Controller;
import io.twentysixty.sa.client.model.event.MessageReceived;
import io.twentysixty.sa.client.model.event.MessageStateUpdated;
import io.twentysixty.sa.client.model.message.MessageReceiptOptions;
import io.twentysixty.sa.client.model.message.ReceiptsMessage;
import io.twentysixty.sa.client.res.s.MessageEventInterface;
import io.twentysixty.sa.client.util.JsonUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;




@Path("")
public class MessageEventResource implements MessageEventInterface {

	private static Logger logger = Logger.getLogger(MessageEventResource.class);



	@Inject MoProducer moProducer;
	//@Inject MtProducer mtProducer;

	@Inject Controller controller;

	
	@Inject StatProducer statProducer;

	@Override
	@POST
	@Path("/message-received")
	@Produces("application/json")
	public Response messageReceived(MessageReceived event) {

		if (controller.isDebugEnabled()) {
			try {
				logger.info("messageReceived: " + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}

		try {
			
			moProducer.sendMessage(event);
		} catch (Exception e) {
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		
		
		
		return  Response.status(Status.OK).build();



	}

	@Override
	@POST
	@Path("/message-state-updated")
	@Produces("application/json")
	public Response messageStateUpdated(MessageStateUpdated event) {

		if (controller.isDebugEnabled()) {
			try {
				logger.info("messageStateUpdated: " + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}
		
		/*
		List<MessageReceiptOptions> receipts = new ArrayList<>();

		
		MessageReceiptOptions stateMsg = new MessageReceiptOptions();
		stateMsg.setMessageId(event.getMessageId());
		stateMsg.setTimestamp(Instant.now());
		stateMsg.setState(event.getState());
		receipts.add(stateMsg);

		ReceiptsMessage r = new ReceiptsMessage();
		r.setConnectionId(event.getConnectionId());
		r.setReceipts(receipts);
		 */
		try {
			moProducer.sendMessage(event);
		} catch (Exception e) {
			logger.error("", e);
			return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		return  Response.status(Status.OK).build();
	}



}
