package be.waines.maven.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;

class PutHandler extends ResourceHandler {
	private final File resourceBase;

	public PutHandler(File repositoryDirectory) {
		this.resourceBase = repositoryDirectory;
		setResourceBase(resourceBase.getAbsolutePath());
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		super.handle(target, baseRequest, request, response);
		
		if (baseRequest.isHandled() || !"PUT".equals(baseRequest.getMethod())) {
			return;
		}

		baseRequest.setHandled(true);

		File file = new File(resourceBase, URLDecoder.decode(request.getPathInfo(),"iso-8859-1"));
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		ServletInputStream in = request.getInputStream();
		try {
			IOUtils.copy(in, out);
		} finally {
			in.close();
			out.close();
		}

	}

}