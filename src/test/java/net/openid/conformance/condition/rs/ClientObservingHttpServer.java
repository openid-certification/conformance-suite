package net.openid.conformance.condition.rs;

import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientObservingHttpServer implements Runnable {

	private KeyStore keyStore;
	private KeyStore trustStore;
	private SSLServerSocket serverSocket;
	private CountDownLatch latch = new CountDownLatch(1);
	private Certificate[] clientCerts;

	public ClientObservingHttpServer(KeyStore serverKeystore, KeyStore trust) {
		this.keyStore = serverKeystore;
		this.trustStore = trust;
	}

	@Override
	public void run() {
		try {
			SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
			clientCerts = clientSocket.getSession().getPeerCertificates();
			clientSocket.close();
			latch.countDown();
		} catch (Exception e) {
			// not generally bothered by this, all we want are the client certs
		}
	}

	public int start() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
		SSLServerSocketFactory sslssf;
		if(trustStore == null) {
			sslssf = (SSLServerSocketFactory) SSLServerSocketFactory
				.getDefault();
		} else {
			TrustManagerFactory tmf = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, "changeit".toCharArray());
			SSLContext sslContext = SSLContexts.custom()
				.build();
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			sslssf = sslContext.getServerSocketFactory();
		}
		serverSocket = (SSLServerSocket) sslssf.createServerSocket(0);
		serverSocket.setEnabledProtocols(new String[] {"TLSv1.2"});
		serverSocket.setNeedClientAuth(true);
		new Thread(this).start();
		return serverSocket.getLocalPort();
	}

	public boolean await() throws InterruptedException {
		return latch.await(3, TimeUnit.SECONDS);
	}

	public void stop() throws IOException {
		if(serverSocket != null){
			serverSocket.close();
		}
	}

	public Certificate[] getClientCerts() {
		return clientCerts;
	}

}
