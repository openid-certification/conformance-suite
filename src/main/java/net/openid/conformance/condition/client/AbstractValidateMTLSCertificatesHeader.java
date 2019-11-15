package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.util.PEMFormatter;

import java.util.List;

public abstract class AbstractValidateMTLSCertificatesHeader extends AbstractCondition {

	protected void validateMTLSCertificatesHeader(String certString, String keyString, String caString) {
		if (Strings.isNullOrEmpty(certString) || Strings.isNullOrEmpty(keyString)) {
			throw error("Couldn't find TLS client certificate or key for MTLS");
		}

		if (Strings.isNullOrEmpty(caString)) {
			// Not an error; we just won't send a CA chain
			log("No certificate authority found for MTLS");
		}

		validatePEMHeader(certString, keyString, caString);

		logSuccess("MTLS certificates header is valid");
	}

	private void validatePEMHeader(String certString, String keyString, String caString) {

		List<String> certHeaderListExpect = ImmutableList.of("-----BEGIN CERTIFICATE-----", "-----BEGIN RSA CERTIFICATE-----");

		List<String> keyHeaderListExpect = ImmutableList.of("-----BEGIN PRIVATE KEY-----", "-----BEGIN RSA PRIVATE KEY-----", "-----BEGIN EC PRIVATE KEY-----");

		List<String> certHeaderList = PEMFormatter.extractPEMHeader(certString);
		for (String certHeader : certHeaderList) {
			if (!certHeaderListExpect.contains(certHeader)) {
				throw error("You uploaded something that begins " + certHeader + ", but you need to provide a certificate which would begin with " + String.join(", ", certHeaderListExpect));
			}
		}

		List<String> keyHeaderList = PEMFormatter.extractPEMHeader(keyString);
		for (String keyHeader : keyHeaderList) {
			if (!keyHeaderListExpect.contains(keyHeader)){
				throw error("You uploaded something that begins " + keyHeader + ", but you need to provide a private key which would begin with " + String.join(", ", keyHeaderListExpect));
			}
		}

		if (caString != null) {
			List<String> caHeaderList = PEMFormatter.extractPEMHeader(caString);
			for (String caHeader : caHeaderList) {
				if (!certHeaderListExpect.contains(caHeader)) {
					throw error("You uploaded something that begins " + caHeader + ", but you need to provide a certificate authority which would begin with " + String.join(", ", certHeaderListExpect));
				}
			}
		}
	}
}
