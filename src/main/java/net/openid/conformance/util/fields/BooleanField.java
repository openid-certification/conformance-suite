package net.openid.conformance.util.fields;

public class BooleanField extends Field {

	public static class Builder {
		private BooleanField booleanField;

		public Builder(String path) {
			booleanField = new BooleanField();
			booleanField.setPath(path);
		}

		public Builder setPattern(String pattern) {
			booleanField.setPattern(pattern);
			return this;
		}

		public Builder setFieldOptional() {
			booleanField.setOptional();
			return this;
		}

		public BooleanField build() {
			return booleanField;
		}
	}
}
