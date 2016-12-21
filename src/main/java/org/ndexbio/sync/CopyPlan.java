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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexProvenanceEventType;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.ProvenanceEvent;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.tools.PropertyHelpers;
import org.ndexbio.model.tools.ProvenanceHelpers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "planType")
@JsonSubTypes(value = { @Type(value = QueryCopyPlan.class, name = "QueryCopyPlan"), @Type(value = IdCopyPlan.class, name = "IdCopyPlan") })
public abstract class CopyPlan implements NdexProvenanceEventType {
	protected final static Logger LOGGER = Logger.getLogger(CopyPlan.class.getName());
	
	NdexServer source;
	NdexServer target;

	String targetGroupName;
	String planFileName;
	List<NetworkSummary> sourceNetworks;
	List<NetworkSummary> targetCandidates;
	Map<UUID, ProvenanceEntity> srcProvenanceMap;
	Map<UUID, ProvenanceEntity> tgtProvenanceMap;
	
	boolean updateTargetNetwork = false;
	boolean updateReadOnlyNetwork = false;	

	public void process() throws JsonProcessingException, IOException, NdexException {
		source.initialize();
		target.initialize();
		if ( !target.getVersion().equals("2.0"))
			throw new NdexException ("This version only supports NDEx version 2.0 server as the target.");
		srcProvenanceMap = new HashMap<>();
		tgtProvenanceMap = new HashMap<> ();
		findSourceNetworks();
		getAllSourceProvenance();
		findTargetCandidates();
		getAllTargetProvenance();
		
		if (updateTargetNetwork) {
			// update network(s) on the target server
		    for (NetworkSummary network: sourceNetworks) {
			    updateTargetNetwork(network);
		    }
		} else {
			// copy source network(s) from source server to target
		    for (NetworkSummary network: sourceNetworks) {
			    copySourceNetwork(network);
		    }
		}
	}


	// Find networks in target NDEx in the target account.
	//
	// In this version:
	//        the account is always the target user account.
	//        the number of networks queried is limited to 100
	//
	private void findTargetCandidates() throws JsonProcessingException, IOException {
		targetCandidates = target.getNdex().findNetworks("", true, target.getUsername(), null, false, 0, 10000);
		LOGGER.info("Found " + targetCandidates.size() + " networks in target NDEx under  " + target.getUsername());
	}

	public abstract void findSourceNetworks() throws NdexException, IOException;

	// Get the provenance history for each candidate network in the target account
	//
	private void getAllTargetProvenance() throws JsonProcessingException, IOException, NdexException {
		LOGGER.info("Getting provenance history for " + targetCandidates.size() + " candidate networks in target account");
		getAllProvenance(target, targetCandidates, tgtProvenanceMap);
	}

	// Get the provenance history for each source network
	//
	private void getAllSourceProvenance() throws JsonProcessingException, IOException, NdexException {
		LOGGER.info("Getting Source Network Provenance for " + sourceNetworks.size() + " networks");
		getAllProvenance(source, sourceNetworks, srcProvenanceMap);
	}
	
	// Get the provenance history for a list of networks
	// Store by UUID in the provenance map
	//
	private void getAllProvenance(NdexServer server, List<NetworkSummary> networks, Map<UUID, ProvenanceEntity> provenanceMap) { //throws JsonProcessingException, IOException, NdexException{
		
		ArrayList<NetworkSummary> networksNotToCopy = new ArrayList<NetworkSummary>();
		
		for (NetworkSummary network : networks) {
			try {
			    ProvenanceEntity provenance = server.getNdex().getNetworkProvenance(network.getExternalId().toString());
			    if (null != provenance) {
			    	LOGGER.info("Storing Provenance for network " + network.getExternalId());
				    provenanceMap.put(network.getExternalId(), provenance);
			    }
			} catch (IOException | NdexException e) {
				// unable to read this networks' provenance.  It means we won't be able to copy/update it.
				// Let's save it in the networksNotToCopy list and remove it from the copy plan later. 
				networksNotToCopy.add(network);

                System.out.println(e.getMessage());
                continue;
			}
		}
				
		for (NetworkSummary network : networksNotToCopy) {
			// remove networks whose provenance we couldn't read, since we can't copy/update these networks
			networks.remove(network);
		}
		
	}

