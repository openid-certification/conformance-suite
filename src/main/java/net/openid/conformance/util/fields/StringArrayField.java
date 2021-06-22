package net.openid.conformance.util.fields;

import java.util.Set;

public class StringArrayField extends Field {

	public static class Builder {
		private StringArrayField stringArrayField;

		public Builder(String path) {
			stringArrayField = new StringArrayField();
			stringArrayField.setPath(path);
		}

		public Builder setPattern(String pattern) {
			stringArrayField.setPattern(pattern);
			return this;
		}

		public Builder setFieldOptional() {
			stringArrayField.setOptional();
			return this;
		}

		public Builder setMaxLength(int maxLength) {
			stringArrayField.setMaxLength(maxLength);
			return this;
		}

		public Builder setEnum(Set<String> enums) {
			stringArrayField.setEnums(enums);
			return this;
		}

		public Builder setMaxItems(int maxItems) {
			stringArrayField.setMaxItems(maxItems);
			return this;
		}

		public Builder setMinItems(int minItems) {
			stringArrayField.setMinItems(minItems);
			return this;
		}

		public StringArrayField build() {
			return stringArrayField;
		}
	}
}
