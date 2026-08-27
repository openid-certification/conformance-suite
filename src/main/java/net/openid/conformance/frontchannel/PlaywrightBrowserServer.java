package net.openid.conformance.frontchannel;

import com.google.gson.JsonObject;
import com.microsoft.playwright.impl.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One browser per JVM that the {@link PlaywrightBrowserRunner}s share, instead of each launching
 * (and shutting down) a browser of its own for every scripted visit.
 *
 * <p>Playwright objects must only be used from the thread that created them, and the runners each
 * have a thread of their own with calls that block (a click waits for its navigation, a wait for
 * its element), so a browser object can't simply be shared between them. What Playwright offers
 * instead is its browser server: one process owns the browser and any number of clients connect to
 * it with {@code BrowserType.connect()}, each getting {@code BrowserContext}s of their own, isolated
 * from the others' just like the contexts of a browser launched locally. The Java binding has no
 * {@code launchServer()} (Node only), so the server is the CLI's {@code launch-server} command, run
 * from the driver bundled with the Java binding, with the {@code launchServer} options in a JSON
 * config file. The command is not in the CLI's documentation but has been there for years and is
 * what the Playwright maintainers point the non-Node bindings at (playwright-java issue 1572);
 * should it ever go away, the runners fall back to launching a browser each, and
 * {@code -Dbrowser.playwright.sharedBrowser=false} turns the server off altogether.
 *
 * <p>The server is started on first use, or at server startup by {@link PlaywrightBrowserInstaller},
 * and started again if it has died. It is stopped when the JVM exits.
 */
final class PlaywrightBrowserServer {

	private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserServer.class);

	/** How long the server gets to launch the browser and print its endpoint. */
	private static final long START_TIMEOUT_MILLIS = 60_000;
	private static final Pattern ENDPOINT = Pattern.compile("ws://\\S+");

	private static Process process;
	private static String endpoint;
	/** The settings the running server was started with; a change of browser type etc. restarts it. */
	private static String startedFor;
	private static boolean shutdownHookInstalled;

	private PlaywrightBrowserServer() {
	}

	/**
	 * The websocket endpoint of the shared browser server for these settings, starting the server
	 * if it isn't running.
	 *
	 * @throws IllegalStateException if the server can't be started
	 */
	static synchronized String endpoint(PlaywrightBrowserRunner.Settings settings) {
		String key = settings.browserType() + "/headless=" + settings.headless() + "/slowMo=" + settings.slowMo();
		if (process != null && process.isAlive() && key.equals(startedFor)) {
			return endpoint;
		}
		stop();
		start(settings, key);
		return endpoint;
	}

	static synchronized boolean isRunning() {
		return process != null && process.isAlive();
	}

	private static void start(PlaywrightBrowserRunner.Settings settings, String key) {
		PlaywrightBrowserRunner.ensureBrowserInstalled(settings.browserType());
		long start = System.currentTimeMillis();
		logger.info("Starting the shared Playwright " + settings.browserType() + " browser server");
		Path config = null;
		Process started = null;
		try {
			// the launchServer options, see https://playwright.dev/docs/api/class-browsertype#browser-type-launch-server
			JsonObject options = new JsonObject();
			options.addProperty("headless", settings.headless());
			options.addProperty("slowMo", settings.slowMo());
			config = Files.createTempFile("playwright-browser-server-", ".json");
			Files.writeString(config, options.toString(), StandardCharsets.UTF_8);

			ProcessBuilder processBuilder = Driver.ensureDriverInstalled(Collections.emptyMap(), false).createProcessBuilder();
			processBuilder.command().addAll(List.of("launch-server", "--browser", settings.browserType(), "--config", config.toString()));
			processBuilder.redirectErrorStream(true);
			started = processBuilder.start();

			// the server prints its endpoint as its first line; the reader thread keeps draining
			// (and logging) its output for as long as it runs
			CompletableFuture<String> printedEndpoint = new CompletableFuture<>();
			Process reading = started;
			Thread reader = new Thread(() -> drainOutput(reading, printedEndpoint), "playwright-browser-server-output");
			reader.setDaemon(true);
			reader.start();

			try {
				endpoint = printedEndpoint.get(START_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				throw new IllegalStateException("The Playwright browser server did not print its endpoint within " + START_TIMEOUT_MILLIS + "ms");
			} catch (ExecutionException e) {
				throw new IllegalStateException("The Playwright browser server exited before printing its endpoint", e.getCause());
			}
			process = started;
			startedFor = key;
			installShutdownHook();
			logger.info("Shared Playwright " + settings.browserType() + " browser server is up at " + endpoint
				+ " (took " + (System.currentTimeMillis() - start) + "ms)");
		} catch (IOException e) {
			destroy(started);
			throw new IllegalStateException("Failed to start the Playwright browser server", e);
		} catch (InterruptedException e) {
			destroy(started);
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while starting the Playwright browser server", e);
		} catch (RuntimeException e) {
			destroy(started);
			throw e;
		} finally {
			if (config != null) {
				try {
					Files.deleteIfExists(config);
				} catch (IOException e) {
					logger.debug("Could not delete " + config, e);
				}
			}
		}
	}

	private static void drainOutput(Process process, CompletableFuture<String> printedEndpoint) {
		try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			for (String line = output.readLine(); line != null; line = output.readLine()) {
				Matcher matcher = ENDPOINT.matcher(line);
				if (!printedEndpoint.isDone() && matcher.find()) {
					printedEndpoint.complete(matcher.group());
				} else {
					logger.info("playwright launch-server: " + line);
				}
			}
		} catch (IOException e) {
			logger.debug("Reading the Playwright browser server's output failed", e);
		}
		int exitCode;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		printedEndpoint.completeExceptionally(new IllegalStateException("playwright launch-server exited with code " + exitCode));
		logger.warn("The shared Playwright browser server exited with code " + exitCode
			+ (exitCode == 0 ? "" : "; it is started again when next needed"));
	}

	private static synchronized void installShutdownHook() {
		if (shutdownHookInstalled) {
			return;
		}
		Runtime.getRuntime().addShutdownHook(new Thread(PlaywrightBrowserServer::stop, "playwright-browser-server-shutdown"));
		shutdownHookInstalled = true;
	}

	/**
	 * Stops the server (and with it the browser) if it is running.
	 */
	static synchronized void stop() {
		if (process != null) {
			if (process.isAlive()) {
				logger.info("Stopping the shared Playwright browser server");
			}
			destroy(process);
		}
		process = null;
		endpoint = null;
		startedFor = null;
	}

	private static void destroy(Process process) {
		if (process == null) {
			return;
		}
		process.descendants().forEach(ProcessHandle::destroy);
		process.destroy();
		try {
			if (!process.waitFor(5, TimeUnit.SECONDS)) {
				process.descendants().forEach(ProcessHandle::destroyForcibly);
				process.destroyForcibly();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
		}
	}
}
