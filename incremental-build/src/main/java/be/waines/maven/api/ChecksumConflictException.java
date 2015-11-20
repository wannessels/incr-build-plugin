package be.waines.maven.api;

import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;

public class ChecksumConflictException extends RuntimeException {

	private static final long serialVersionUID = -5895183767271704852L;
	private ModuleId moduleId;
	private Checksum checksum;
	private Checksum conflictingChecksum;

	public ChecksumConflictException(ModuleId moduleId, Checksum checksum, Checksum conflictingChecksum) {
		this.moduleId = moduleId;
		this.checksum = checksum;
		this.conflictingChecksum = conflictingChecksum;
	}
	
	public Checksum getChecksum() {
		return checksum;
	}
	
	public Checksum getConflictingChecksum() {
		return conflictingChecksum;
	}
	
	@Override
	public String getMessage() {
		return String.format("checksum %s does not match expected checksum %s for module %s",checksum,conflictingChecksum,moduleId);
	}
	
}
