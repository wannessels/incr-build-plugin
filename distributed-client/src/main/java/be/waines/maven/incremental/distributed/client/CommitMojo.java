package be.waines.maven.incremental.distributed.client;

import javax.ws.rs.client.Entity;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name="commit", requiresProject=false)
public class CommitMojo extends AbstractMojo{

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		String url = Connection.URL + "/" + Connection.BUILD_ID;
		
		getLog().info("committing " + url);
		Connection.CLIENT.target(url).path("commit").request().put(Entity.text(""));
	}

}
