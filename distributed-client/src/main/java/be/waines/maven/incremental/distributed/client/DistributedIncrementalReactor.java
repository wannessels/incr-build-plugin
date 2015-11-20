package be.waines.maven.incremental.distributed.client;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.codehaus.plexus.component.annotations.Component;

import be.waines.maven.api.ChecksumConflictException;
import be.waines.maven.api.IncrementalReactor;
import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;

@Component(role=IncrementalReactor.class, hint="distributed")
public class DistributedIncrementalReactor implements IncrementalReactor{

	@Override
	public Checksum getChecksum(ModuleId moduleId) {
		try {
			return checksum(moduleId).get(Checksum.class);
		} catch (WebApplicationException e) {
			if (e.getResponse().getStatus() == Status.NOT_FOUND.getStatusCode()) {
				return null;
			} else {
				throw e;
			}
		}
	}


	@Override
	public void setChecksum(ModuleId moduleId, Checksum checksum) {
		try {
			checksum(moduleId).put(Entity.text(checksum));
		} catch (WebApplicationException e) {
			if (e.getResponse().getStatus() == Status.CONFLICT.getStatusCode()) {
				Checksum conflictingChecksum = new Checksum(String.valueOf(e.getResponse().getHeaders().getFirst("original-checksum")));
				throw new ChecksumConflictException(moduleId, checksum, conflictingChecksum);
			} else {
				throw e;
			}
		}
	}

	@Override
	public boolean contains(ModuleId moduleId) {
		return getChecksum(moduleId) != null;
	}
	
	private Builder checksum(ModuleId moduleId) {
		URI uri = getURI(moduleId).resolve("sourcesChecksum");
		return Connection.CLIENT.target(uri)
				.request(MediaType.TEXT_PLAIN_TYPE);
	}
	
	private URI getURI(ModuleId moduleId) {
		try {
			return new URI(Connection.URL +  "/" + Connection.BUILD_ID + "/" + moduleId.toString() + "/");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean isEnabled() {
		return Connection.CONFIGURED;
	}

}
