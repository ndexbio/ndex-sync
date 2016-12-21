/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.ndexbio.model.exceptions.NdexException;

import com.fasterxml.jackson.core.JsonProcessingException;

public class Copier {
	private final static Logger LOGGER = Logger.getLogger(Copier.class.getName());


    private List<CopyPlan> plans = new ArrayList<>();
    
    
    public void runPlans(String directoryString) throws JsonProcessingException, IOException, NdexException{
    	
    	LOGGER.info("Starting Copy Session");
    	if (readCopyPlans(directoryString)){
    		processCopyPlans();
    	}
    	LOGGER.info("Finishing Copy Session");
    	
    }
    
    
    // Read plans from ndex-copy-plans directory
	private boolean readCopyPlans(String directoryString){

		//LOGGER.info("Reading Copy Plans");
		//String currentDirectory = System.getProperty("user.dir");
		//LOGGER.info("Current directory for NDEx Copier is: " + currentDirectory);
		//String copyPlanDirectory = currentDirectory + "/ndex-copy-plans";
		//LOGGER.info("Therefore expecting copy plans in: " + copyPlanDirectory);
		try {
			File cpd = new File(directoryString);
			
			String copyPlanDirectory = cpd.getCanonicalPath();
			
			LOGGER.info("Reading Copy Plans in: " + copyPlanDirectory);
			
			CopyPlanReader cpr = new CopyPlanReader(copyPlanDirectory);
			plans = cpr.getCopyPlans();
			LOGGER.info("Found " + plans.size() + " copy plans");
			return true;
		} catch (Exception e) {
			LOGGER.severe("Error attempting to read copyplan files from directory " + directoryString);
			e.printStackTrace();
		}
		return false;
		
	}
	
	// 
	private void processCopyPlans() throws JsonProcessingException, IOException, NdexException{
		System.out.println("Processing Copy Plans");
		for (CopyPlan plan : this.plans){
			
				LOGGER.info("Processing copyPlan: " + plan.getPlanFileName());
				LOGGER.info("  Source: " + plan.getSource().getRoute() + "  username: " + plan.getSource().getUsername());
				LOGGER.info("  Target: " + plan.getTarget().getRoute() + "  username: " + plan.getTarget().getUsername());
				
				plan.process();
				
		}	
	}

}
