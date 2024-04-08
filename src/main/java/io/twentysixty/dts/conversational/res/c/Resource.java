package io.twentysixty.dts.conversational.res.c;

import java.io.InputStream;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;

public class Resource {

	@FormParam("chunk")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream chunk;
}