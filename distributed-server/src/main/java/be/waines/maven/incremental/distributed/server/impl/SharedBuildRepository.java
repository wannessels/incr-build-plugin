package be.waines.maven.incremental.distributed.server.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;

import be.waines.maven.api.BuildResult;
import be.waines.maven.impl.BuildResultUtil;
import be.waines.maven.model.BuildId;
import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;

@Singleton
public class SharedBuildRepository  {
	
	private static final String BUILD_RESULT_FILE_NAME = "buildResult";
	private static final String INVALID_FILE_NAME = "buildResult_invalid";
	private static final String USAGE_TRACKING_FILE_NAME = "usedby";
	private File repoDir;
	private LockManager<ModuleId> lockManager = new LockManager<ModuleId>();
	
	@Inject
	SharedBuildRepository(@Named(BuildServerConfig.REPOSITORY_ROOT_DIR) File rootDir) {
		this.repoDir = new File(rootDir, "shared");
		this.repoDir.mkdirs();
	}
	
	public Set<BuildResult> getBuildResults(ModuleId moduleId) {
		Lock readLock = lockManager.getLock(moduleId).readLock();
		try {
			readLock.lock();
			File moduleDir = moduleDir(moduleId);
			Set<BuildResult> buildResults = new HashSet<BuildResult>();
			if (moduleDir.exists()) {
				for (File dir : moduleDir.listFiles()) {
					File invalidFile = new File(dir, INVALID_FILE_NAME);
					File serializedBuildResult = new File(dir,BUILD_RESULT_FILE_NAME);
					if (!invalidFile.exists() && serializedBuildResult.exists()) {
						try {
							buildResults.add(BuildResultUtil.deserialize(serializedBuildResult));
						} catch (IOException e) {
							System.err.println("could not read buildResult " + serializedBuildResult + " due to " + e.getMessage());
						}
					}
				}
			}
			return buildResults;
		} finally {
			readLock.unlock();
		}
	}
	
	private void trackUsage(ModuleId moduleId, BuildId buildId, Checksum buildResultChecksum) {
		try {
			File moduleDir = moduleDir(moduleId);
			File usageFile = new File(moduleDir, USAGE_TRACKING_FILE_NAME);
			Map<BuildId, Checksum> usage = getOrCreateUsageMap(usageFile);
			Checksum previousChecksum = usage.get(buildId);
			usage.put(buildId, buildResultChecksum);
			cleanupPreviousIfNotUsed(moduleDir, usage, previousChecksum);
			new ObjectMapper().writeValue(usageFile, usage);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<BuildId, Checksum> getOrCreateUsageMap(File usedBy) throws IOException {
		Map<BuildId,Checksum> usage = new HashMap<BuildId, Checksum>();
		if (usedBy.exists()) {
			ObjectMapper objectMapper = new ObjectMapper();
			MapType mapType = objectMapper.getTypeFactory().constructMapType(Map.class, BuildId.class, Checksum.class);
			usage = objectMapper.readValue(usedBy,mapType);
		} else {
			usedBy.createNewFile();
		}
		return usage;
	}

	private void cleanupPreviousIfNotUsed(File moduleDir,Map<BuildId, Checksum> usage, Checksum previousChecksum) throws IOException {
		if (previousChecksum != null && !usage.containsValue(previousChecksum)) {
			File unusedBuildResult = new File(moduleDir, previousChecksum.toString());
			FileUtils.deleteDirectory(unusedBuildResult);
		}
	}
	
	void importBuildResultAndArtifacts(ModuleId moduleId, BuildId buildId, BuildResult buildResult, File inProgressArtifactsDir) throws IOException {
		Lock writeLock = lockManager.getLock(moduleId).writeLock();
		Checksum buildResultChecksum = buildResult.getChecksum();
		try {
			writeLock.lock();
			File checksumDir = checksumDir(moduleId, buildResultChecksum);
			File buildResultFile = new File(checksumDir,BUILD_RESULT_FILE_NAME);
			BuildResultUtil.serialize(buildResult, buildResultFile);
			
			File targetArtifactsDir = artifactsDir(moduleId, buildResultChecksum);
			if (targetArtifactsDir.exists()) {
				FileUtils.deleteDirectory(targetArtifactsDir);
			}
			FileUtils.moveDirectory(inProgressArtifactsDir, targetArtifactsDir);
			
			trackUsage(moduleId, buildId, buildResultChecksum);
		} finally {
			writeLock.unlock();
		}
	}
	
	public InputStream getArtifact(ModuleId moduleId, Checksum buildResultChecksum,String name) throws FileNotFoundException {
		Lock readLock = lockManager.getLock(moduleId).readLock();
		try {
			readLock.lock();
			File artifactsDir = artifactsDir(moduleId, buildResultChecksum);
			File artifactFile = new File(artifactsDir, name);
			return new FileInputStream(artifactFile);
		} finally {
			readLock.unlock();
		}
	}

	File artifactsDir(ModuleId moduleId, Checksum buildResultChecksum) {
		File artifactsDir = new File(checksumDir(moduleId, buildResultChecksum),"artifacts");
		artifactsDir.mkdirs();
		return artifactsDir;
	}
	
	void markAsInvalid(ModuleId moduleId, BuildId buildId, Checksum buildResultChecksum) throws IOException {
		Lock writeLock = lockManager.getLock(moduleId).writeLock();
		try {
			writeLock.lock();
			File checksumDir = checksumDir(moduleId, buildResultChecksum);
			File invalidFile = new File(checksumDir, INVALID_FILE_NAME);
			invalidFile.createNewFile();
			trackUsage(moduleId, buildId, buildResultChecksum);
		} finally {
			writeLock.unlock();
		}
	}
	
	private File checksumDir(ModuleId moduleId, Checksum buildResultChecksum) {
		File moduleDir = moduleDir(moduleId);
		File checksumDir = new File(moduleDir, buildResultChecksum.toString());
		checksumDir.mkdirs();
		return checksumDir;
	}

	private File moduleDir(ModuleId moduleId) {
		File moduleDir = new File(repoDir, moduleId.toString().replaceAll(":", "/"));
		moduleDir.mkdirs();
		return moduleDir;
	}

}
