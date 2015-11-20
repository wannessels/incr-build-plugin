package be.waines.maven.incremental.distributed;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;

import be.waines.maven.model.BuildId;

@Singleton
@Provider
public class BuildIdBodyReaderAndWriter implements MessageBodyReader<BuildId>,
		MessageBodyWriter<BuildId> {

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type.isAssignableFrom(BuildId.class);
	}

	@Override
	public BuildId readFrom(Class<BuildId> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
		String value = IOUtils.toString(entityStream, "utf-8");
		return new BuildId(value);
	}
	
	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type.isAssignableFrom(BuildId.class);
	}

	@Override
	public long getSize(BuildId buildId, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return buildId.toString().length();
	}

	@Override
	public void writeTo(BuildId buildId, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
		entityStream.write(buildId.toString().getBytes("utf-8"));
	}

}
