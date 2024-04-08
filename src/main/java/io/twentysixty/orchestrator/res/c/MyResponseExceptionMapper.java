package io.twentysixty.orchestrator.res.c;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

//@Provider
public class MyResponseExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

	private static final Logger logger = Logger.getLogger("Controller");

    @Override
    public RuntimeException toThrowable(Response response) {
        if (response.getStatus() == 500) {
            //throw new RuntimeException("The remote service responded with HTTP 500");
        }
        logger.info("here " + response.getStatus());
        return null;
    }
}