package be.waines.maven.incremental.distributed.server.impl;

import java.io.File;

import javax.inject.Singleton;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import be.waines.maven.incremental.distributed.BuildIdBodyReaderAndWriter;
import be.waines.maven.incremental.distributed.ChecksumBodyReaderAndWriter;
import be.waines.maven.incremental.distributed.server.resources.BuildRestService;


public class BuildServerConfig extends ResourceConfig {
	
	public static final String REPOSITORY_ROOT_DIR = "repositoryRootDir";
	private final File repositoryRootDir;
	
	public BuildServerConfig(File repositoryRootDir) {
		this.repositoryRootDir = repositoryRootDir;
		register(new AbstractBinder() {
			protected void configure() {
				bind(BuildServerConfig.this.repositoryRootDir).named(REPOSITORY_ROOT_DIR).to(File.class);
				bind(InProgressRepositories.class).to(InProgressRepositories.class).in(Singleton.class);
				bind(SharedBuildRepository.class).to(SharedBuildRepository.class).in(Singleton.class);
			};
		});
		registerClasses(BuildRestService.class);
		
		register(BuildIdBodyReaderAndWriter.class);
		register(ChecksumBodyReaderAndWriter.class);
		//registerClasses(SerializationMessageBodyReaderAndWriter.class);
		register(JacksonJsonProvider.class);
	}
	

}
