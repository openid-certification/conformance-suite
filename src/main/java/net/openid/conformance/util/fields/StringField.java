package net.openid.conformance.util.fields;

import java.util.Collections;
import java.util.Set;

public class StringField extends Field {

	private Set<String> enums = Collections.emptySet();

	public Set<String> getEnums() {
		return this.enums;
	}

	public void setEnums(Set<String> enums) {
		this.enums = enums;
	}

	public static class Builder {
		private StringField stringField;

		public Builder(String path) {
			stringField = new StringField();
			stringField.setPath(path);
		}

		public Builder setPattern(String pattern) {
			stringField.setPattern(pattern);
			return this;
		}

		public Builder setFieldOptional() {
			stringField.setOptional();
			return this;
		}

		public Builder setMaxLength(int maxLength) {
			stringField.setMaxLength(maxLength);
			return this;
		}

		public Builder setEnums(Set<String> enums) {
			stringField.enums = enums;
			return this;
		}

		public StringField build() {
			return stringField;
		}
	}
}
