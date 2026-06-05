package net.openid.conformance.condition.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * In-process HTTP server fixture for unit tests that simulates an unresponsive endpoint: it accepts
 * the TCP connection (so connect succeeds) but never sends a complete HTTP response, forcing the
 * client's response/read timeout to fire. This reproduces the failure that hung the conformance
 * suite - see https://gitlab.com/openid/conformance-suite/-/work_items/1827
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@code ACCEPT_AND_HANG}: accept the connection and send nothing at all.</li>
 *   <li>{@code HEADERS_THEN_HANG}: send a status line and headers, then stall before the body. This
 *       defeats a per-read socket timeout if data keeps arriving, but is still caught by an overall
 *       response timeout.</li>
 * </ul>
 *
 * <p>Listens on the loopback address on an ephemeral port. {@link AutoCloseable}; server-side
 * exceptions are intentionally swallowed (tests assert on the client-side outcome only).
 */
public final class StalledHttpServer implements AutoCloseable {

	public enum Mode { ACCEPT_AND_HANG, HEADERS_THEN_HANG, SLOW_DRIP }

	// SLOW_DRIP sends one body byte at a time, slower than a single response cycle but well under any
	// reasonable per-read timeout, so only an overall wall-clock deadline can stop it.
	private static final long DRIP_INTERVAL_MILLIS = 1000;
	private static final int DRIP_BYTE_COUNT = 120;

	private final ServerSocket serverSocket;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final List<Socket> accepted = new ArrayList<>();
	private volatile boolean running = true;

	public StalledHttpServer(Mode mode) throws IOException {
		serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
		executor.submit(() -> acceptLoop(mode));
	}

	private void acceptLoop(Mode mode) {
		while (running) {
			final Socket socket;
			try {
				socket = serverSocket.accept();
			} catch (IOException e) {
				return; // server socket closed - we're shutting down
			}
			synchronized (accepted) {
				accepted.add(socket);
			}
			if (mode == Mode.HEADERS_THEN_HANG) {
				executor.submit(() -> {
					try {
						OutputStream out = socket.getOutputStream();
						// Status line + a Content-Length that we will never satisfy, then stall.
						out.write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n".getBytes(StandardCharsets.UTF_8));
						out.flush();
					} catch (IOException e) {
						// connection closed by the client (e.g. on timeout) - expected, nothing to do
					}
				});
			} else if (mode == Mode.SLOW_DRIP) {
				executor.submit(() -> dripSlowly(socket));
			}
			// ACCEPT_AND_HANG: leave the socket open with no response at all.
		}
	}

	private void dripSlowly(Socket socket) {
		try {
			OutputStream out = socket.getOutputStream();
			out.write("HTTP/1.1 200 OK\r\nContent-Length: 1000000\r\n\r\n".getBytes(StandardCharsets.UTF_8));
			out.flush();
			for (int i = 0; i < DRIP_BYTE_COUNT && running; i++) {
				Thread.sleep(DRIP_INTERVAL_MILLIS);
				out.write('x');
				out.flush();
			}
		} catch (IOException e) {
			// connection closed by the client (deadline abort) - expected
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public String getUrl() {
		return "http://" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort() + "/";
	}

	@Override
	public void close() {
		running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			// already closing - nothing to do
		}
		synchronized (accepted) {
			for (Socket s : accepted) {
				try {
					s.close();
				} catch (IOException e) {
					// best-effort cleanup
				}
			}
		}
		executor.shutdownNow();
		try {
			executor.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
