package be.waines.maven.incremental.distributed.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;

import be.waines.maven.LogUtil;
import be.waines.maven.api.ArtifactReference;
import be.waines.maven.api.BuildResult;

public class DistributedBuildResult extends BuildResult {
	
	private transient LogUtil logUtil;

	private static final long serialVersionUID = 2L;
	public DistributedBuildResult(BuildResult buildResult, LogUtil logUtil, String source) {
		super(buildResult.getModuleId(),
				buildResult.getSourcesChecksum(),
				buildResult.getDependencyChecksums(), 
				buildResult.getArtifact(), 
				buildResult.getAttachedArtifacts(),
				buildResult.getExtensions(),
				source);
		this.logUtil = logUtil;
	}
	
	@Override
	public void downloadArtifacts(File buildDirectory) throws IOException {
		logUtil.info("downloading artifacts from " + artifactsUrl());
		
		downloadArtifact(buildDirectory, getArtifact());
		for (ArtifactReference attachedArtifact : getAttachedArtifacts()) {
			downloadArtifact(buildDirectory, attachedArtifact);
		}
	}

	private void downloadArtifact(File buildDirectory, ArtifactReference artifactReference) throws IOException  {
		if (artifactReference != null) {
			String artifactRelativePath = artifactReference.getRelativePath();
			File artifactFile = new File(buildDirectory, artifactRelativePath);
			File tmpFile = new File(buildDirectory, artifactRelativePath + ".download");
			
			downloadToTempFile(artifactReference, tmpFile);
			deletePreviousFile(artifactFile);
			renameTempToArtifactFile(tmpFile, artifactFile);
			logUtil.info("downloaded " + artifactRelativePath);
		}
	}

	private void downloadToTempFile(ArtifactReference artifactReference, File tmpFile) throws FileNotFoundException, IOException {
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			inputStream = artifactResource(artifactReference).get(InputStream.class);
			outputStream = new FileOutputStream(tmpFile);
			IOUtils.copy(inputStream, outputStream );
		} finally {
			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(inputStream);
		}
	}

	private void renameTempToArtifactFile(File tmpFile, File artifactFile)
			throws IOException {
		boolean renamed = tmpFile.renameTo(artifactFile);
		if (!renamed) {
			throw new IOException("could not rename " + tmpFile + " to " + artifactFile);
		}
	}

	private void deletePreviousFile(File artifactFile) throws IOException {
		if (artifactFile.exists()) {
			boolean deleted = artifactFile.delete();
			if (!deleted) {
				throw new IOException("could not delete " + artifactFile);
			}
		}
	}
	
	private Builder artifactResource(ArtifactReference artifactReference) {
		return Connection.CLIENT.target(artifactsUrl())
				.queryParam("name", artifactReference.getRelativePath())
				.request(MediaType.WILDCARD_TYPE);
	}

	private String artifactsUrl() {
		return Connection.URL + "/" + Connection.BUILD_ID + "/" + getModuleId() + "/" + getChecksum().toString() + "/artifacts";
	}
	
}
