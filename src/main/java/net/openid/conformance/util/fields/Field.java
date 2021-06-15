package net.openid.conformance.util.fields;

public abstract class Field {

	private boolean isOptional;
	private String path = "";
	private String pattern = "";
	private int maxLength;
	private int minLength;

	public boolean isOptional() {
		return isOptional;
	}

	public void setOptional(boolean optional) {
		isOptional = optional;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public int getMinLength() {
		return minLength;
	}

	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}
}
