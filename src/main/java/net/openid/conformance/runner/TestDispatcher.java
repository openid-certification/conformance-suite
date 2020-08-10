package net.openid.conformance.runner;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.EventLog;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestInterruptedException;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.testmodule.UserFacing;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class TestDispatcher implements DataUtils {

	private static final Logger logger = LoggerFactory.getLogger(TestDispatcher.class);

	public static final String TEST_PATH = "/test/"; // path for incoming test requests
	public static final String TEST_MTLS_PATH = "/test-mtls/"; // path for incoming MTLS requests

	@Autowired
	private TestRunnerSupport support;

	@Autowired
	private EventLog eventLog;

	/**
	 * Dispatch a request to a running test. This came in on the /test/ URL either as /test/test-id-string or /test/a/test-alias.
	 * This requests may or may not be user-facing so we don't assume anything about the response.
	 */
	@RequestMapping({TEST_PATH + "**", TEST_MTLS_PATH + "**"})
	public Object handle(
		HttpServletRequest req, HttpServletResponse res,
		HttpSession session,
		@RequestHeader MultiValueMap<String, String> headers,
		@RequestBody(required = false) String body,
		@RequestHeader(name = "Content-type", required = false) MediaType contentType) {

		/*
		 * We have to parse the path by hand so that we can match the substrings that apply
		 * to the test itself and also pull out the query parameters to be passed on to
		 * the underlying handler functions.
		 */

		String path = (String) req.getAttribute(
			HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String bestMatchPattern = (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

		AntPathMatcher apm = new AntPathMatcher();
		String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);

		Iterator<String> pathParts = Splitter.on("/").split(finalPath).iterator();

		String testId = pathParts.next(); // used to route to the right test

		if (testId.equals("a")) {
			// it's an aliased test, look it up
			String alias = pathParts.next();
			if (!support.hasAlias(alias)) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			testId = support.getTestIdForAlias(alias);
		}

		if (!support.hasTestId(testId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		TestModule test = support.getRunningTestById(testId);

		if (test == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		try {

			// wrap up all the rest of the path as a string again, stripping off the initial bits
			String restOfPath = Joiner.on("/").join(pathParts);

			// convert the parameters and headers into a JSON object to make it easier for the test modules to ingest
			JsonObject requestParts = new JsonObject();
			requestParts.add("headers", mapToJsonObject(headers, true)); // do lowercase headers
			requestParts.add("query_string_params", mapToJsonObject(convertQueryStringParamsToMap(req.getQueryString()), false));
			requestParts.addProperty("method", req.getMethod().toUpperCase()); // method is always uppercase

			if (body != null) {
				requestParts.addProperty("body", body);

				// check the content type and try to parse it if it's JSON
				if (contentType != null) {
					if (contentType.equalsTypeAndSubtype(MediaType.APPLICATION_JSON)) {
						// parse the body as json
						requestParts.add("body_json", new JsonParser().parse(body));
					}

					if (contentType.equalsTypeAndSubtype(MediaType.APPLICATION_FORM_URLENCODED)) {
						requestParts.add("body_form_params", mapToJsonObject(convertQueryStringParamsToMap(body), false));
					}
				}
			}

			Object response;
			logIncomingHttpRequest(test, restOfPath, requestParts);

			if (TestModule.Status.CREATED.equals(test.getStatus())) {
				throw new TestFailureException(test.getId(), "Please wait for the test to be in WAITING state. The current status is CREATED");
			}

			try {
				if (path.startsWith(TEST_PATH)) {
					response = test.handleHttp(restOfPath, req, res, session, requestParts);
				} else if (path.startsWith(TEST_MTLS_PATH)) {
					response = test.handleHttpMtls(restOfPath, req, res, session, requestParts);
				} else {
					throw new TestFailureException(test.getId(), "Failure to route to path " + path);
				}

				test.checkLockReleased();
			} finally {
				// release the lock, so other threads can still run
				test.forceReleaseLock();
			}

			logOutgoingHttpResponse(test, restOfPath, response);

			return response;

		} catch (TestInterruptedException e) {
			if (e.getTestId() == null || !e.getTestId().equals(testId)) {
				throw new TestFailureException(testId, "A TestInterruptedException has been caught that does not contain the test id for the current test, this is a bug in the test module", e);
			}
			throw e;
		} catch (ConditionError e) {
			// we deliberately don't pass 'e' as the cause here, as doing so would make other parts of the
			// suite believe log messages had already been added for this failure.
			// see https://gitlab.com/openid/conformance-suite/issues/443
			throw new TestFailureException(testId, "A ConditionError has been incorrectly thrown by a TestModule, this is a bug in the test module: " + e.getMessage());
		} catch (Exception e) {
			throw new TestFailureException(test.getId(), e);
		}
	}

	/**
	 * Handle /.well-known/webfinger requests
	 * We follow the same pattern as the python suite as described at https://openid.net/certification/rp_testing/:
	 *     If your RP supports WebFinger, you can look up the issuer for a test using either of these issuer identifier syntaxes:
	 *       acct:RP_ID.TEST_ID@rp.certification.openid.net:8080
	 *       https://rp.certification.openid.net:8080/RP_ID/TEST_ID
	 *     ...
	 * We expect the following format:
	 *   acct:alias.testname@wedonotcheckthehostname:port
	 *   https://wedontcheckthehostname:port/alias/testname
	 */
	@GetMapping(value = {"/.well-known/webfinger"})
	public Object handleWellKnownWebFingerRequest(
		HttpServletRequest servletRequest, HttpServletResponse servletResponse,
		HttpSession httpSession,
		@RequestHeader MultiValueMap<String, String> headers,
		@RequestParam(required = false) String resource,
		@RequestParam(required = false) String rel) {

		//resource
		if(resource==null) {
			//https://tools.ietf.org/html/rfc7033#section-4
			//If the "resource" parameter is absent or malformed, the WebFinger
			//   resource MUST indicate that the request is bad as per Section 10.4.1
			//   of RFC 2616 [2].
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		String testName = null;
		String alias = null;
		boolean requestUsingAcctPrefix = false;
		boolean requestUsingHttpsPrefix = false;
		try {
			//using Pattern.CASE_INSENSITIVE as these are URIs and technically case insensitive
			//TODO Joseph: aliases are case sensitive and may theoretically contain . chars which would break this
			Pattern acctPattern = Pattern.compile("^acct:([a-zA-Z0-9_-]+)\\.([a-zA-Z0-9_-]+)@.*$", Pattern.CASE_INSENSITIVE);
			Matcher acctMatcher = acctPattern.matcher(resource);
			if(acctMatcher.matches()) {
				requestUsingAcctPrefix = true;
				alias = acctMatcher.group(1);
				testName = acctMatcher.group(2);
			} else {
				Pattern httpsPattern = Pattern.compile("^https?://.*/([a-zA-Z0-9_-]+)/([a-zA-Z0-9_-]+)$", Pattern.CASE_INSENSITIVE);
				Matcher httpsMatcher = httpsPattern.matcher(resource);
				if (httpsMatcher.matches()) {
					requestUsingHttpsPrefix = true;
					alias = httpsMatcher.group(1);
					testName = httpsMatcher.group(2);
				} else {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}
			}
		} catch(IllegalStateException | IndexOutOfBoundsException ex) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (!support.hasAlias(alias)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		String testId = support.getTestIdForAlias(alias);

		if (!support.hasTestId(testId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		TestModule test = support.getRunningTestById(testId);

		if (test == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		if (!test.getName().equals(testName)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if("oidcc-client-test-discovery-webfinger-acct".equals(testName) && !requestUsingAcctPrefix) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if("oidcc-client-test-discovery-webfinger-url".equals(testName) && !requestUsingHttpsPrefix) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		try {
			// convert the parameters and headers into a JSON object to make it easier for the test modules to ingest
			JsonObject requestParts = new JsonObject();
			requestParts.add("headers", mapToJsonObject(headers, true)); // do lowercase headers
			requestParts.add("query_string_params", mapToJsonObject(convertQueryStringParamsToMap(servletRequest.getQueryString()), false));
			requestParts.addProperty("method", servletRequest.getMethod().toUpperCase()); // method is always uppercase

			logIncomingHttpRequest(test, "/.well-known/webfinger", requestParts);

			if (TestModule.Status.CREATED.equals(test.getStatus())) {
				throw new TestFailureException(test.getId(), "Please wait for the test to be in WAITING state. The current status is CREATED");
			}

			JsonObject response = new JsonObject();
			response.addProperty("subject", resource);
			JsonArray linksArray = new JsonArray();
			JsonObject linkEntry = new JsonObject();
			linkEntry.addProperty("rel", "http://openid.net/specs/connect/1.0/issuer");
			linkEntry.addProperty("href", test.getExposedValues().get("issuer"));
			linksArray.add(linkEntry);
			response.add("links", linksArray);
			logOutgoingHttpResponse(test, "/.well-known/webfinger", response);

			return new ResponseEntity<>(response, HttpStatus.OK);

		} catch (Exception e) {
			throw new TestFailureException(test.getId(), e);
		}
	}

	protected MultiValueMap<String, String> convertQueryStringParamsToMap(String queryString) {
		List<NameValuePair> parameters = URLEncodedUtils.parse(queryString, Charset.defaultCharset());
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

		for (NameValuePair pair : parameters) {
			queryParams.add(pair.getName(), pair.getValue());
		}
		return queryParams;
	}

	protected void logIncomingHttpRequest(TestModule test, String path, JsonObject requestParts) {
		eventLog.log(test.getId(), test.getName(), test.getOwner(), args(
			"msg", "Incoming HTTP request to test instance " + test.getId(),
			"http", "incoming",
			"incoming_path", path,
			"incoming_query_string_params", requestParts.get("query_string_params"),
			"incoming_body_form_params", requestParts.get("body_form_params"),
			"incoming_method", requestParts.get("method"),
			"incoming_headers", requestParts.get("headers"),
			"incoming_body", requestParts.get("body"),
			"incoming_body_json", requestParts.get("body_json")));
	}

	protected void logOutgoingHttpResponse(TestModule test, String path, Object response) {
		if (response instanceof ResponseEntity) {
			ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
			eventLog.log(test.getId(), test.getName(), test.getOwner(), args(
				"msg", "Response to HTTP request to test instance " + test.getId(),
				"http", "outgoing",
				"outgoing_path", path,
				"outgoing_status_code", responseEntity.getStatusCodeValue(),
				"outgoing_headers", responseEntity.getHeaders(),
				"outgoing_body", responseEntity.getBody()));
		} else {
			// ModelAndView or other cases; just log 'toString'
			eventLog.log(test.getId(), test.getName(), test.getOwner(), args(
				"msg", "Response to HTTP request to test instance " + test.getId(),
				"http", "outgoing",
				"outgoing_path", path,
				"outgoing", response.toString()));
		}
	}

	private boolean exceptionCameFromUserFacingMethod(TestInterruptedException error) {
		Throwable throwable = error.getCause();
		if (throwable == null) {
			throwable = error;
		}
		for (StackTraceElement ste : throwable.getStackTrace()) {
			// look for the user-facing annotation in the stack
			Class<?> clz = null;
			try {
				clz = Class.forName(ste.getClassName());
			} catch (ClassNotFoundException e) {
				logger.error("Unable to find class when parsing exception stack trace: " + e.toString(), error);
				continue;
			}

			if (clz.equals(getClass())) {
				// stop if we hit the dispatcher, no need to go further up the stack
				break;
			}

			// check only the TestModule classes
			if (!clz.equals(AbstractTestModule.class) && TestModule.class.isAssignableFrom(clz)) {
				for (Method m : clz.getDeclaredMethods()) {
					if (m.getName().equals(ste.getMethodName()) && m.isAnnotationPresent(UserFacing.class)) {
						// if this is user-facing, return a user-facing view
						return true;
					}

				}
			}
		}
		return false;
	}

	// handle errors thrown by running tests
	@ExceptionHandler(TestInterruptedException.class)
	public Object testFailure(TestInterruptedException error) {
		try {
			ResponseEntity<Object> response = TestRunner.handleTestInterruptedException(error, support,
				"TestDispatcher.java exception handler");

			if (exceptionCameFromUserFacingMethod(error)) {
				return new RedirectView("/log-detail.html?log=" + error.getTestId());
			}

			// otherwise the plain API view (no HTML)
			return response;
		} catch (Exception e) {
			logger.error("Something terrible happened when handling an error, I give up", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
