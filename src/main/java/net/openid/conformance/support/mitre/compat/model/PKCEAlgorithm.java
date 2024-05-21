package net.openid.conformance.support.mitre.compat.model;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.Requirement;

/**
 * @author jricher
 *
 */
public final class PKCEAlgorithm extends Algorithm {

	/**
	 *
	 */
	private static final long serialVersionUID = 7752852583210088925L;

	public static final PKCEAlgorithm plain = new PKCEAlgorithm("plain", Requirement.REQUIRED);

	public static final PKCEAlgorithm S256 = new PKCEAlgorithm("S256", Requirement.OPTIONAL);

	public PKCEAlgorithm(String name, Requirement req) {
		super(name, req);
	}

	public PKCEAlgorithm(String name) {
		super(name, null);
	}

	public static PKCEAlgorithm parse(final String s) {
		if (s.equals(plain.getName())) {
			return plain;
		} else if (s.equals(S256.getName())) {
			return S256;
		} else {
			return new PKCEAlgorithm(s);
		}
	}



}
