package net.openid.conformance.condition.common;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class EnsureIncomingTls12WithBCP195SecureCipherOrTls13 extends EnsureIncomingTls12WithSecureCipherOrTls13 {

	@Override
	protected List<String> getRecomendedCiphers() {
		// List of the latest recommended cyphers from BCP195; note that apache uses the OpenSSL cipher name
		// unlike the constants found in the CipherSuite enum used by DisallowInsecureCipher which
		// uses the IANA name.
		//
		// See https://ciphersuite.info/cs/ for mappings.
		return ImmutableList.of(
			"ECDHE-RSA-AES128-GCM-SHA256",
			"ECDHE-RSA-AES256-GCM-SHA384",
			"ECDHE-ECDSA-AES128-GCM-SHA256",
			"ECDHE-ECDSA-AES256-GCM-SHA384");
	}
}
