package be.waines.maven.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.util.io.AutoCRLFInputStream;


//TODO check compatibility met git
//TODO gebruik checksum uit .git repository indien geen lokale changes
public class ChecksumUtil {

	public static Checksum calculateSourcesChecksum(MavenProject project) throws IOException {
		
		File pomFile = project.getFile();
		File sourcesDirectory = new File(project.getBasedir(),"src");
		
		return calculateChecksum(pomFile, sourcesDirectory);
	}
	
	public static Checksum calculateChecksum(File singleFile) throws IOException {
		return calculateChecksum(singleFile, null);
	}

	public static Checksum calculateChecksum(File pomfile, File sourcesDirectory) throws IOException {
		TreeFormatter treeFormatter = new TreeFormatter();
		ObjectInserter objectInserter = new ObjectInserter.Formatter();

		addFile(treeFormatter, objectInserter, pomfile);
		if (sourcesDirectory != null && sourcesDirectory.exists()) {
			addFolder(treeFormatter, objectInserter, sourcesDirectory);
		}
		
		ObjectId objectId = treeFormatter.computeId(objectInserter);
		return new Checksum(objectId.getName());
	}
	
	private static void addFile(TreeFormatter treeFormatter, ObjectInserter objectInserter, File file) throws IOException {
		InputStream inputStream = null;
		try {
			inputStream = new AutoCRLFInputStream(new FileInputStream(file),true);
			byte[] filteredContents = IOUtils.toByteArray(inputStream);
			ObjectId fileId = objectInserter.idFor(Constants.OBJ_BLOB, filteredContents);
			treeFormatter.append(file.getName(), FileMode.REGULAR_FILE, fileId);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
	
	private static void addFolder(TreeFormatter treeFormatter, ObjectInserter objectInserter, File folder) throws IOException {
		TreeFormatter childFormatter = new TreeFormatter();
		for (File childFile : folder.listFiles()) {
			if (childFile.isDirectory()) {
				addFolder(childFormatter, objectInserter, childFile);
			} else {
				addFile(childFormatter, objectInserter, childFile);
			}
		}
		treeFormatter.append(folder.getName(), FileMode.TREE, childFormatter.computeId(objectInserter));
	}
	
}
