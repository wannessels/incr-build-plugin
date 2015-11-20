package be.waines.maven.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.ProjectExecutionEvent;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import be.waines.maven.api.BuildResultExtension;
import be.waines.maven.api.BuildResultExtensionCalculator;

@Component(role=BuildResultExtensionCalculators.class)
public class BuildResultExtensionCalculators {
	
	@Requirement(role=BuildResultExtensionCalculator.class, optional=true)
	private List<BuildResultExtensionCalculator> extensionCalculators;

	public Set<BuildResultExtension> calculate(ProjectExecutionEvent projectExecutionEvent) {
		Set<BuildResultExtension> result = new LinkedHashSet<BuildResultExtension>();
		for (BuildResultExtensionCalculator calculator : extensionCalculators) {
			BuildResultExtension extension = calculator.calculate(projectExecutionEvent);
			if (extension != null) {
				result.add(extension);
			}
		}
		return result;
	}
}
