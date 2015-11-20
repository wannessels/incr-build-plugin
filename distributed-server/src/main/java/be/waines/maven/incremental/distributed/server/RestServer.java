package be.waines.maven.incremental.distributed.server;

import java.net.URL;

public interface RestServer {

	public abstract void start();

	public abstract void stop();

	public abstract void join() throws InterruptedException;

}