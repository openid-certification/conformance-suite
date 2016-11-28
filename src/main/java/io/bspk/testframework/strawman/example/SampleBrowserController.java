/*******************************************************************************
 * Copyright 2016 The MITRE Corporation
 *   and the MIT Internet Trust Consortium
 *
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

package io.bspk.testframework.strawman.example;

import io.bspk.testframework.strawman.frontChannel.BrowserControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jricher
 *
 */
public class SampleBrowserController implements BrowserControl {

	private static Logger logger = LoggerFactory.getLogger(SampleBrowserController.class);
	

	@Override
	public void goToUrl(String url) {
		
		logger.info("Browser going to: " + url);

	}


	/* (non-Javadoc)
	 * @see io.bspk.testframework.strawman.frontChannel.BrowserControl#urlVisited(java.lang.String)
	 */
	@Override
	public void urlVisited(String url) {
	
		logger.info("Browser went to: " + url);

	}

}
