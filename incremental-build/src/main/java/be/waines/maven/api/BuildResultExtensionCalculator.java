package be.waines.maven.api;

import org.apache.maven.execution.ProjectExecutionEvent;

public interface BuildResultExtensionCalculator {

	BuildResultExtension calculate(ProjectExecutionEvent projectExecutionEvent);
}
