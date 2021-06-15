package net.openid.conformance.util.fields;

import java.util.Collections;
import java.util.List;

public class StringField extends Field {

	private List<String> enumList = Collections.emptyList();

	public List<String> getEnumList() {
		return this.enumList;
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

		public Builder setFieldOptional(boolean isOptional) {
			stringField.setOptional(isOptional);
			return this;
		}

		public Builder setMaxLength(int maxLength) {
			stringField.setMaxLength(maxLength);
			return this;
		}

		public Builder setEnumList(List<String> enumList) {
			stringField.enumList = enumList;
			return this;
		}

		public StringField build() {
			return stringField;
		}
	}
}