	// Process one source network
	//
	private void updateTargetNetwork(NetworkSummary sourceNetwork) throws JsonProcessingException, IOException, NdexException {
		LOGGER.info("Trying to update target network created from source " + sourceNetwork.getName() + " ; source last modified " + sourceNetwork.getModificationTime());
		
		// for targetCandidate, get provenance history and determine whether the target candidate
		// is a first generation copy of the source network.
		
		boolean copySourceNetwork = true;

		String sourceNetworkUUID = sourceNetwork.getExternalId().toString(); 
		String parentNetworkUUID = null;
		String parentEntityUri   = null;
		
		// Get the provenance history of the source from the provenance map
		ProvenanceEntity sourceRootProvenanceEntity = srcProvenanceMap.get(sourceNetwork.getExternalId());
		
		// Evaluate all targetCandidates to see if there is an existing copy of the source
		// and whether that copy needs update
		for (NetworkSummary targetCandidate : targetCandidates) {
			
			// get provenance of the target network from the server
			ProvenanceEntity targetRootProvenanceEntity = tgtProvenanceMap.get(targetCandidate.getExternalId());
			
			if (null == targetRootProvenanceEntity){
				// no provenance root entity, hence unknown status
				LOGGER.info("No provenance entity exists for target " + targetCandidate.getExternalId());
				continue;   // get next target network
			} 
				
			LOGGER.info("Processing provenance history for target " + targetCandidate.getExternalId());
			ProvenanceEvent targetProvenanceEvent = targetRootProvenanceEntity.getCreationEvent();
			
			if (null == targetProvenanceEvent) {
				LOGGER.info("No provenance event exists for target " + targetCandidate.getExternalId());
				continue;   // get next target network
			}
			
			if (!SNYC_COPY.equalsIgnoreCase(targetProvenanceEvent.getEventType())) {
				//  latest (most recent) event in provenance
				continue;
			}
			
			
			// COPY was the latest (most recent) event for the current target;  let's get UUID of the parent network
				
			if ((targetRootProvenanceEntity.getProperties() != null) && (targetRootProvenanceEntity.getProperties().size() >= 3) ) {
				parentEntityUri = targetRootProvenanceEntity.getProperties().get(2).getValue();  // get URI of parent network
	
				try {
					//extract UUID from URI
					URI uri = new URI(parentEntityUri);
					String[] segments = uri.getPath().split("/");
					parentNetworkUUID = segments[segments.length-1];
						
					//parentNetworkUUID = parentEntityUri.replaceFirst(".*/([^/?]+).*", "$1");  // this is another way of extracting UUID of parent network
					LOGGER.info("UUID of the parent network found in provenance of target network " + targetCandidate.getExternalId() + 
						    " is " + parentNetworkUUID);
						
				} catch (URISyntaxException e) {

					LOGGER.info("Unable to get UUID of the parent network from provenance of target network " + targetCandidate.getExternalId() 
							+ "  Exception thrown: " + e.getMessage());		
					continue;  // get next target network
				}
					
					
				// if we reached this point, it means we found/extracted from the provenace of target network the UUID of the
				// network that created this target network by COPY and that COPY  was the last event of the target network 
				// (target network was not modified after that). 
				// Let's check UUIDs of source and target networks.
					
				if (!sourceNetworkUUID.equals(parentNetworkUUID))  {
					// this target network was NOT created from the current source network,
					// therefore, we cannot update it
					// LOGGER.info("sourceNetworkUUID " + sourceNetworkUUID + "doesn't equal parentNetworkUUID " + parentNetworkUUID);
					continue;  // get next target network
				} 
					
				if (null == sourceRootProvenanceEntity){
					// no provenance root entity, hence unknown status
					LOGGER.info("No provenance entity exists for source network" + sourceNetwork.getExternalId().toString());
					continue;   // get next target network
				} 		
					
				ProvenanceEvent sourceProvenanceEvent = sourceRootProvenanceEntity.getCreationEvent();
					
				if (null == sourceProvenanceEvent) {
					LOGGER.info("No provenance event exists for source " + sourceNetwork.getExternalId().toString());
					continue;   // get next target network
				}
					
    	//		LOGGER.info("sourceNetworkUUID " + sourceNetworkUUID + " equals parentNetworkUUID " + parentNetworkUUID);
    				
                // target network was created from source network and was not modified after that (last target event was COPY).
			    // Let's check if target network is "out-of-date".

    			// calculate latestSourceDate as the later of modification date and the last provenance history event end date for the source network.
    			Timestamp latestSourceDate = 
    					(sourceNetwork.getModificationTime().after((Timestamp)sourceProvenanceEvent.getEndedAtTime())) ?
    					sourceNetwork.getModificationTime() : ((Timestamp)sourceProvenanceEvent.getEndedAtTime());
    						
    	    	// calculate earliestTargetDate as the earlier of modification date and the last provenance history event end date for the target network.
    	    	Timestamp earliestTargetDate = 
    	    			(targetCandidate.getModificationTime().before((Timestamp)targetProvenanceEvent.getEndedAtTime())) ?
    	    			targetCandidate.getModificationTime() : ((Timestamp) targetProvenanceEvent.getEndedAtTime());

    	    	// System.out.println("sourceNetwork.getModificationTime()=" + sourceNetwork.getModificationTime() + "   " +
    	    	// 		" (Timestamp)sEvent.getEndedAtTime()=" + (Timestamp)sourceProvenanceEvent.getEndedAtTime());	
    	    	    		
    	    	// System.out.println("targetCandidate.getModificationTime()=" + targetCandidate.getModificationTime() + "   " +
    	    	//		" pEvent.getEndedAtTime()=" + (Timestamp) targetProvenanceEvent.getEndedAtTime());	
    	    		
    	    	//System.out.println("latestSourceDate=" + latestSourceDate.toString() + "   earliestTargetDate= " + earliestTargetDate.toString() );
    	    				
                if (latestSourceDate.before(earliestTargetDate)) {
                    // target network update/modify time is more recent than that of source network;  don't update target,
                	// we may need to copy source network to target server
                    LOGGER.info("latestSourceDate = " + latestSourceDate.toString() + 
                    		";  earliestTargetDate =  " + earliestTargetDate.toString() + ". Not updating target.");
                    	
                   	// since there exists a copy of the source network on the target server that doesn't require updating,
                   	// we will not copy this source network to target.
                    copySourceNetwork = false;
                    	
                    continue;  // get next target network
                }

                // let's check if the target network is read-only, and if yes, check the value of updateReadOnlyNetwork 
                // configuration parameter.  To check if target is read-only, if (targetCandidate.getReadOnlyCommitId() > 0).
                    
    	    	if ((targetCandidate.getReadOnlyCommitId() > 0) && (false == updateReadOnlyNetwork)) {
    	     	    // the target is read-only and updateReadOnlyNetwork config parameter is false, don't update target
    				LOGGER.info("Target network " + targetCandidate.getExternalId() + " is read-only and updateReadOnlyNetwork is false. Not updating target.");
                    	
    				//copySourceNetwork = false;             	
                    continue;  // get next target network
    	    	}
    				
    	    	// finally, update the target network
    	    	System.out.println("Updating network " + sourceNetwork.getExternalId() + "(source) -> " + targetCandidate.getExternalId() + "(target)");
    	    	if (target.isReadOnly(targetCandidate.getExternalId())) {
    	    		// target network is read-only
					updateReadonlyNetworkAsCX(sourceNetwork, targetCandidate);
					
    	    		copySourceNetwork = false;
    	    	} else {
    	    		// target network is not read-only
						updateNetworkAsCX(sourceNetwork, targetCandidate);
					

    	    		copySourceNetwork = false;
    	    	}
    	    		
			} else {  // if ((pRoot.getProperties() != null) && (pRoot.getProperties().size() >= 3) )
				
				LOGGER.info("Unable to get UUID of the parent network because the pav:retrievedFrom property is missing from provenance of target network " + 
				     targetCandidate.getExternalId());		
				continue;  // get next target network
			}
		}
		
		// we finished looping through the list of target networks.
		// If no copy of the source network exists on the target, then copy source network to target
		if (copySourceNetwork) {
			LOGGER.info("No target that is a copy of the source found, will therefore copy the network ");
			copyNetworkAsCX(sourceNetwork);

			copySourceNetwork = false; 
		}
	}

