package be.waines.maven.incremental.distributed.server.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.process.internal.RequestScoped;

import be.waines.maven.api.BuildResult;
import be.waines.maven.api.ChecksumConflictException;
import be.waines.maven.incremental.distributed.server.impl.BuildInProgressRepository;
import be.waines.maven.incremental.distributed.server.impl.InProgressRepositories;
import be.waines.maven.incremental.distributed.server.impl.RepositoryCommit;
import be.waines.maven.incremental.distributed.server.impl.SharedBuildRepository;
import be.waines.maven.model.BuildId;
import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;


@Path("/")
@RequestScoped
public class BuildRestService {
	
	private InProgressRepositories inProgressRepositories;
	private SharedBuildRepository sharedBuildRepository;

	@Inject
	public BuildRestService(InProgressRepositories inProgressRepositories, SharedBuildRepository sharedBuildRepository) {
		this.inProgressRepositories = inProgressRepositories;
		this.sharedBuildRepository = sharedBuildRepository;
	}
	
	@GET
	@Path("bla")
	public String burp() {
		return "blabla";
	}
	
	@PUT
	@Path("{build_id}/{moduleId}/sourcesChecksum")
	public void setChecksum(@PathParam("build_id") BuildId buildId, @PathParam("moduleId") ModuleId moduleId, Checksum checksum) {
		BuildInProgressRepository inProgressRepository = inProgressRepositories.getInProgressRepository(buildId);
		try {
			inProgressRepository.setChecksum(moduleId, checksum);
		} catch (ChecksumConflictException e) {
			throw new WebApplicationException(e,Response.status(Status.CONFLICT).header("original-checksum", e.getChecksum()).build());
		}
	}
	
	@GET
	@Path("{build_id}/{moduleId}/sourcesChecksum")
	public Checksum getChecksum(@PathParam("build_id") BuildId buildId, @PathParam("moduleId") ModuleId moduleId) {
		BuildInProgressRepository inProgressRepository = inProgressRepositories.getInProgressRepository(buildId);
		Checksum checksum = inProgressRepository.getChecksum(moduleId);
		if (checksum == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("no checksum found").build());			
		} else {
			return checksum;
		}
	}
	
	@POST
	@Path("{build_id}/{moduleId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void saveBuildResult(@PathParam("build_id") BuildId buildId, @PathParam("moduleId") ModuleId moduleId, BuildResult buildResult) {
		BuildInProgressRepository inProgressRepository = inProgressRepositories.getInProgressRepository(buildId);
		inProgressRepository.saveBuildResult(moduleId, buildResult);
	}
	
	@DELETE
	@Path("{build_id}/{moduleId}")
	public void markBuildResultInvalid(@PathParam("build_id") BuildId buildId, @PathParam("moduleId") ModuleId moduleId) {
		BuildInProgressRepository inProgressRepository = inProgressRepositories.getInProgressRepository(buildId);
		inProgressRepository.markAsInvalid(moduleId);
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("{build_id}/{moduleId}/valid")
	public String isBuildResultValid(@PathParam("build_id") BuildId buildId, @PathParam("moduleId") ModuleId moduleId) {
		BuildInProgressRepository inProgressRepository = inProgressRepositories.getInProgressRepository(buildId);
		return Boolean.valueOf(inProgressRepository.isValid(moduleId)).toString();
	}
	
	@GET
	@Path("{build_id}/{moduleId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBuildResult(@PathParam("build_id") BuildId buildId, @PathParam("moduleId") ModuleId moduleId) {
		List<BuildResult> results = new ArrayList<BuildResult>();
		results.addAll(sharedBuildRepository.getBuildResults(moduleId));
		
		BuildInProgressRepository inProgressRepository = inProgressRepositories.getInProgressRepository(buildId);
		BuildResult inProgressBuildResult = inProgressRepository.getBuildResult(moduleId);
		if (inProgressBuildResult != null) {
			results.add(inProgressBuildResult);
		}
		
		return Response.ok(new GenericEntity<List<BuildResult>>(results){}).build();
	}
	

	@PUT
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("{build_id}/{moduleId}/artifacts")
	public void saveArtifact(@PathParam("build_id") BuildId buildId, @PathParam("moduleId") ModuleId moduleId,@QueryParam("name") String name, InputStream inputStream) {
		BuildInProgressRepository inProgressRepository = inProgressRepositories.getInProgressRepository(buildId);
		try {
			inProgressRepository.saveArtifact(moduleId, name, inputStream);
		} catch (IOException e) {
			throw new WebApplicationException(e);
		}
	}

	
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("{build_id}/{moduleId}/{checksum}/artifacts")
	public Response getArtifact(@PathParam("build_id") BuildId buildId, @PathParam("moduleId") ModuleId moduleId, @PathParam("checksum") Checksum buildResultChecksum, @QueryParam("name") String name) {
		BuildInProgressRepository inProgressRepository = inProgressRepositories.getInProgressRepository(buildId);
		
		InputStream inputStream;
		try {
			inputStream = sharedBuildRepository.getArtifact(moduleId, buildResultChecksum, name);
		} catch (FileNotFoundException sharedNotFound) {
			try {
				inputStream = inProgressRepository.getArtifact(moduleId, name);
			} catch (FileNotFoundException inProgressNotFound) {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		return Response.ok(inputStream).build();
	}
	
	@PUT
	@Path("{build_id}/commit")
	public void commit(@PathParam("build_id") BuildId buildId) throws IOException {
		BuildInProgressRepository inProgressRepository = inProgressRepositories.getInProgressRepository(buildId);
		new RepositoryCommit(inProgressRepository, sharedBuildRepository).execute();
		inProgressRepositories.removeInProgressRepository(buildId);
	}
}
