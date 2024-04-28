package io.twentysixty.dts.conversational.res.s;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.twentysixty.dts.conversational.ex.NonexistentConnectionException;
import io.twentysixty.dts.conversational.svc.BcastService;
import io.twentysixty.dts.conversational.svc.MessagingService;
import io.twentysixty.sa.client.model.message.BaseMessage;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("")
public class BroadcastResource  {
/*

	private static Logger logger = Logger.getLogger(BroadcastResource.class);

	@Inject BcastService service;
	


	@GET
	@Path("/lockAndGetConnections/{qty}")
	@Produces("application/json")
	public Response lockAndGetConnections(@PathParam(value="qty") Integer qty) {

		List<List<Object>> lockedConnections = null;
		try {
			lockedConnections = service.lockAndGetConnections(qty);
		} catch (Exception e) {
			logger.error("", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		return Response.status(Status.OK).entity(lockedConnections).build();

	}

	@POST
	@Path("/sendBaseMessage/{connectionId}")
	@Produces("application/json")
	public Response sendBaseMessage(@PathParam(value="connectionId") UUID connectionId, BaseMessage message) {

		List<List<Object>> lockedConnections = null;
		try {
			service.sendBaseMessage(connectionId, message);
		} catch (NonexistentConnectionException e) {
			logger.error("", e);
			return Response.status(Status.BAD_REQUEST).build();
		} catch (Exception e) {
			logger.error("", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		return Response.status(Status.OK).entity(lockedConnections).build();

	}
*/

}