	private void updateNetworkAsCX(NetworkSummary sourceNetwork, NetworkSummary targetNetwork)
	{
		try
		{
			InputStream cxStream = source.getNdex().getNetworkAsCXStream(sourceNetwork.getExternalId().toString());
			target.getNdex().updateCXNetwork(targetNetwork.getExternalId(), cxStream);
			LOGGER.info("Updated " + sourceNetwork.getExternalId() + " to " + targetNetwork.getExternalId());
			while ( ! target.finishedLoading(targetNetwork.getExternalId())) {
				System.out.println("Waiting for this network to be validated by NDEx server.");
				Thread.sleep(3000);
			}
			ProvenanceEntity newProvananceHistory = createCopyProvenance(targetNetwork, sourceNetwork);

			ObjectMapper mapper = new ObjectMapper();
			String s0 = mapper.writeValueAsString( newProvananceHistory);
//			System.out.print("\n\n" + s0 + "\n\n");

			target.setNetworkProvenance(targetNetwork.getExternalId(), newProvananceHistory);
			LOGGER.info("Set provenance for copy " + targetNetwork.getExternalId());
		}
		catch (Exception e)
		{
			LOGGER.severe("Error attempting to update as cx " + sourceNetwork.getExternalId());
			e.printStackTrace();
		}
	}

