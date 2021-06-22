package net.openid.conformance.util.fields;


import java.util.Collections;
import java.util.Set;

/**
 * The base class describes features of value that can contain key
 */
public abstract class Field {

	private boolean isOptional;
	private String path = "";
	private String pattern = "";
	private int maxLength;
	private int minLength;
	private int maxItems;
	private int minItems;
	private int maximum;
	private Set<String> enums = Collections.emptySet();

	public Set<String> getEnums() {
		return this.enums;
	}

	public void setEnums(Set<String> enums) {
		this.enums = enums;
	}

	public boolean isOptional() {
		return isOptional;
	}

	public void setOptional() {
		isOptional = true;
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

	public int getMaxItems() {
		return maxItems;
	}

	public void setMaxItems(int maxItems) {
		this.maxItems = maxItems;
	}
	public int getMaximum() {
		return maximum;
	}

	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}


	public int getMinItems() {
		return minItems;
	}

	public void setMinItems(int minItems) {
		this.minItems = minItems;
	}
}
