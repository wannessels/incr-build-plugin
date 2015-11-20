package be.waines.maven.incremental.distributed.server;

import java.io.File;
import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import be.waines.maven.incremental.distributed.server.impl.BuildServerConfig;
import be.waines.maven.incremental.distributed.server.resources.BuildRestService;

public class RestServerImpl implements RestServer {
	
	private Server server;
	private URL url;
	private int port;
	private File repositoryRootDir;
	
	public RestServerImpl(int port, File repositoryRootDir) {
		this.port = port;
		this.repositoryRootDir = repositoryRootDir;
	}
	
	@Override
	public void start() {
		
		try {
			if (server != null && server.isRunning()) {
				throw new IllegalStateException("server already running on " + url);
			}
			ResourceConfig config = new BuildServerConfig(repositoryRootDir);
			//config.register(BuildRestService.class);
			ServletContainer servletContainer = new ServletContainer(config);
			ServletHolder sh = new ServletHolder(servletContainer);                
			server = new Server(port);		
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
	        context.setContextPath("/");
	        context.addServlet(sh, "/*");
			server.setHandler(context);
			server.start();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void stop() {
		try {
			server.stop();
			server.join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void join() throws InterruptedException {
		server.join();
	}

}
