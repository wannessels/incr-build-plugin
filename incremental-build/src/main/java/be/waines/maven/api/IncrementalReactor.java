package be.waines.maven.api;

import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;

public interface IncrementalReactor {

	public abstract Checksum getChecksum(ModuleId moduleId);

	public abstract void setChecksum(ModuleId moduleId, Checksum checksum) throws ChecksumConflictException;

	public abstract boolean contains(ModuleId moduleId);
	
	public abstract boolean isEnabled();

}