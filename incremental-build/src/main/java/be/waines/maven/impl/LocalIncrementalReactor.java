package be.waines.maven.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.plexus.component.annotations.Component;

import be.waines.maven.api.IncrementalReactor;
import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;

@Component(role=IncrementalReactor.class, hint="local")
public class LocalIncrementalReactor implements IncrementalReactor {
	
	private ConcurrentMap<ModuleId, Checksum> modulesInReactor = new ConcurrentHashMap<ModuleId, Checksum>();

	@Override
	public Checksum getChecksum(ModuleId moduleId) {
		return modulesInReactor.get(moduleId);
	}
	
	@Override
	public void setChecksum(ModuleId moduleId, Checksum checksum) {
		modulesInReactor.put(moduleId, checksum);
	}

	@Override
	public boolean contains(ModuleId moduleId) {
		return modulesInReactor.containsKey(moduleId);
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

}
