/** *****************************************************************************
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
 ****************************************************************************** */
package io.fintechlabs.testframework.testmodule;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.logging.EventLog;

/**
 * @author jricher
 *
 */
public interface TestModule {

    public static enum Status {
        CREATED, // test has been instantiated 
        CONFIGURED, // configuration files have been sent and set up
        RUNNING, // test is executing
        WAITING, // test is waiting for external input
        FINISHED, // test is no longer running
        UNKNOWN // test status is unknown, probably an error
    }
    
    public static enum Result {
    	PASSED,  // test has passed successfully
    	FAILED,  // test has failed
    	WARNING, // test has warnings
    	UNKNOWN  // test results not yet known, probably still running (see status)
    }

    /**
     * *
     * Method is called to pass configuration parameters
     *
     * @param config A JSON object consisting of details that the testRunner
     * doesn't need to know about
     * @param eventLog The event Logging object to log details of the test
     * @param id The id of this test
     * @param browser The "front channel" control
     * @param baseUrl The base of the URL that will need to be appended to any
     * URL construction.
     */
    void configure(JsonObject config, EventLog eventLog, String id, BrowserControl browser, String baseUrl);

    /**
     * *
     *
     * @return The name of the test.
     */
    String getName();

    /**
     * @return the id of this test
     */
    String getId();

    /**
     * @return The current status of the test
     */
    Status getStatus();

    /**
     * Called by the TestRunner to start the test
     */
    void start();

    /**
     * Called by the test runner to stop the test
     */
    void stop();

    /**
     * Called when a the test runner calls a URL
     *
     * @param path The path that was called
     * @param req The request that passed to the server
     * @param res A response that will be sent from the server
     * @param session Session details
     * @param requestParts elements from the request parsed out into a json object for use in condition classes
     * @return A response (could be a ModelAndview, ResponseEntity, or other item)
     */
    Object handleHttp(String path,
            HttpServletRequest req, HttpServletResponse res,
            HttpSession session,
            JsonObject requestParts);

	/**
	 * @return get the test results
	 */
	Result getResult();
	
	/**
	 * @return a map of runtime values exposed by the test itself, potentially useful for configuration
	 * of external entities.
	 */
	Map<String, String> getExposedValues();

	/**
	 * @return the associated browser control module
	 */
	BrowserControl getBrowser();

}
