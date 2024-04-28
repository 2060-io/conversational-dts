package io.twentysixty.orchestrator.res.c;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.twentysixty.orchestrator.res.c.RegisterResourceInterface;
import jakarta.ws.rs.core.Response;

@RegisterRestClient
//@RegisterProvider(MyResponseExceptionMapper.class)
public interface RegisterResource extends RegisterResourceInterface {


}
