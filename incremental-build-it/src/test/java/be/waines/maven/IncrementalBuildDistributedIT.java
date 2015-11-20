package be.waines.maven;

import static be.waines.maven.MavenLogAssert.*;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import be.waines.maven.incremental.distributed.server.RestServer;
import be.waines.maven.incremental.distributed.server.RestServerImpl;

public class IncrementalBuildDistributedIT extends AbstractIncrementalBuildIT {

	private static final String SKIPPING_UPLOAD = "[INFO] skipping upload, artifacts already on server";
	private static final String UPLOADING_ARTIFACTS = "[INFO] uploading artifacts...";
	
	private Verifier verifier1;
	private Verifier verifier2;
	private Verifier verifier3;

	private File repositoryRootDir;

	private RestServer restServer;
	
	@Before
	public void setUp() throws Exception {
		startServer();

		String[] cliOptions = new String[]{"-f pom-distributed.xml",
											"-Ddistributed.server.url=http:///localhost:8081",
											"-Ddistributed.build=maven-test"};
		verifier1 = setupProjects(cliOptions);
		verifier2 = setupProjects(cliOptions);
		verifier3 = setupProjects(cliOptions);
	}
	
	@After
	public void tearDown() throws Exception {
		stopServer();
		cleanUpProjects();
	}
	
	public void startServer() throws Exception {
		repositoryRootDir = createTempDirectory("distributed-repository");
		restServer = new RestServerImpl(8081, repositoryRootDir);
		restServer.start();
	}
	
	public void stopServer() throws Exception {
		restServer.stop();
		FileUtils.deleteDirectory(repositoryRootDir);
	}
	
	public void cleanUpProjects() throws IOException {
		FileUtils.deleteDirectory(new File(verifier1.getBasedir()));
		FileUtils.deleteDirectory(new File(verifier2.getBasedir()));
		FileUtils.deleteDirectory(new File(verifier3.getBasedir()));
	}
	
	public void build(Verifier verifier) throws IOException, VerificationException {
		verifier.executeGoals(GOALS);
		verifier.verifyErrorFreeLog();
		verifier.resetStreams();
	}
	
	@Test
	public void buildNonDistributedWhenDistributedServerUrlNotSet() throws Exception {
		build(verifier1);
		stopServer();
		verifier2.getCliOptions().remove("-Ddistributed.server.url=http:///localhost:8081");
		build(verifier2);
		
		assertProjectBuilt(verifier2, "projectB",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier2, "projectA",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier2, "projectC",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier2, "aggregator",TEST_PROJECTS_VERSION);
	}

	@Test
	public void skipAllProjectsAndDownloadArtifactsWhenOtherNodeHasPreviousBuild() throws IOException, VerificationException {
		build(verifier1);
		
		assertProjectLog(verifier1, "projectB", TEST_PROJECTS_VERSION, UPLOADING_ARTIFACTS);
		assertProjectLog(verifier1, "projectA", TEST_PROJECTS_VERSION, UPLOADING_ARTIFACTS);
		assertProjectLog(verifier1, "projectC", TEST_PROJECTS_VERSION, UPLOADING_ARTIFACTS);
		assertProjectLog(verifier1, "aggregator", TEST_PROJECTS_VERSION, UPLOADING_ARTIFACTS);
		
		build(verifier2);
		
		assertProjectSkipped(verifier2, "projectB",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier2, "projectA",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier2, "projectC",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier2, "aggregator",TEST_PROJECTS_VERSION);
		
		verifier2.assertFilePresent("projectA/target/projectA-" + TEST_PROJECTS_VERSION + ".jar");
		verifier2.assertFilePresent("projectB/target/projectB-" + TEST_PROJECTS_VERSION + ".jar");
		verifier2.assertFilePresent("projectB/target/projectB-" + TEST_PROJECTS_VERSION + "-tests.jar");
		verifier2.assertFilePresent("projectC/target/projectC-" + TEST_PROJECTS_VERSION + ".jar");
		
		assertProjectLog(verifier2, "projectB", TEST_PROJECTS_VERSION, SKIPPING_UPLOAD);
		assertProjectLog(verifier2, "projectA", TEST_PROJECTS_VERSION, SKIPPING_UPLOAD);
		assertProjectLog(verifier2, "projectC", TEST_PROJECTS_VERSION, SKIPPING_UPLOAD);
		assertProjectLog(verifier2, "aggregator", TEST_PROJECTS_VERSION, SKIPPING_UPLOAD);
	}
	
	@Test
	public void forceBuildOnAllOtherNodesProjectIfFailedOnOneNode() throws Exception {
		verifier1.addCliOption("-DprojectA.fail=true");
		try {
			build(verifier1);
			try {
				assertProjectFailed(verifier1, "projectA");
			} catch (VerificationException e) {
				e.printStackTrace();
			}
			Assert.fail("expected VerificationException\n" + FileUtils.readFileToString(new File(verifier1.getBasedir(), verifier1.getLogFileName())));
		} catch (VerificationException e) {
			//expected
		}
		build(verifier2);
		assertProjectBuilt(verifier2, "projectA",TEST_PROJECTS_VERSION);
		
		build(verifier3);
		assertProjectBuilt(verifier3, "projectA",TEST_PROJECTS_VERSION);
	}
	
	@Test
	public void resultsFromOtherBuildIdAreOnlyAvailableAfterCommit() throws Exception {
		build(verifier1);
		
		verifier2.getCliOptions().remove("-Ddistributed.build=maven-test");
		verifier2.addCliOption("-Ddistributed.build=maven-test2");
		build(verifier2);
		assertProjectBuilt(verifier2, "projectA",TEST_PROJECTS_VERSION);
		
		commit("maven-test");
		verifier3.getCliOptions().remove("-Ddistributed.build=maven-test");
		verifier3.addCliOption("-Ddistributed.build=maven-test3");
		
		build(verifier3);
		assertProjectSkipped(verifier3, "projectA",TEST_PROJECTS_VERSION);
	}

	private void commit(String buildId) {
		ClientBuilder.newClient().target("http://localhost:8081/").path(buildId).path("commit").request().put(Entity.text(""));
	}
	
}
