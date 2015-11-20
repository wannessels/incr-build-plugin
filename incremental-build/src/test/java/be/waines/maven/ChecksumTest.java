package be.waines.maven;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Stopwatch;

import be.waines.maven.model.Checksum;
import be.waines.maven.model.ChecksumUtil;

public class ChecksumTest {

	public static void main(String[] args) throws IOException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		File projectDirectory = new File("C:\\PrivateWS\\okapi\\sources\\be.waines.ctx.db\\be.waines.ctx.db.okapi");
		File pomFile = new File(projectDirectory,"pom.xml");
		File sourcesDirectory = new File(projectDirectory, "src");
		Checksum checksum = ChecksumUtil.calculateChecksum(pomFile, sourcesDirectory);
		stopwatch.stop();
		System.out.println(stopwatch.toString());
		System.out.println(checksum);
	}
}
