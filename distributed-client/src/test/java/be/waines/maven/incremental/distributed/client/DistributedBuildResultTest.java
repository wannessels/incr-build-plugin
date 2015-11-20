package be.waines.maven.incremental.distributed.client;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import be.waines.maven.api.BuildResult;

public class DistributedBuildResultTest {

	private String groupAndArtifactId;
	private String version;

	public DistributedBuildResultTest(String groupAndArtifactId, String version) {
		this.groupAndArtifactId = groupAndArtifactId;
		this.version = version;
	}

	public static void main(String[] args) {
		String groupAndArtifactId="be.waines.ctx.personen:be.waines.ctx.personen.overlijden.query.impl";
		String version = "OKA.R201503.0.0-SNAPSHOT";
		DistributedBuildResultTest distributedBuildResultTest = new DistributedBuildResultTest(groupAndArtifactId, version);
		
		for (BuildResult buildResult : distributedBuildResultTest.getBuildResults()) {
			System.out.println(ReflectionToStringBuilder.toString(buildResult));
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private List<BuildResult> getBuildResults() {
		return Connection.CLIENT.target(baseUrl())
		.request(MediaType.APPLICATION_JSON).get(List.class);
	}

	public String baseUrl() {
		return "http://sv-arg-bld-d2:7000/DistributedBuildResultTest/" + groupAndArtifactId + ":" + version;
	}
}
