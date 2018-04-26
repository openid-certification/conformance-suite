/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.runner;

import java.lang.reflect.Method;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.UserFacing;

/**
 * @author jricher
 *
 */
@Controller
public class TestDispatcher {

	private static Logger logger = LoggerFactory.getLogger(TestDispatcher.class);

	public static final String TEST_PATH = "/test/"; // path for incoming test requests
	public static final String TEST_MTLS_PATH = "/test-mtls/"; // path for incoming MTLS requests

	@Autowired
	private TestRunnerSupport support;

	/**
	 * Dispatch a request to a running test. This came in on the /test/ URL either as /test/test-id-string or /test/a/test-alias.
	 * This requests may or may not be user-facing so we don't assume anything about the response.
	 * 
	 * @param req
	 * @param res
	 * @param session
	 * @param params
	 * @param m
	 * @return
	 */
	@RequestMapping({ TEST_PATH + "**", TEST_MTLS_PATH + "**" })
	public Object handle(
		HttpServletRequest req, HttpServletResponse res,
		HttpSession session,
		@RequestParam MultiValueMap<String, String> params,
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

		// wrap up all the rest of the path as a string again, stripping off the initial bits
		String restOfPath = Joiner.on("/").join(pathParts);

		// convert the parameters and headers into a JSON object to make it easier for the test modules to ingest
		JsonObject requestParts = new JsonObject();
		requestParts.add("params", mapToJsonObject(params));
		requestParts.add("headers", mapToJsonObject(headers));
		requestParts.addProperty("method", req.getMethod());

		if (body != null) {
			requestParts.addProperty("body", body);

			// check the content type and try to parse it if it's JSON
			if (contentType != null) {
				if (contentType.equals(MediaType.APPLICATION_JSON)) {
					// parse the body as json
					requestParts.add("body_json", new JsonParser().parse(body));
				}
				// TODO: convert other data types?
			}
		}

		TestModule test = support.getRunningTestById(testId);
		if (test != null) {
			if (path.startsWith(TEST_PATH)) {
				return test.handleHttp(restOfPath, req, res, session, requestParts);
			} else if (path.startsWith(TEST_MTLS_PATH)) {
				return test.handleHttpMtls(restOfPath, req, res, session, requestParts);
			} else {
				throw new TestFailureException(test.getId(), "Failure to route to path " + path);
			}
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	// handle errors thrown by running tests
	@ExceptionHandler(TestFailureException.class)
	public Object testFailure(TestFailureException error) {
		try {
			TestModule test = support.getRunningTestById(error.getTestId());
			if (test != null) {
				logger.error("Caught an error while running the test, stopping the test: " + error.getMessage());
				test.stop();
			}
			test.setFinalError(error);

			for (StackTraceElement ste : error.getCause().getStackTrace()) {
				// look for the user-facing annotation in the stack
				Class<?> clz = Class.forName(ste.getClassName());

				if (clz.equals(getClass())) {
					// stop if we hit the dispatcher, no need to go further up the stack
					break;
				}

				// check only the TestModule classes
				if (!clz.equals(AbstractTestModule.class) && TestModule.class.isAssignableFrom(clz)) {
					for (Method m : clz.getDeclaredMethods()) {
						if (m.getName().equals(ste.getMethodName()) && m.isAnnotationPresent(UserFacing.class)) {
							// if this is user-facing, return a user-facing view
							//return new ModelAndView("testError", ImmutableMap.of("error", error));
							return new RedirectView("/log-detail.html?log=" + error.getTestId());
						}

					}
				}
			}

			// return a plain API view (no HTML)
			JsonObject obj = new JsonObject();
			obj.addProperty("error", error.getMessage());
			obj.addProperty("cause", error.getCause() != null ? error.getCause().getMessage() : null);
			obj.addProperty("testId", error.getTestId());
			return new ResponseEntity<>(obj, HttpStatus.INTERNAL_SERVER_ERROR);

		} catch (Exception e) {
			logger.error("Something terrible happened when handling an error, I give up", e);
			return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * utility function to convert an incoming multi-value map to a JSonObject for storage
	 * 
	 * @param params
	 * @return
	 */
	protected JsonObject mapToJsonObject(MultiValueMap<String, String> params) {
		JsonObject o = new JsonObject();
		for (String key : params.keySet()) {
			o.addProperty(key, params.getFirst(key));
		}
		return o;
	}

}