	private void updateReadonlyNetworkAsCX(NetworkSummary sourceNetwork, NetworkSummary targetNetwork)
	{
		String networkId = targetNetwork.getExternalId().toString();

		try {
			// set target network to read-write mode
			target.getNdex().setNetworkFlag(networkId, "readOnly", "false");
		} catch (Exception e) {
			LOGGER.severe("Error attempting  to set readOnly flag to false for network " + sourceNetwork.getExternalId());
			e.printStackTrace();
		}

		updateNetworkAsCX(sourceNetwork, targetNetwork);

		try {
			// set target network back to read-only mode
			target.getNdex().setNetworkFlag(networkId, "readOnly", "true");
		} catch (Exception e) {
			LOGGER.severe("Error attempting  to set readOnly flag to true for network " + sourceNetwork.getExternalId());
			e.printStackTrace();
		}
	}

/*	private void updateReadOnlyNetwork(NetworkSummary sourceNetwork, NetworkSummary targetNetwork) throws IOException, NdexException {
		
		String networkId = targetNetwork.getExternalId().toString();

		try {
	        // set target network to read-write mode
			target.getNdex().setNetworkFlag(networkId, "readOnly", "false");
		} catch (Exception e) {
			LOGGER.severe("Error attempting  to set readOnly flag to false for network " + sourceNetwork.getExternalId());
			e.printStackTrace();
		}
		
		// update target network
		updateNetwork(sourceNetwork, targetNetwork);

		try {
	        // set target network back to read-only mode
			target.getNdex().setNetworkFlag(networkId, "readOnly", "true");
		} catch (Exception e) {
			LOGGER.severe("Error attempting  to set readOnly flag to true for network " + sourceNetwork.getExternalId());
			e.printStackTrace();			
		}
	}*/
	
/*	private void updateNetwork(NetworkSummary sourceNetwork, NetworkSummary targetNetwork) throws IOException, NdexException {
		
		Network entireNetwork = source.getNdex().getNetwork(sourceNetwork.getExternalId().toString());
		
		entireNetwork.setExternalId(targetNetwork.getExternalId());

		try {

			// update target network
			NetworkSummary copiedNetwork = target.getNdex().updateNetwork(entireNetwork);

			LOGGER.info("Updated " + sourceNetwork.getExternalId() + " to " + copiedNetwork.getExternalId());
			ProvenanceEntity newProvananceHistory = createCopyProvenance(copiedNetwork, sourceNetwork);
			target.getNdex().setNetworkProvenance(copiedNetwork.getExternalId().toString(), newProvananceHistory);
			LOGGER.info("Set provenance for copy " + copiedNetwork.getExternalId());
		} catch (Exception e) {
			LOGGER.severe("Error attempting to copy " + sourceNetwork.getExternalId());
			e.printStackTrace();
		}
	} */
	
