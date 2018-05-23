package io.fintechlabs.testframework.condition.common;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;

class ProxyUtil {

	/**
	 * @param tlsTestHost The host that will be used to create the socket.
	 * @param tlsTestPort The port that will be used to create the socket.
	 * @return a newly created socket using the proxy is one is set.
	 * @throws IOException thrown if there is an issue with the socket connection.
	 */
	static Socket setupSocket(String tlsTestHost, Integer tlsTestPort) throws IOException {
		String proxyHost = System.getProperty("https.proxyHost", "");
		int proxyPort = Integer.parseInt(System.getProperty("https.proxyPort", "0"));
		Socket socket;
		if (isNotEmpty(proxyHost) && proxyPort != 0) {
			Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
			socket = new Socket(proxy);
			socket.connect(new InetSocketAddress(tlsTestHost, tlsTestPort));
		} else {
			socket = new Socket(InetAddress.getByName(tlsTestHost), tlsTestPort);
		}
		return socket;
	}
}
