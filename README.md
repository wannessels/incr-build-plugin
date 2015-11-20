# Incremental build for maven
This maven extension can skip the entire execution of a module if no changes have been detected.

## Important limitations and assumptions
* requires Maven 3.1.2+
* incr-build _does not care for maven lifecycle phases_. It does not make a distinction between `mvn clean` and `mvn validate`. This makes incr-build only useful in your CI, where your maven call is always identical.
* incr-build only looks at `pom.xml`, `/src` directory, and `dependencies` for changes.
* The only recorded output is your module artifacts. This means no support for a separate module for db changes, or setting up some external files or system

## Local builds
Add to your pom.xml
````xml
<build>
	<extensions>
		<extension>
			<groupId>be.waines.maven</groupId>
			<artifactId>incremental-build</artifactId>
			<version>3.1.0-SNAPSHOT</version>
		</extension>
	</extensions>
</build>
````
Because maven cannot conditionally enable a build extension (you can't put an extension in a profile), you have to enable it via `mvn -DenableIncrementalBuild`

Before maven builds your module, incr-build will calculate the checksum of your `pom.xml`, all files & folders at `/src`, and all `dependencies`. It will then look for your previous build checksums in `target/.incrbld` and compare with the calculated checksums. If they match, the entire execution of the module is skipped. No clean, resources, compile, jar, test, ...
If the checksums don't match, the module is built as usual, and afterwards the calculated checksums is saved in `target/.incrbld` together with links to any generated artifacts.

## Distributed builds
Add to your pom.xml
````xml
<build>
	<extensions>
		<extension>
			<groupId>be.waines.maven.incremental</groupId>
			<artifactId>distributed-client</artifactId>
			<version>3.1.0-SNAPSHOT</version>
		</extension>
	</extensions>
</build>
````
You have to run the distributed server, use something like `mvn -pl be.waines.maven.incremental:distributed-server exec:java -DstorageDir=/tmp/distributed-storage -Dexec.mainClass=be.waines.maven.incremental.distributed.server.Main` , or you could create a fat jar with `mvn -pl be.waines.maven.incremental:distributed-server assembly:single`

You can now run your build with `mvn -DenableIncrementalBuild -Ddistributed.server.url=http://hostname:8081	-Ddistributed.build=$JOB_NAME`.

Your maven build will now also download/upload build results and artifacts to the server.

### Combining incr-build-plugin with cloud-build-plugin
Incr-build-plugin can and should be combined with [cloud-build-plugin](https://github.com/wannessels/cloud-build-plugin). This has a few advantages:
* If a node started a build later or builds slower, it can very quickly catch up to the other nodes because it can use it's shared build result & artifacts.
* Because the build results are identified by checksums, they can be shared even between different builds of different branches or even different projects.

Obviously the combination of incr-build-plugin and cloud-build-plugin can lead to drastically reduced build times.

There is however a potential issue: when using cloud-build-plugin tests are distributed over all available nodes. This means that a build of a maven module might skip a failing test and have a successful build of that module, even when that module failed on another node. For that reason the distributed-server internally has two separate repositories: an inProgressRepository and a sharedRepository. The build results are only shared within a build (based on distributed.build parameter). After all the nodes have finished, call `mvn be.waines.maven.incremental:distributed-client:3.1.0-SNAPSHOT:commit -Ddistributed.server.url=http://hostname:8081 -Ddistributed.build=$JOB_NAME` . The distributed-server will then verify for every build-result no failure was reported, and share the build result with all other builds accordingly.

### Differentiating builds
If you have a staged build where for example you don't run webdriver/end2end tests at your regular checkin build, but you do want to run them in a separate build, then you need a mechanism to force the rebuild of a module.

For this you can implement a `BuildResultExtensionCalculator` and mark it with `@Component`. Here's a short example
````java
@Component(role=BuildResultExtensionCalculator.class, hint="okapi-webdriver")
public class WebdriverExtension implements BuildResultExtensionCalculator{
	
	@Override
	public BuildResultExtension calculate(ProjectExecutionEvent projectExecutionEvent) {
		boolean isWebdriversActive = projectExecutionEvent.getSession().getRequest().getActiveProfiles().contains("webdrivers");
		boolean isWebdriverProject = hasWebdriverDependency(projectExecutionEvent.getProject());
		
		if (isWebdriverProject && isWebdriversActive) {
			return new BuildResultExtension("webdriver");
		} else {
			return null;
		}
	}

	private boolean hasWebdriverDependency(MavenProject project) {
		for (Dependency dependency : project.getDependencies()) {
			if (dependency.getArtifactId().equals(WEBDRIVER_ARTIFACTID) &&
					dependency.getGroupId().equals(WEBDRIVER_GROUPID)) {
				return true;
			}
		}
		return false;
	}

}
````
