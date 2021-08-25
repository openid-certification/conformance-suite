package net.openid.conformance.util.field;

public class BooleanField extends Field {

	public BooleanField(String path) { super(path); }

	public BooleanField(boolean optional, boolean nullable, String path) {
		super(optional, nullable, path);
	}

	public static class Builder extends FieldBuilder {

		public Builder(String path) {
			super(path);
		}

		@Override
		public BooleanField build() {
			return new BooleanField(this.optional, this.nullable, this.path);
		}
	}
}
