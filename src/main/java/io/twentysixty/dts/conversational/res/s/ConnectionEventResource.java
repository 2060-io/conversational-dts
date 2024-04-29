package io.twentysixty.dts.conversational.res.s;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.twentysixty.dts.conversational.jms.MoProducer;
import io.twentysixty.dts.conversational.svc.Controller;
import io.twentysixty.dts.conversational.svc.MessagingService;
import io.twentysixty.sa.client.model.event.ConnectionStateUpdated;
import io.twentysixty.sa.client.model.event.DidExchangeState;
import io.twentysixty.sa.client.res.s.ConnectionEventInterface;
import io.twentysixty.sa.client.util.JsonUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("")
public class ConnectionEventResource implements ConnectionEventInterface {


	private static Logger logger = Logger.getLogger(ConnectionEventResource.class);

	@Inject MessagingService service;
	
	@Inject Controller controller;

	@Inject MoProducer moProducer;
	
	@Override
	@POST
	@Path("/connection-state-updated")
	@Produces("application/json")
	public Response connectionStateUpdated(ConnectionStateUpdated event) {
		if (controller.isDebugEnabled()) {
			try {
				logger.info("connectionStateUpdated: " + JsonUtil.serialize(event, false));
			} catch (JsonProcessingException e) {
				logger.error("", e);
			}
		}
		
		/*
		switch (event.getState()) {
		case COMPLETED: {
			try {
				service.newConnection(event.getConnectionId());
			} catch (Exception e) {

				logger.error("", e);
				return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			break;
		}
		case TERMINATED: {
			try {
				service.archiveConnection(event.getConnectionId());
			} catch (Exception e) {

				logger.error("", e);
				return  Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			break;
		}
		}
		
		if (event.getState().equals(DidExchangeState.COMPLETED)) {
			
		}
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
