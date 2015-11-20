package be.waines.maven.api;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import be.waines.maven.model.Checksum;

public class BuildResultExtension implements Serializable{
	
	private static final long serialVersionUID = -456160567777532373L;
	
	private String name;
	
	@SuppressWarnings("unused")
	private BuildResultExtension() {}

	public BuildResultExtension(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Build result extension: " + name;
	}
	
	public String getName() {
		return name;
	}
	
	public Checksum calculateChecksum() {
		try {
			MessageDigest crypt = MessageDigest.getInstance("SHA-1");
			crypt.reset();
			crypt.update(name.getBytes("utf-8"));
			byte [] result = crypt.digest();
			return new Checksum(result);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BuildResultExtension other = (BuildResultExtension) obj;
		if (!name.equals(other.name))
			return false;
		return true;
	}
	
	

}
