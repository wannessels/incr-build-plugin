package be.waines.maven.impl;

import be.waines.maven.api.IncrementalReactor;
import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;

public class NoOpIncrementalReactor implements IncrementalReactor {

	@Override
	public Checksum getChecksum(ModuleId moduleId) {
		return null;
	}

	@Override
	public void setChecksum(ModuleId moduleId, Checksum checksum) {
	}

	@Override
	public boolean contains(ModuleId moduleId) {
		return false;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

}
