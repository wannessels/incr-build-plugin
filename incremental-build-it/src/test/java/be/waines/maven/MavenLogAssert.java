package be.waines.maven;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.shared.utils.StringUtils;

import com.google.common.collect.Lists;

public class MavenLogAssert {
	
	private static final String LOG_SEPARATOR_LINE = "[INFO] ------------------------------------------------------------------------";

	public static void assertProjectFailed(Verifier verifier, String projectName) throws VerificationException {
		String projectFailedLine = "[INFO] " + projectName + " " + StringUtils.repeat(".", 51-projectName.length()) + " FAILURE";
		verifier.verifyTextInLog(projectFailedLine);
	}
	
	public static void assertProjectBuilt(Verifier verifier, String name,	String version)  throws VerificationException {
		assertProjectLog(verifier, name, version, "[INFO] building...");
	}

	public static void assertProjectSkipped(Verifier verifier, String name, String version) throws VerificationException {
		assertProjectLog(verifier,name,version, "[INFO] skipping build");
	}
	
	public static void assertProjectLog(Verifier verifier, String name, String version, String... linesToAssert) throws VerificationException {
		File logFile = new File(verifier.getBasedir(), verifier.getLogFileName());
		List<String> logLines = verifier.loadFile(logFile, false);
		
		List<String> projectStart = Lists.newArrayList(LOG_SEPARATOR_LINE, "[INFO] Building " + name + " " + version, LOG_SEPARATOR_LINE);
		int indexProjectStart = Collections.indexOfSubList(logLines, projectStart);
		int indexLastProjectEnd = logLines.lastIndexOf(LOG_SEPARATOR_LINE);
		
		List<String> projectLogLines = logLines.subList(indexProjectStart + projectStart.size(), indexLastProjectEnd);
		projectLogLines = projectLogLines.subList(0, projectLogLines.indexOf(LOG_SEPARATOR_LINE));

		int index = Collections.indexOfSubList(projectLogLines, Lists.newArrayList(linesToAssert));
		if (index == -1) {
			throw new VerificationException("expected \n" + toString(linesToAssert) + " \nin log file (" + logFile+ "):\n" + toString(projectLogLines));
		}
	}
	
	public static String toString(String... lines) {
		return StringUtils.join(lines, "\n");
	}
	
	public static String toString(Collection<String> lines) {
		return StringUtils.join(lines.iterator(),"\n");
	}
}
