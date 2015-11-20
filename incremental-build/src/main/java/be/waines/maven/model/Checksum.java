package be.waines.maven.model;

import java.io.Serializable;

import javax.xml.bind.DatatypeConverter;

public class Checksum implements Serializable, Comparable<Checksum>{

	private static final long serialVersionUID = 2321662727061733325L;
	
	public String value;
	
	public Checksum(byte[] value) {
		this(DatatypeConverter.printHexBinary(value));
	}
	
	public Checksum(String value) {
		assert value != null;
		assert value.length() == 40;
		this.value = value;
	}
	
	
	@SuppressWarnings("unused")
	private Checksum() {}
	
	@Override
	public String toString() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	public Checksum xor(Checksum other) {
		byte[] thisValue = DatatypeConverter.parseHexBinary(this.value);
		byte[] otherValue = DatatypeConverter.parseHexBinary(other.value);
		byte[] newValue = new byte[thisValue.length];
		for (int i = 0; i < newValue.length; i++) {
			newValue[i] = (byte) (thisValue[i] ^ otherValue[i]);
		}
		return new Checksum(newValue);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Checksum other = (Checksum) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public int compareTo(Checksum other) {
		return this.value.compareTo(other.value);
	}
	
	
}
