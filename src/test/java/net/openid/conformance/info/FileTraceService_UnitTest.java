package net.openid.conformance.info;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FileTraceService_UnitTest {

	@TempDir
	Path tracesDir;

	private FileTraceService service(String dir) {
		FileTraceService service = new FileTraceService();
		ReflectionTestUtils.setField(service, "tracesDir", dir);
		return service;
	}

	@Test
	public void returnsTraceForTestId() throws IOException {
		Files.writeString(tracesDir.resolve("abc123.zip"), "trace", StandardCharsets.UTF_8);

		assertThat(service(tracesDir.toString()).getTraceForTestId("abc123"))
			.hasValue("trace".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void emptyWhenNoTraceRecorded() {
		assertThat(service(tracesDir.toString()).getTraceForTestId("abc123")).isEmpty();
	}

	@Test
	public void emptyWhenTracesDirNotConfigured() throws IOException {
		Files.writeString(tracesDir.resolve("abc123.zip"), "trace", StandardCharsets.UTF_8);

		assertThat(service("").getTraceForTestId("abc123")).isEmpty();
		assertThat(service(null).getTraceForTestId("abc123")).isEmpty();
	}

	@Test
	public void refusesTestIdsThatAreNotPlainAlphanumeric() throws IOException {
		Path secret = tracesDir.resolve("secret.zip");
		Files.writeString(secret, "secret", StandardCharsets.UTF_8);
		Path sub = Files.createDirectory(tracesDir.resolve("sub"));

		FileTraceService service = service(sub.toString());
		for (String crafted : new String[] {"../secret", "..\\secret", "/" + secret, "abc/../../secret", "abc%2F..%2Fsecret", "secret\0", "", null}) {
			assertThat(service.getTraceForTestId(crafted)).as("read with id %s", crafted).isEmpty();
			service.deleteTraceForTestId(crafted);
			assertThat(secret).as("after delete with id %s", crafted).exists();
		}
	}

	@Test
	public void deletesTraceForTestId() throws IOException {
		Path trace = tracesDir.resolve("abc123.zip");
		Files.writeString(trace, "trace", StandardCharsets.UTF_8);
		Path other = tracesDir.resolve("def456.zip");
		Files.writeString(other, "other", StandardCharsets.UTF_8);

		service(tracesDir.toString()).deleteTraceForTestId("abc123");

		assertThat(trace).doesNotExist();
		assertThat(other).exists();
	}

	@Test
	public void deleteIsANoOpWithoutATraceOrTracesDir() throws IOException {
		service(tracesDir.toString()).deleteTraceForTestId("abc123"); // nothing recorded

		Files.writeString(tracesDir.resolve("abc123.zip"), "trace", StandardCharsets.UTF_8);
		service("").deleteTraceForTestId("abc123");
		service(null).deleteTraceForTestId("abc123");
		assertThat(tracesDir.resolve("abc123.zip")).exists();
	}
}
