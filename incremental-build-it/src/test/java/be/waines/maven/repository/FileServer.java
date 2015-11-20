package be.waines.maven.repository;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

public class FileServer {
	private Server server;
	private URL url;
	private int port;
	private File repositoryRootDir;
	
	public static void main(String[] args) throws InterruptedException {
		FileServer fileServer = new FileServer(8082, new File("c:/temp/repo"));
		fileServer.start();
		fileServer.join();
	}
	
	public FileServer(int port, File repositoryRootDir) {
		this.port = port;
		this.repositoryRootDir = repositoryRootDir;
	}
	
	public void start() {
		try {
			if (server != null && server.isRunning()) {
				throw new IllegalStateException("server already running on " + url);
			}
			server = new Server(port);
			PutHandler resourceHandler = new PutHandler(repositoryRootDir);
			resourceHandler.setDirectoriesListed(true);
			server.setHandler(resourceHandler);
			server.start();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void stop() {
		try {
			server.stop();
			server.join();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void join() throws InterruptedException {
		server.join();
	}

}
