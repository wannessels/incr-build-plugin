package be.waines.maven.incremental.distributed.server;

import java.io.File;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Main {
	
	private static final RestServer server = new RestServerImpl(8081, new File(System.getProperty("storageDir"),"buildRepository"));

	public static void main(String[] args) throws Exception {
		configureLogging();

		server.start();
	}

	private static void configureLogging() {
		PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} [%t] %-5p %c %x - %m%n");
		ConsoleAppender appender = new ConsoleAppender(layout);
		Logger rootLogger = Logger.getRootLogger();
		rootLogger.removeAllAppenders();
		rootLogger.addAppender(appender);
	}

}
