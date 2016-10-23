/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Bespoke.Heart.Test;

import Bespoke.Heart.Runner.RunnerCallBack;
import org.slf4j.Logger;

/**
 *
 * @author tlewis
 */
public interface TestCaseInterface {

    /***
     * 
     * @return The name of a given test
     */
    String getName();
    
   /***
    * Called by the runner to send the setup Event
    */
    void testSetup();
    
    /***
     * Called by the runner to run the test
     */
    void testRun();
    
    /***
     * Called by the runner to stop the test.
     */
    void testStop();
    /***
     * Configures the test case, this is called before anything else
     * @param config Configuration object. TODO probably a json blob
     * @param log The log that can be used to report/save results 
     * @param rcb The call back object to handle the test
     */
    public  void configureTestCase(Object config, Logger log, RunnerCallBack rcb);
    
}