	// Process one source network
	//
	private void copySourceNetwork(NetworkSummary sourceNetwork) throws JsonProcessingException, IOException, NdexException {
		LOGGER.info("Processing source network " + sourceNetwork.getName() + " last modified " + sourceNetwork.getModificationTime());
		
		// Get the provenance history of the source from the provenance map
		ProvenanceEntity sRoot = srcProvenanceMap.get(sourceNetwork.getExternalId());
		
		// for targetCandidate, get provenance history and determine whether the target candidate
		// is a first generation copy of the source network.
		
		NetworkSummary targetNetwork = null;
		boolean targetNetworkNeedsUpdate = false;
		
		// Evaluate all targetCandidates to see if there is an existing copy of the source
		// and whether that copy needs update
		for (NetworkSummary targetCandidate : targetCandidates){
			ProvenanceEntity pRoot = tgtProvenanceMap.get(targetCandidate.getExternalId().toString());
			
			if (null == pRoot){
				// no provenance root entity, hence unknown status
				LOGGER.info("No provenance history for target " + targetCandidate.getExternalId());
				
			} else {
				LOGGER.info("Processing provenance history for target " + targetCandidate.getExternalId());
				ProvenanceEvent pEvent = pRoot.getCreationEvent();
				
				// is the creation event a copy?
				// TODO: checking for valid copy event: should have just one input
				if (null != pEvent && SNYC_COPY.equalsIgnoreCase(pEvent.getEventType())){
					LOGGER.info("Found target candidate that is derived from a copy event ");
					List<ProvenanceEntity> inputs = pEvent.getInputs();
					if (null != inputs && inputs.size() > 0){
						
						// does the input UUID match source UUID? 
						ProvenanceEntity input = inputs.get(0);
						if (input.getUri().equalsIgnoreCase(sRoot.getUri())){
							// Yes, this is a copy of the source network
							LOGGER.info("Found direct copy of source network " + sRoot.getUri());
							targetNetwork = targetCandidate;
							
							
							// Now check the modification date...
							if(sourceNetwork.getModificationTime().after(pEvent.getEndedAtTime())){
								// The sourceNetwork is later than the end date of the copy event
								// Therefore we should update the target
								LOGGER.info("Source copy date is after target copy event, therefore needs update"); 
								targetNetworkNeedsUpdate = true;
								
								break;
							}
						}
					}
	
				} else {
					LOGGER.info("No provenance event or not a copy event for  " + targetCandidate.getExternalId());
					// Most proximal event is not a copy, so this network cannot match the source,
					// Therefore do nothing
				}
			}
		}
		
		// now do a copy or update, if 
		// 1. there is no target that is a copy or 
		// 2. if there is a only a copy needing update.
		//
		if (null != targetNetwork){
			if (targetNetworkNeedsUpdate){
				// overwrite target
				LOGGER.info("We have a target that is a copy needing update, but updateTargetNetwork is false, so just making another copy.");
				copyNetworkAsCX(sourceNetwork);

			} else {
				LOGGER.info("We have a target that is an existing copy, but it does not need update, therefore not copying.");
			}
		} else {
			// no target found, copy network
			LOGGER.info("No target that is a copy of the source found, will therefore copy the network ");
				copyNetworkAsCX(sourceNetwork);
		}
	}
	
/*	private void copyNetwork(NetworkSummary sourceNetwork) throws IOException, NdexException{
		
		try {
			// TODO create updated provenance history
			Network entireNetwork = source.getNdex().getNetwork(sourceNetwork.getExternalId().toString());
			long lStartTime = System.currentTimeMillis();
			NetworkSummary copiedNetwork = target.getNdex().createNetwork(entireNetwork);
			long lEndTime = System.currentTimeMillis();
			long lElapsedTime = lEndTime - lStartTime;
			LOGGER.info("Copied " + sourceNetwork.getExternalId() + " to " + copiedNetwork.getExternalId()  + " in " + lElapsedTime/1000 + " seconds");
			ProvenanceEntity newProvananceHistory = createCopyProvenance(copiedNetwork, sourceNetwork);
			target.getNdex().setNetworkProvenance(copiedNetwork.getExternalId().toString(), newProvananceHistory);
			LOGGER.info("Set provenance for copy " + copiedNetwork.getExternalId());
		} catch (Exception e) {
			LOGGER.severe("Error attempting to copy " + sourceNetwork.getExternalId());
			e.printStackTrace();
		}
	} */
	
