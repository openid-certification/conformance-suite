package net.openid.conformance.info;

import net.openid.conformance.security.AuthenticationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Service for retrieving Playwright trace files from the file system.
 */
@Service
public class FileTraceService implements TraceService {

	private static final Logger logger = LoggerFactory.getLogger(FileTraceService.class);

	@Value("${browser.playwright.tracesDir:}")
	private String tracesDir;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private TestInfoService testInfoService;

	@Override
	public Optional<byte[]> getTraceForTestId(String testId, boolean assumeAdmin) {
		if (tracesDir == null || tracesDir.isEmpty()) {
			logger.debug("tracesDir not configured");
			return Optional.empty();
		}

		// Apply access control if not admin
		if (!assumeAdmin && !authenticationFacade.isAdmin()) {
			var testOwner = testInfoService.getTestOwner(testId);
			if (testOwner == null || !authenticationFacade.getPrincipal().equals(testOwner)) {
				logger.debug("Access denied for trace, testId: {}", testId);
				return Optional.empty();
			}
		}

		Path tracePath = Path.of(tracesDir, testId + ".zip");

		if (!Files.exists(tracePath)) {
			logger.debug("No trace found for testId: {}", testId);
			return Optional.empty();
		}

		try {
			byte[] traceBytes = Files.readAllBytes(tracePath);
			logger.debug("Found trace for testId: {}, size: {} bytes", testId, traceBytes.length);
			return Optional.of(traceBytes);
		} catch (IOException e) {
			logger.error("Failed to read trace for testId: {}", testId, e);
			return Optional.empty();
		}
	}
}
