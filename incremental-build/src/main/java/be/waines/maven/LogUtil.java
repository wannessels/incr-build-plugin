package be.waines.maven;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import be.waines.maven.model.ModuleIdFactory;

@Component(role=LogUtil.class)
public class LogUtil {

	@Requirement
	private Logger mvnLogger;
	
	private final org.apache.log4j.Logger log4jRootLogger = org.apache.log4j.Logger.getLogger("incremental");
	private volatile org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger("incremental");
	
	private boolean loggingToFile = false;

	@Requirement
	private ModuleIdFactory moduleIdFactory;
	
	public LogUtil() {
		
	}
	
	public void logToFile(File file) {
		log4jRootLogger.setLevel(Level.DEBUG);
		if (!loggingToFile) {
			try {
				PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} [%t] %-5p %c %x - %m%n");
				FileAppender appender = new FileAppender(layout, file.getAbsolutePath(), false);
				log4jRootLogger.addAppender(appender);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				loggingToFile = true;
			}
		}
	}
	
	public void logProject(MavenProject project) {
		log4jLogger = org.apache.log4j.Logger.getLogger("incremental." + moduleIdFactory.create(project));
	}
	public void info(String msg) {
		mvnLogger.info(msg);
		log4jLogger.info(msg);
	}	
	
	public void debug(String msg) {
		mvnLogger.debug(msg);
		log4jLogger.debug(msg);
	}

	public void warn(String msg, Exception e) {
		mvnLogger.warn(msg, e);
		log4jLogger.warn(msg, e);
	}
	
	public void warn(String msg) {
		mvnLogger.warn(msg);
		log4jLogger.warn(msg);
	}
	
	public void error(String msg) {
		mvnLogger.error(msg);
		log4jLogger.error(msg);
	}
}
