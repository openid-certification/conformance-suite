package net.openid.conformance.openbanking_brasil.testmodules.support;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OverrideClientWithOldBrcacClient extends AbstractOverrideClient {
	@Override
	String clientCert() {
		try {
			return IOUtils.resourceToString("dcr_brcac2022_hardcoded_creds/brcac_old.pem", StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());
		} catch (IOException e) {
			throw error("Could not load old BRCAC certificate", e);
		}
	}

	@Override
	String clientKey() {
		try {
			return IOUtils.resourceToString("dcr_brcac2022_hardcoded_creds/brcac_old.key", StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader());
		} catch (IOException e) {
			throw error("Could not load old BRCAC key", e);

		}
	}

	@Override
	String role() {
		return "Old BRCAC client";
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
