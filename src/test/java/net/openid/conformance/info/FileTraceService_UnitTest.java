package net.openid.conformance.info;

import net.openid.conformance.frontchannel.BrowserSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FileTraceService_UnitTest {

	@TempDir
	Path tracesDir;

	private FileTraceService service(String dir) {
		return new FileTraceService(new BrowserSettings(name -> "browser.playwright.tracesDir".equals(name) ? dir : null));
	}

	private Path trace(String testId, int visit, String content) throws IOException {
		Path trace = BrowserSettings.traceFile(tracesDir.toString(), testId, visit);
		Files.createDirectories(trace.getParent());
		return Files.writeString(trace, content, StandardCharsets.UTF_8);
	}

	@Test
	public void returnsTheTraceOfEachVisit() throws IOException {
		trace("abc123", 1, "first");
		trace("abc123", 2, "second");

		FileTraceService service = service(tracesDir.toString());

		assertThat(service.getTrace("abc123", 1)).hasValue("first".getBytes(StandardCharsets.UTF_8));
		assertThat(service.getTrace("abc123", 2)).hasValue("second".getBytes(StandardCharsets.UTF_8));
		assertThat(service.getTrace("abc123", 3)).isEmpty();
		assertThat(service.getTrace("abc123", 0)).isEmpty();
		assertThat(service.getTrace("abc123", -1)).isEmpty();
	}

	@Test
	public void listsTheTracesOfATestByVisit() throws IOException {
		trace("abc123", 10, "tenth");
		trace("abc123", 2, "second!");
		trace("def456", 1, "other test");
		// not traces: left alone and not listed
		Files.writeString(tracesDir.resolve("abc123").resolve("notes.txt"), "x", StandardCharsets.UTF_8);
		Files.writeString(tracesDir.resolve("abc123").resolve("0.zip"), "x", StandardCharsets.UTF_8);

		assertThat(service(tracesDir.toString()).listTraces("abc123"))
			.containsExactly(new TraceService.TraceInfo(2, 7), new TraceService.TraceInfo(10, 5));
	}

	@Test
	public void emptyWhenNoTraceRecorded() {
		assertThat(service(tracesDir.toString()).getTrace("abc123", 1)).isEmpty();
		assertThat(service(tracesDir.toString()).listTraces("abc123")).isEmpty();
	}

	@Test
	public void emptyWhenTracesDirNotConfigured() throws IOException {
		trace("abc123", 1, "trace");

		assertThat(service("").getTrace("abc123", 1)).isEmpty();
		assertThat(service(null).getTrace("abc123", 1)).isEmpty();
		assertThat(service("").listTraces("abc123")).isEmpty();
		assertThat(service(null).listTraces("abc123")).isEmpty();
	}

	@Test
	public void refusesTestIdsThatAreNotPlainAlphanumeric() throws IOException {
		// a trace one level above the traces directory, where a crafted id would have to reach
		Path secret = Files.createDirectories(tracesDir.resolve("secret")).resolve("1.zip");
		Files.writeString(secret, "secret", StandardCharsets.UTF_8);
		Path sub = Files.createDirectory(tracesDir.resolve("sub"));

		FileTraceService service = service(sub.toString());
		for (String crafted : new String[] {"../secret", "..\\secret", "/" + secret.getParent(), "abc/../../secret", "abc%2F..%2Fsecret", "secret\0", ".", "..", "", null}) {
			assertThat(service.getTrace(crafted, 1)).as("read with id %s", crafted).isEmpty();
			assertThat(service.listTraces(crafted)).as("list with id %s", crafted).isEmpty();
			service.deleteTracesForTestId(crafted);
			assertThat(secret).as("after delete with id %s", crafted).exists();
			assertThat(sub).as("traces directory after delete with id %s", crafted).exists();
		}
	}

	@Test
	public void deletesEveryTraceOfATest() throws IOException {
		Path first = trace("abc123", 1, "first");
		Path second = trace("abc123", 2, "second");
		Path other = trace("def456", 1, "other");

		service(tracesDir.toString()).deleteTracesForTestId("abc123");

		assertThat(first).doesNotExist();
		assertThat(second).doesNotExist();
		assertThat(tracesDir.resolve("abc123")).doesNotExist();
		assertThat(other).exists();
	}

	@Test
	public void deleteIsANoOpWithoutATraceOrTracesDir() throws IOException {
		service(tracesDir.toString()).deleteTracesForTestId("abc123"); // nothing recorded

		Path trace = trace("abc123", 1, "trace");
		service("").deleteTracesForTestId("abc123");
		service(null).deleteTracesForTestId("abc123");
		assertThat(trace).exists();
	}
}
