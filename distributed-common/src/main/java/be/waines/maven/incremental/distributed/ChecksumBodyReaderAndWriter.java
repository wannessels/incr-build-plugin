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

import be.waines.maven.model.Checksum;

@Singleton
@Provider
public class ChecksumBodyReaderAndWriter implements MessageBodyReader<Checksum>, MessageBodyWriter<Checksum>  {

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type.isAssignableFrom(Checksum.class);
	}

	@Override
	public Checksum readFrom(Class<Checksum> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		String value = IOUtils.toString(entityStream, "utf-8");
		return new Checksum(value);
	}
	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type.isAssignableFrom(Checksum.class);
	}

	@Override
	public long getSize(Checksum t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return 40;
	}

	@Override
	public void writeTo(Checksum checksum, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		entityStream.write(checksum.toString().getBytes());
	}


}
