package net.openid.conformance.util.field;

public class BooleanField extends Field {

	private BooleanField(boolean optional, boolean nullable, String path) {
		super(optional, nullable, path);
	}

	public static class Builder {

		private final String path;
		private boolean optional;
		private boolean nullable;

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

		public BooleanField build() {
			return new BooleanField(this.optional, this.nullable, this.path);
		}
	}
}
