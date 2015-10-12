/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Copier {
	private final static Logger LOGGER = Logger.getLogger(Copier.class.getName());


    private List<CopyPlan> plans = new ArrayList<>();
    
    
    public void runPlans(String directoryString, Boolean cxMode){
    	
    	LOGGER.info("Starting Copy Session");
    	if (readCopyPlans(directoryString)){
    		processCopyPlans(cxMode);
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
	private void processCopyPlans(Boolean cxMode){
		System.out.println("Processing Copy Plans");
		for (CopyPlan plan : this.plans){
			try {
			
				LOGGER.info("Processing copyPlan: " + plan.getPlanFileName());
				LOGGER.info("  Source: " + plan.getSource().getRoute() + "  username: " + plan.getSource().getUsername());
				LOGGER.info("  Target: " + plan.getTarget().getRoute() + "  username: " + plan.getTarget().getUsername());
				
				plan.process(cxMode);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
			e.printStackTrace();
			}
		}	
	}
    /*
    public void testCopyBELNetworkInBlocks() throws IllegalArgumentException, Exception
    {
    	int edgesPerBlock = 100;
    	int nodesPerBlock = 100;
        try
        {
            List<Network> networks = mal.findNetworks("BEL Framework Three Citation Corpus Document");
            
            Network network = networks.get(0);
            
            if (null != network)
            	copyNetworkInBlocks(network, edgesPerBlock, nodesPerBlock);
            
            // Get the target network stats
            
            // Stats should be equal
        
        }
        catch (Exception e)
        {

            e.printStackTrace();
        }
    }
    
	public void testGet(){
		try {
            List<Network> networks = mal.findNetworks("BEL Framework Three Citation Corpus Document");
            
            Network network = networks.get(0);
            
			String route = "/networks/" + network.getExternalId() + "/edges/" + 0 + "/" + 10; 
			JsonNode result = client.get(route, "");
			System.out.println("Network as JSON: " + result.get("name").asText());
			
			Network currentSubnetwork = mal.getEdges(network.getExternalId().toString(), 0, 10);
			System.out.println("Network as Object Model: " + currentSubnetwork.getName());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    public void copyNetworkInBlocks(Network network, int edgesPerBlock, int nodesPerBlock) throws IllegalArgumentException, Exception{
    	Network currentSubnetwork = null;
    	
    	int skipBlocks = 0;
    	
    	// Get the first block of edges from the source network
    	System.out.println("Getting " + edgesPerBlock + " at offset " + skipBlocks);
    	currentSubnetwork = mal.getEdges(network.getExternalId().toString(), skipBlocks, edgesPerBlock);
    	
    	currentSubnetwork.setName(currentSubnetwork.getName() + " - copy " + Math.random());
    	currentSubnetwork.setMembers(null);
    	
    	//ObjectModelTools.summarizeNetwork(currentSubnetwork);
    	
    	// Create the target network
    	System.out.println("Creating network with " + currentSubnetwork.getEdgeCount()  + " edges");
    	Network targetNetwork = mal.createNetwork(currentSubnetwork);

    	String targetNetworkId = targetNetwork.getExternalId().toString();
    	
 
    	// Loop getting subnetworks by edges until the returned subnetwork has no edges
    	do { 
    		skipBlocks++;
    		System.out.println("Getting " + edgesPerBlock + " at offset " + skipBlocks);
    		currentSubnetwork = mal.getEdges(network.getExternalId().toString(), skipBlocks, edgesPerBlock);
    		// Add the subnetwork to the target
    		System.out.println("Adding " + currentSubnetwork.getEdgeCount()  + " edges to network " + targetNetworkId);
    		//ObjectModelTools.summarizeNetwork(currentSubnetwork);
    		if (currentSubnetwork.getEdgeCount() > 0) 
    			mal.addNetwork(targetNetworkId, "JDEX_ID", currentSubnetwork);
    	} while (currentSubnetwork.getEdgeCount() > 0);
    	
    	skipBlocks = -1;
    	// Loop getting subnetworks by nodes not in edges until the returned subnetwork has no more nodes
    	do { 
    		skipBlocks++;
    		System.out.println("Getting " + nodesPerBlock + " at offset " + skipBlocks);
    		currentSubnetwork = mal.getNetworkByNonEdgeNodes(network.getExternalId().toString(), skipBlocks, nodesPerBlock);
    		// Add the subnetwork to the target
    		System.out.println("Adding " + currentSubnetwork.getNodeCount()  + " nodes to network " + targetNetworkId);
    		//ObjectModelTools.summarizeNetwork(currentSubnetwork);
    		if (currentSubnetwork.getNodeCount() > 0) 
    			mal.addNetwork(targetNetworkId, "JDEX_ID", currentSubnetwork);
    	} while (currentSubnetwork.getNodeCount() > 0);
    			
    }
*/


	public void setMode(String string) {
		// TODO Auto-generated method stub
		
	}


}
