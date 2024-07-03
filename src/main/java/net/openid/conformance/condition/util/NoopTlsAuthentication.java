package net.openid.conformance.condition.util;


import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsCredentials;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class NoopTlsAuthentication implements TlsAuthentication {

	@Override
	public void notifyServerCertificate(Certificate serverCertificate) throws IOException {

	}

	@Override
	public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
		return null;
	}
}
