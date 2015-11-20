package be.waines.maven.incremental.distributed.server.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import org.apache.commons.io.IOUtils;

import be.waines.maven.api.BuildResult;
import be.waines.maven.api.ChecksumConflictException;
import be.waines.maven.model.BuildId;
import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;

public class BuildInProgressRepository {
	
	private ConcurrentMap<ModuleId, Checksum> moduleChecksums = new ConcurrentHashMap<ModuleId, Checksum>();

	private Map<ModuleId, BuildResult> buildResults = new ConcurrentHashMap<ModuleId,BuildResult>();
	private Set<ModuleId> invalidBuildResults = Collections.synchronizedSet(new HashSet<ModuleId>());
	
	private LockManager<File> fileLockManager = new LockManager<File>();
	private File repoDir;

	private boolean halted;

	private BuildId buildId;
	
	public BuildInProgressRepository(File repoDir, BuildId buildId) {
		this.repoDir = repoDir;
		this.buildId = buildId;
	}
	
	public Checksum getChecksum(ModuleId moduleId) {
		return moduleChecksums.get(moduleId);
	}
	
	public void setChecksum(ModuleId moduleId, Checksum checksum) throws ChecksumConflictException {
		assertNotHalted();
		Checksum previousChecksum = moduleChecksums.putIfAbsent(moduleId, checksum);
		if (previousChecksum != null && !previousChecksum.equals(checksum)) {
			throw new ChecksumConflictException(moduleId, previousChecksum, checksum);
		}
	}
	
	void halt() {
		halted = true;
	}
	
	private void assertNotHalted() throws IllegalStateException {
		if (halted) {
			throw new IllegalStateException();
		}
	}
	Map<ModuleId, BuildResult> getBuildResults() {
		return buildResults;
	}
	File moduleDir(ModuleId moduleId) {
		File moduleDir = new File(repoDir, moduleId.toString().replace(':', '/'));
		moduleDir.mkdirs();
		return moduleDir;
	}
	File repoDir() {
		return repoDir;
	}
	BuildId getBuildId() {
		return buildId;
	}
	
	public BuildResult getBuildResult(ModuleId moduleId) {
		if (invalidBuildResults.contains(moduleId)) {
			return null;
		} else {
			return getBuildResultInternal(moduleId);
		}
	}
	
	BuildResult getBuildResultInternal(ModuleId moduleId) {
		return buildResults.get(moduleId);
	}
	
	public void saveBuildResult(ModuleId moduleId, BuildResult buildResult) {
		assertNotHalted();
		buildResults.put(moduleId, buildResult);
	}
	
	public boolean isValid(ModuleId moduleId) {
		return !invalidBuildResults.contains(moduleId);
	}
	public void markAsInvalid(ModuleId moduleId) {
		assertNotHalted();
		invalidBuildResults.add(moduleId);
	}
	
	public void saveArtifact(ModuleId moduleId,String name, InputStream inputStream) throws IOException {
		assertNotHalted();
		File artifactFile = artifactFile(moduleId, name);
		if (artifactFile.exists()) {
			return;
		}
		Lock writeLock = fileLockManager.getLock(artifactFile).writeLock();
		FileOutputStream fileOutputStream = null;
		try {
			writeLock.lockInterruptibly();
			if (artifactFile.exists()) {
				return;
			}
			fileOutputStream = new FileOutputStream(artifactFile);
			IOUtils.copy(inputStream, fileOutputStream);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(fileOutputStream);
			writeLock.unlock();
		}
	}

	private File artifactFile(ModuleId moduleId, String name) {
		File moduleDir = moduleDir(moduleId);
		File artifactFile = new File(moduleDir, name);
		return artifactFile;
	}

	
	
	public FileInputStream getArtifact(ModuleId moduleId,String name) throws FileNotFoundException {
		File artifactFile = artifactFile(moduleId, name);
		Lock readLock = fileLockManager.getLock(artifactFile).readLock();
		try {
			readLock.lockInterruptibly(); 
			return new FileInputStream(artifactFile);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);	
		} finally {
			//TODO unlock after inputstream read
			readLock.unlock();
		}
	}

}
