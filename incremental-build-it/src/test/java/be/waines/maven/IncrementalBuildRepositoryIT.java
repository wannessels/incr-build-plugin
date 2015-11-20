package be.waines.maven;

import static be.waines.maven.MavenLogAssert.assertProjectBuilt;
import static be.waines.maven.MavenLogAssert.assertProjectSkipped;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import be.waines.maven.repository.FileServer;

public class IncrementalBuildRepositoryIT extends AbstractIncrementalBuildIT {

	private Verifier verifier;
	private FileServer repositoryServer;
	private File repositoryDir;
	
	public void setupRepository() throws IOException {
		repositoryDir = createTempDirectory("maven-repository");
		repositoryServer = new FileServer(8082, repositoryDir);
		repositoryServer.start();
	}
	
	@Before
	public void setUp() throws IOException, VerificationException {
		verifier = setupProjects("-f pom-local.xml","-Pwith-external-dependency");
		setupRepository();
		buildAndDeployExternalDependency();
	}

	@After
	public void cleanup() throws IOException {
        FileUtils.deleteDirectory( new File(verifier.getBasedir()) );
        repositoryServer.stop();        
        FileUtils.deleteDirectory(repositoryDir);
	}
	
	@Test
	public void skipWhenExternalSnapshotDependencyNotChanged() throws IOException, VerificationException {
		verifier.executeGoals(GOALS);
		verifier.verifyErrorFreeLog();
		verifier.resetStreams();
		
		verifier.executeGoals(GOALS);
		verifier.verifyErrorFreeLog();
		
		assertProjectSkipped(verifier, "projectB",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier, "projectA",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier, "projectC",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier, "aggregator",TEST_PROJECTS_VERSION);

		verifier.assertFilePresent("projectA/target/projectA-" + TEST_PROJECTS_VERSION + ".jar");
		verifier.assertFilePresent("projectB/target/projectB-" + TEST_PROJECTS_VERSION + ".jar");
		verifier.assertFilePresent("projectB/target/projectB-" + TEST_PROJECTS_VERSION + "-tests.jar");
		verifier.assertFilePresent("projectC/target/projectC-" + TEST_PROJECTS_VERSION + ".jar");
	}
	
	@Test
	public void buildWhenExternalSnapshotDependencyChanged() throws IOException, VerificationException {
		verifier.executeGoals(GOALS);
		verifier.verifyErrorFreeLog();
		verifier.resetStreams();
		
		buildAndDeployExternalDependency();
		verifier.executeGoals(GOALS);
		verifier.verifyErrorFreeLog();
		
		assertProjectBuilt(verifier, "projectA",TEST_PROJECTS_VERSION);
	}
	
	private void buildAndDeployExternalDependency() throws IOException, VerificationException {
		Verifier verifier = setupProjects("-f external\\pom.xml");
		verifier.writeFile("external/src/main/resources/timestamp.txt", String.valueOf(System.currentTimeMillis()));
		verifier.executeGoals(Arrays.asList("-Dmaven.install.skip=true","clean","deploy"));
		verifier.verifyErrorFreeLog();
	}

	
}
