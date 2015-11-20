package be.waines.maven.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.project.MavenProject;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import be.waines.maven.api.ArtifactReference;
import be.waines.maven.api.BuildResult;


public class BuildResultUtil {
	
	public static BuildResult deserialize(File file) throws IOException {
		return new ObjectMapper().readValue(file, BuildResult.class);
	}
	
	public static void serialize(BuildResult buildResult, File file) {
		try {
			new ObjectMapper().writeValue(file, buildResult);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static boolean hasArtifactReferencesForProject(BuildResult buildResult, MavenProject project) {
		boolean projectWithArtifacts = !project.getPackaging().equals("pom");
		boolean buildResultHasArtifacts = buildResult.getArtifact() != null || !buildResult.getAttachedArtifacts().isEmpty();
		return projectWithArtifacts ? buildResultHasArtifacts : true;
	}
	
	public static boolean hasArtifactFiles(BuildResult buildResult, MavenProject project) {
		boolean projectWithArtifacts = !project.getPackaging().equals("pom");
		if (!projectWithArtifacts) {
			return true;
		}
		
		File buildDirectory = new File(project.getBuild().getDirectory());
		boolean hasAllFiles = true;
		
		hasAllFiles &= buildResult.getArtifact().getFile(buildDirectory).exists();
		for (ArtifactReference attachedArtifact : buildResult.getAttachedArtifacts()) {
			hasAllFiles &= attachedArtifact.getFile(buildDirectory).exists();
		}
		return hasAllFiles;
	}

}
