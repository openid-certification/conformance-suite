package net.openid.conformance.validation;

public interface Match {

	boolean matches(String value);
	boolean matches(Number value);

}
