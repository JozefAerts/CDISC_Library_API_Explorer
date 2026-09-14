package com.xml4pharma.cdisclibraryapiviewer;

import javax.ws.rs.client.*;  // Client and ClientBuilder - javax.ws.rs-api-2.1.jar
import javax.ws.rs.core.*;

// Jersey-2 imports
import org.glassfish.jersey.client.*;

public class BasicCDISCLibraryClient {
	
	private String base = "https://library.cdisc.org/api";  // the base of the service
	private String apiKey;
	private Client client;
	
	private int responseStatus;
	
	public BasicCDISCLibraryClient(String apiKey) {
		this.apiKey = apiKey;
		ClientConfig clientConfig = new ClientConfig();
		// see e.g. https://jersey.github.io/documentation/latest/logging_chapter.html for how to add logging
		//clientConfig.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);
		client = ClientBuilder.newClient(clientConfig);
	}
	
	
	/** Returns the scenario fields as XML - uses HATEAOS */
	public String getCDISCLibraryXML(String query) {
		WebTarget webTarget = client.target(base).path(query);
		System.out.println("Running query = " + query);
		Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_XML);
		// Response requires javax.annotation-api-1.2.jar, hk2-api-2.5.0-b42.jar, hk2-locator-2.5.0-b42.jar, hk2-utils-2.5.0-b42.jar
		// and javax.inject-1.jar, javax.inject-2.5.0-b42.jar
		// and jersey-media-json-binding.jar
		// from the Jersey-2 library - see https://jersey.github.io/
		//Response response = invocationBuilder.get();
		Response response = invocationBuilder.header("api-key", apiKey).get();
		responseStatus = response.getStatus();
		System.out.println("HTTP Response Status = " + responseStatus);
		String xml = response.readEntity(String.class);
		return xml;
	}
	
	/** Returns the scenario fields as XML - uses HATEAOS */
	public String getCDISCLibraryJSON(String query) {
		WebTarget webTarget = client.target(base).path(query);
		System.out.println("Running query = " + query);
		Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON);
		// Response requires javax.annotation-api-1.2.jar, hk2-api-2.5.0-b42.jar, hk2-locator-2.5.0-b42.jar, hk2-utils-2.5.0-b42.jar
		// and javax.inject-1.jar, javax.inject-2.5.0-b42.jar
		// and jersey-media-json-binding.jar
		// from the Jersey-2 library - see https://jersey.github.io/
		//Response response = invocationBuilder.get();
		Response response = invocationBuilder.header("api-key", apiKey).get();
		responseStatus = response.getStatus();
		System.out.println("HTTP Response Status = " + responseStatus);
		String xml = response.readEntity(String.class);
		return xml;
	}


	public int getResponseStatus() {
		return responseStatus;
	}

}
