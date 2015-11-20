package be.waines.maven.incremental.distributed.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import be.waines.maven.incremental.distributed.BuildIdBodyReaderAndWriter;
import be.waines.maven.incremental.distributed.ChecksumBodyReaderAndWriter;
import be.waines.maven.model.BuildId;


class Connection {
	
	private static final int TIMEOUT = 5 * 60 * 1000;
	private static final String URL_PROPERTY = "distributed.server.url";
	private static final String BUILD_ID_PROPERTY = "distributed.build";
	
	public static final Client CLIENT = getClient();
	public static final String URL = getServerUrl();
	public static final boolean CONFIGURED = isUrlPropertySpecified();
	public static final BuildId BUILD_ID = getBuildId();
	
	
	private static Client getClient() {
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(BuildIdBodyReaderAndWriter.class);
		clientConfig.register(ChecksumBodyReaderAndWriter.class);
		clientConfig.property(ClientProperties.CONNECT_TIMEOUT, TIMEOUT);
		clientConfig.property(ClientProperties.READ_TIMEOUT, TIMEOUT);
		clientConfig.property("com.sun.jersey.api.json.POJOMappingFeature", true);
		Client client = ClientBuilder.newClient(clientConfig);
		client.register(JacksonJsonProvider.class);
		return client;
	}


	private static BuildId getBuildId() {
		String buildId = System.getProperty(BUILD_ID_PROPERTY);
		if (buildId == null) {
			try {
				buildId=InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		}
		return new BuildId(buildId);
	}


	private static boolean isUrlPropertySpecified() {
		return getServerUrl() != null;
	}


	private static String getServerUrl() {
		String serverURL = System.getProperty(URL_PROPERTY);
		if (serverURL == null) {
			serverURL = System.getenv(URL_PROPERTY);
		}
		return serverURL;
	}
	
}
