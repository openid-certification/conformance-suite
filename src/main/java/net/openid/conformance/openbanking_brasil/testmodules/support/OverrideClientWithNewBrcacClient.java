package net.openid.conformance.openbanking_brasil.testmodules.support;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OverrideClientWithNewBrcacClient extends AbstractOverrideClient {
	@Override
	String clientCert() {
		try {
			return IOUtils.resourceToString("dcr_brcac2022_hardcoded_creds/brcac_2022.pem", StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());
		} catch (IOException e) {
			throw error("Could not load old BRCAC certificate", e);
		}
	}

	@Override
	String clientKey() {
		try {
			return IOUtils.resourceToString("dcr_brcac2022_hardcoded_creds/brcac_2022.key", StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());
		} catch (IOException e) {
			throw error("Could not load old BRCAC key", e);

		}
	}

	@Override
	String role() {
		return "New BRCAC client";
	}

	@Override
	String directoryClientId() {
		return "QjRzruzFWi_U_tMahlz01";
	}

	@Override
	String clientJwks() {
		try {
			return IOUtils.resourceToString("dcr_brcac2022_hardcoded_creds/JWKS.json", StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());
		} catch (IOException e) {
			throw error("Could not load client JWKS", e);
		}
	}
}
