package net.openid.conformance.util.field;

import java.util.Collections;
import java.util.Set;

public class StringArrayField extends Field {

	private StringArrayField(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength,
							 int maxItems, int minItems, Set<String> enums) {
		super(optional, nullable, path, pattern, maxLength, minLength, maxItems, minItems, enums);
	}

	public static class Builder {

		private boolean optional;
		private boolean nullable;
		private String path = "";
		private String pattern = "";
		private int maxLength;
		private int minLength;
		private Set<String> enums = Collections.emptySet();
		private int maxItems;
		private int minItems;

		public Builder(String path) {
			this.path = path;
		}

		public Builder setOptional() {
			this.optional = true;
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

		public Builder setMaxItems(int maxItems) {
			this.maxItems = maxItems;
			return this;
		}

		public Builder setMinItems(int minItems) {
			this.minItems = minItems;
			return this;
		}

		public StringArrayField build() {
			return new StringArrayField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxItems, this.minItems, this.enums);
		}
	}
}
