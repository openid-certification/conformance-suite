package net.openid.conformance.condition.util;


import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsServerCertificate;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class NoopTlsAuthentication implements TlsAuthentication {

	@Override
	public void notifyServerCertificate(TlsServerCertificate serverCertificate) throws IOException {

	}

	@Override
	public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
		return null;
	}
}
