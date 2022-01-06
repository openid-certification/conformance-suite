package net.openid.conformance.util.field;


import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The base class describes features of value that can contain key
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class Field {

	protected boolean optional;
	protected boolean nullable;
	protected String path = "";
	protected String pattern = "";
	protected int maxLength;
	protected int minLength;
	protected int maxItems;
	protected int minItems;
	protected BigDecimal maxValue;
	protected BigDecimal minValue;
	protected Consumer<JsonObject> validator;
	protected Set<String> enums = Collections.emptySet();

	Field(boolean optional, boolean nullable, String path) {
		this.optional = optional;
		this.nullable = nullable;
		this.path = path;
	}

	Field(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength, int maxItems, int minItems, Set<String> enums) {
		this.optional = optional;
		this.nullable = nullable;
		this.path = path;
		this.pattern = pattern;
		this.maxLength = maxLength;
		this.minLength = minLength;
		this.maxItems = maxItems;
		this.minItems = minItems;
		this.enums = enums;
	}

	Field(boolean optional, boolean nullable, String path, Consumer<JsonObject> validator, int minItems, int maxItems) {
		this.optional = optional;
		this.nullable = nullable;
		this.path = path;
		this.validator = validator;
		this.maxItems = maxItems;
		this.minItems = minItems;

	}

	Field(boolean optional, boolean nullable, String path, Consumer<JsonObject> validator) {
		this.optional = optional;
		this.nullable = nullable;
		this.path = path;
		this.validator = validator;
	}

	Field(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength) {
		this.optional = optional;
		this.nullable = nullable;
		this.path = path;
		this.pattern = pattern;
		this.maxLength = maxLength;
		this.minLength = minLength;
	}

	Field(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength, Set<String> enums) {
		this.optional = optional;
		this.nullable = nullable;
		this.path = path;
		this.pattern = pattern;
		this.maxLength = maxLength;
		this.minLength = minLength;
		this.enums = enums;
	}

	public boolean isOptional() {
		return optional;
	}

	public boolean isNullable() {
		return nullable;
	}

	public String getPath() {
		return path;
	}

	public String getPattern() {
		return pattern;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public int getMinLength() {
		return minLength;
	}

	public int getMaxItems() {
		return maxItems;
	}

	public int getMinItems() {
		return minItems;
	}

	public BigDecimal getMaxValue() {
		return maxValue;
	}

	public BigDecimal getMinValue() {
		return minValue;
	}

	public Consumer<JsonObject> getValidator() {
		return validator;
	}

	public Set<String> getEnums() {
		return enums;
	}
}
