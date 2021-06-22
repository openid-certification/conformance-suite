package net.openid.conformance.util.fields;

public class ArrayField extends Field {

	public static class Builder {
		private ArrayField arrayField;

		public Builder(String path) {
			arrayField = new ArrayField();
			arrayField.setPath(path);
		}

		public Builder setFieldOptional() {
			arrayField.setOptional();
			return this;
		}

		public Builder setMaxItems(int maxItems) {
			arrayField.setMaxItems(maxItems);
			return this;
		}

		public Builder setMinItems(int minItems) {
			arrayField.setMinItems(minItems);
			return this;
		}

		public ArrayField build() {
			return arrayField;
		}
	}
}
