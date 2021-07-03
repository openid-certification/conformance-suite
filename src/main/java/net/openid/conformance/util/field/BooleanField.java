package net.openid.conformance.util.field;

public class BooleanField extends Field {

	public BooleanField(String path) {
		super(path);
	}

	public static class Builder extends FieldBuilder {

		public Builder(String path) {
			super(path);
		}

		public BooleanField build() {
			return new BooleanField(this.path);
		}
	}
}