	private void copyNetworkAsCX(NetworkSummary sourceNetwork) throws IOException, NdexException{
		try {
			long lStartTime = System.currentTimeMillis();
			InputStream inStream = source.getNdex().getNetworkAsCXStream(sourceNetwork.getExternalId().toString());
			UUID copiedNetworkId = target.getNdex().createCXNetwork(inStream);
			long lEndTime = System.currentTimeMillis();
			long lElapsedTime = lEndTime - lStartTime;
			while ( ! target.finishedLoading(copiedNetworkId)) {
				System.out.println("Waiting for this network to be validated by NDEx server.");
				Thread.sleep(3000);
			}
			// TODO create updated provenance history
			NetworkSummary copiedNetwork = target.getNdex().getNetworkSummaryById(copiedNetworkId.toString());
			LOGGER.info("Copied (via CX) " + sourceNetwork.getExternalId() + " to " + copiedNetwork.getExternalId() + " in " + lElapsedTime/1000 + " seconds");

			ProvenanceEntity newProvananceHistory = createCopyProvenance(copiedNetwork, sourceNetwork);

			ObjectMapper mapper = new ObjectMapper();
			String s0 = mapper.writeValueAsString( newProvananceHistory);
			System.out.print("\n\n" + s0 + "\n\n");

			target.setNetworkProvenance(copiedNetwork.getExternalId(), newProvananceHistory);
			
		} catch (Exception e) {
			LOGGER.severe("Error attempting to copy " + sourceNetwork.getExternalId());
			e.printStackTrace();
		}
	}
	
	// Attributes to be read from file

	private ProvenanceEntity createCopyProvenance(
			NetworkSummary copiedNetwork,
			NetworkSummary sourceNetwork) {
		ProvenanceEntity sourceProvenanceEntity = srcProvenanceMap.get(sourceNetwork.getExternalId());
		
		// If the source has no provenance history, we create a minimal
		// ProvenanceEntity that has the appropriate URI
		if (null == sourceProvenanceEntity){
			sourceProvenanceEntity = new ProvenanceEntity(sourceNetwork, source.getNdex().getBaseRoute());
		}
		
	
		// Create the history
		ProvenanceEntity copyProv = ProvenanceHelpers.createProvenanceHistory(
				copiedNetwork,
				copiedNetwork.getURI(),
				SNYC_COPY, 
				new Timestamp(Calendar.getInstance().getTimeInMillis()),
				sourceProvenanceEntity
				);
		
		// Add properties based on 
		if (null != sourceNetwork.getName()){
			PropertyHelpers.addProperty("dc:title", sourceNetwork.getName(), copyProv.getProperties());
		}
		if (null != sourceNetwork.getDescription()){
			PropertyHelpers.addProperty("dc:description", sourceNetwork.getName(), copyProv.getProperties());
		}
		PropertyHelpers.addProperty("pav:retrievedFrom", sourceNetwork.getURI(), copyProv.getProperties());
		return copyProv;
	}

	public String getTargetGroupName() {
		return targetGroupName;
	}

	public void setTargetAccountName(String targetGroupName) {
		this.targetGroupName = targetGroupName;
	}

	public String getPlanFileName() {
		return planFileName;
	}

	public void setPlanFileName(String planFileName) {
		this.planFileName = planFileName;
	}

	public NdexServer getSource() {
		return source;
	}

	public void setSource(NdexServer source) {
		this.source = source;
	}

	public NdexServer getTarget() {
		return target;
	}

	public void setTarget(NdexServer target) {
		this.target = target;
	}

	public boolean getUpdateTargetNetwork() {
		return updateTargetNetwork;
	}	
	
	public void setUpdateTarget(boolean updateTargetNetwork) {
		this.updateTargetNetwork = updateTargetNetwork;
	}
	
	public boolean getUpdateReadOnlyNetwork() {
		return updateReadOnlyNetwork;
	}	
	
	public void setUpdateReadOnlyNetwork(boolean updateReadOnlyNetwork) {
		this.updateReadOnlyNetwork = updateReadOnlyNetwork;
	}

}
