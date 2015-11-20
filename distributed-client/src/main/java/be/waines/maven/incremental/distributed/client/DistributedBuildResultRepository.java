package be.waines.maven.incremental.distributed.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import be.waines.maven.LogUtil;
import be.waines.maven.api.ArtifactReference;
import be.waines.maven.api.BuildResult;
import be.waines.maven.api.BuildResultRepository;
import be.waines.maven.impl.BuildResultUtil;
import be.waines.maven.model.ModuleIdFactory;

@Component(role=BuildResultRepository.class, hint="distributed")
public class DistributedBuildResultRepository implements BuildResultRepository {
	
	@Requirement
	private LogUtil logUtil;
	
	@Requirement
	private ModuleIdFactory moduleIdFactory;
	
	public DistributedBuildResultRepository() {
	}
	
	@Override
	public boolean isEnabled() {
		return Connection.CONFIGURED;
	}

	@Override
	public Collection<BuildResult> readBuilds(MavenProject project) {
		try {
			List<BuildResult> serverResults = getServerBuildResults(project);
			Collection<BuildResult> distributedBuildResults = new ArrayList<BuildResult>();
			for (BuildResult serverResult : serverResults) {
				if (BuildResultUtil.hasArtifactReferencesForProject(serverResult, project)) {
					String artifactsDownloadUrl = url(project) + "/" + serverResult.getChecksum() + "/artifacts";
					distributedBuildResults.add(new DistributedBuildResult(serverResult, logUtil, artifactsDownloadUrl));
				} else {
					logUtil.warn("Skipping BuildResult(" + serverResult.getChecksum() + ") because it doesn't have any artifacts");
				}
			}
			return distributedBuildResults;
		} catch (SerializationException e) {
			logUtil.warn("could not read buildResults from " + url(project), e);
			return Collections.emptyList();
		}
	}

	private List<BuildResult> getServerBuildResults(MavenProject project) {
		return buildResultResource(project).get(new GenericType<List<BuildResult>>(){});
	}
	
	@Override
	public boolean isValid(MavenProject project) {
		return Boolean.valueOf(Connection.CLIENT.target(url(project)).path("valid").request().get(String.class));
	}
	
	@Override
	public void markAsInvalid(MavenProject project) {
		Connection.CLIENT.target(url(project)).request().delete(Void.class);
	}

	@Override
	public void writeBuild(MavenProject project, BuildResult buildResult) {
		verifyValidBuildResult(project, buildResult);
		if (getServerBuildResults(project).contains(buildResult)) {
			logUtil.info("skipping upload, artifacts already on server");
			return;
		} else {
			logUtil.info("uploading artifacts...");
			uploadBuildResult(project, buildResult);
			
			uploadArtifact(project, buildResult.getArtifact());
			for (ArtifactReference attachedArtifact : buildResult.getAttachedArtifacts()) {
				uploadArtifact(project, attachedArtifact);
			}
		}
	}

	private Void uploadBuildResult(MavenProject project, BuildResult buildResult) {
		try {
			return buildResultResource(project).post(Entity.json(buildResult), Void.class);
		} catch (WebApplicationException e) {
			logUtil.error(e.getResponse().readEntity(String.class));
			throw e;
		}
	}

	public void verifyValidBuildResult(MavenProject project, BuildResult buildResult) {
		if (!BuildResultUtil.hasArtifactReferencesForProject(buildResult, project)) {
			throw new IllegalArgumentException("Can't save buildResult without artifacts");
		}
	}

	private void uploadArtifact(MavenProject project, ArtifactReference artifact) {
		if (artifact != null) {
			FileInputStream fileInputStream = null;
			try {
				fileInputStream = new FileInputStream(new File(project.getBuild().getDirectory(), artifact.getRelativePath()));
				artifactResource(project, artifact).put(Entity.entity(fileInputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE), Void.class);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} finally {
				IOUtils.closeQuietly(fileInputStream);
			}
		}
	}
	
	private Builder artifactResource(MavenProject project, ArtifactReference artifactReference) {
		return Connection.CLIENT.target(url(project)).path("/artifacts/")
				.queryParam("name", artifactReference.getRelativePath())
				.request(MediaType.APPLICATION_OCTET_STREAM_TYPE);
	}
	
	private Builder buildResultResource(MavenProject project) {
		return Connection.CLIENT.target(url(project))
				.request(MediaType.APPLICATION_JSON);
	}

	private String url(MavenProject project) {
		return Connection.URL + "/" + Connection.BUILD_ID + "/" + moduleIdFactory.create(project);
	}

	
	

}
