package net.openid.conformance.util.field;

import java.util.Collections;
import java.util.Set;

public class StringField extends Field {

	private StringField(boolean optional, boolean nullable, String path, String pattern,
						int maxLength, int minLength, Set<String> enums) {
		super(optional, nullable, path, pattern, maxLength, minLength, enums);
	}

	public static class Builder {

		private final String path;
		private String pattern = "";
		private boolean optional;
		private boolean nullable;
		private int maxLength;
		private int minLength;
		private Set<String> enums = Collections.emptySet();

		public Builder(String path) {
			this.path = path;
		}

		public Builder setOptional() {
			this.optional = true;
			return this;
		}

		public Builder setOptional(boolean value) {
			this.optional = value;
			return this;
		}

		public Builder setNullable() {
			this.nullable = true;
			return this;
		}

		public Builder setPattern(String pattern) {
			this.pattern = pattern;
			return this;
		}

		public Builder setMaxLength(int maxLength) {
			this.maxLength = maxLength;
			return this;
		}

		public Builder setMinLength(int minLength) {
			this.minLength = minLength;
			return this;
		}

		public Builder setEnums(Set<String> enums) {
			this.enums = enums;
			return this;
		}

		public StringField build() {
			return new StringField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.enums);
		}
	}
}
