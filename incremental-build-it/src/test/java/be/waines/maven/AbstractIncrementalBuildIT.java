package be.waines.maven;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public abstract class AbstractIncrementalBuildIT {

	protected static final String TEST_PROJECTS_DIR = "/test-projects";
	protected static final List<String> GOALS = Arrays.asList("clean","verify");
	protected static final String TEST_PROJECTS_VERSION = "1.0.0-SNAPSHOT";

	public Verifier setupProjects(String... cliOptions) throws IOException, VerificationException {
		File testDir = createTempDirectory("incremental-test-projects");
	    ResourceExtractor.extractResourceToDestination(getClass(), TEST_PROJECTS_DIR, testDir, true );
	    
		Verifier verifier = new Verifier(testDir.getAbsolutePath());
		verifier.addCliOption("-DenableIncrementalBuild");
		for (String cliOption : cliOptions) {
			verifier.addCliOption(cliOption);
		}
		return verifier;
	}
	
	File createTempDirectory(String name) throws IOException {
		File temp = File.createTempFile(name, Long.toString(System.nanoTime()));

		if (!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return (temp);
	}

}