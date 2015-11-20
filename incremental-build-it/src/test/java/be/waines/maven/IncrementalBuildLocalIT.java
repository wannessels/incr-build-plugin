package be.waines.maven;

import static be.waines.maven.MavenLogAssert.assertProjectBuilt;
import static be.waines.maven.MavenLogAssert.assertProjectFailed;
import static be.waines.maven.MavenLogAssert.assertProjectSkipped;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IncrementalBuildLocalIT extends AbstractIncrementalBuildIT {

	private Verifier verifier;
	
	@Before
	public void buildOnce() throws IOException, VerificationException {
		verifier = setupProjects("-f pom-local.xml");
		verifier.executeGoals(GOALS);
		verifier.verifyErrorFreeLog();
		verifier.resetStreams();
	}
	
	@After
	public void cleanup() throws IOException {
        FileUtils.deleteDirectory( new File(verifier.getBasedir()) );
	}
	
	@Test
	public void skipAllProjectsWhenNoChanges() throws IOException, VerificationException {
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
	public void buildOnlyChangedProjects() throws IOException, VerificationException {
		verifier.writeFile("projectC/src/dummy.txt", "dummy");
		
		verifier.executeGoals(GOALS);
		verifier.verifyErrorFreeLog();
		
		assertProjectSkipped(verifier, "projectB",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier, "projectA",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier, "projectC",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier, "aggregator",TEST_PROJECTS_VERSION);
	}
	
	@Test
	public void buildWhenParentChanges() throws IOException, VerificationException {
		File parentPomFile = new File(verifier.getBasedir(),"parent/pom.xml");
		FileUtils.write(parentPomFile, "<!-- dummy change -->", true);
		
		verifier.executeGoals(GOALS);
		verifier.verifyErrorFreeLog();
		
		assertProjectBuilt(verifier, "parent",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier, "projectB",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier, "projectA",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier, "projectC",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier, "aggregator",TEST_PROJECTS_VERSION);
	}
	
	@Test
	public void buildDownstreamProjectsOfChangedProjects() throws Exception {
		verifier.writeFile("projectB/src/dummy.txt", "dummy");
		
		verifier.executeGoals(GOALS);
		verifier.verifyErrorFreeLog();
		
		assertProjectBuilt(verifier, "projectB",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier, "projectA",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier, "projectC",TEST_PROJECTS_VERSION);
		assertProjectSkipped(verifier, "aggregator",TEST_PROJECTS_VERSION);
	}
	
	@Test
	public void buildDownstreamProjectOfChangedProjectEvenIfNotChangedInCurrentBuild() throws Exception {
		verifier.writeFile("projectB/src/dummy.txt", "dummy");
		verifier.setSystemProperty("projectA.fail", "true");
		
		try {
			verifier.executeGoals(GOALS);
		} catch (VerificationException e) {
			//supposed to fail
		}
		
		assertProjectBuilt(verifier, "projectB",TEST_PROJECTS_VERSION);
		assertProjectBuilt(verifier, "projectA",TEST_PROJECTS_VERSION);
		assertProjectFailed(verifier, "projectA");
		verifier.assertFileNotPresent("projectA/target/projectA-" + TEST_PROJECTS_VERSION + ".jar");
	}

}
