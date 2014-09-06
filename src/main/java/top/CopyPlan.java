package top;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.ProvenanceEvent;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.tools.PropertyHelpers;
import org.ndexbio.model.tools.ProvenanceHelpers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.core.JsonProcessingException;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "planType")
@JsonSubTypes(value = { @Type(value = QueryCopyPlan.class, name = "QueryCopyPlan"), @Type(value = IdCopyPlan.class, name = "IdCopyPlan") })
public abstract class CopyPlan {
	protected final static Logger LOGGER = Logger.getLogger(CopyPlan.class.getName());
	
	NdexServer source;
	NdexServer target;

	String targetGroupName;
	String planFileName;
	List<NetworkSummary> sourceNetworks;
	List<NetworkSummary> targetCandidates;
	Map<String, ProvenanceEntity> provenanceMap;
	

	public void process() throws JsonProcessingException, IOException {
		source.initialize();
		target.initialize();
		provenanceMap = new HashMap<String, ProvenanceEntity>();
		findSourceNetworks();
		getAllSourceProvenance();
		findTargetCandidates();
		getAllTargetProvenance();
		for (NetworkSummary network: sourceNetworks){
			processSourceNetwork(network);
		}
		
	}

	// Find networks in target NDEx in the target account.
	//
	// In this version:
	//        the account is always the target user account.
	//        the number of networks queried is limited to 100
	//
	private void findTargetCandidates() throws JsonProcessingException, IOException {
		targetCandidates = target.getNdex().findNetworks("", target.getUsername(), 0, 100);
		LOGGER.info("Found " + targetCandidates.size() + " networks in target NDEx under  " + target.getUsername());		
	}

	public void findSourceNetworks(){
		LOGGER.severe("Unexpected call to default no-op method for finding source networks");
		sourceNetworks = new ArrayList<NetworkSummary>();
	}

	// Get the provenance history for each candidate network in the target account
	//
	private void getAllTargetProvenance() throws JsonProcessingException, IOException {
		LOGGER.info("Getting provenance history for " + targetCandidates.size() + " candidate networks in target account");
		getAllProvenance(target, targetCandidates);
	}

	// Get the provenance history for each source network
	//
	private void getAllSourceProvenance() throws JsonProcessingException, IOException {
		LOGGER.info("Getting Source Network Provenance for " + sourceNetworks.size() + " networks");
		getAllProvenance(source, sourceNetworks);
	}
	
	// Get the provenance history for a list of networks
	// Store by UUID in the provenance map
	//
	private void getAllProvenance(NdexServer server, List<NetworkSummary> networks) throws JsonProcessingException, IOException{
		
		for (NetworkSummary network : networks){
			ProvenanceEntity provenance = server.getNdex().getNetworkProvenance(network.getExternalId().toString());
			if (null != provenance){
				LOGGER.info("Storing Provenance for network " + network.getExternalId());
				provenanceMap.put(network.getExternalId().toString(), provenance);
			}
		}	
	}

	// Process one source network
	//
	private void processSourceNetwork(NetworkSummary sourceNetwork) throws JsonProcessingException, IOException {
		LOGGER.info("Processing source network " + sourceNetwork.getName() + " last modified " + sourceNetwork.getModificationTime());
		
		// Get the provenance history of the source from the provenance map
		ProvenanceEntity sRoot = provenanceMap.get(sourceNetwork.getExternalId().toString());
		
		// for targetCandidate, get provenance history and determine whether the target candidate
		// is a first generation copy of the source network.
		
		NetworkSummary targetNetwork = null;
		boolean targetNetworkNeedsUpdate = false;
		
		for (NetworkSummary targetCandidate : targetCandidates){
			ProvenanceEntity pRoot = provenanceMap.get(targetCandidate.getExternalId().toString());
			
			if (null == pRoot){
				// no provenance root entity, hence unknown status
				
			} else {
				ProvenanceEvent pEvent = pRoot.getCreationEvent();
				
				// is the creation event a copy?
				// TODO: checking for valid copy event: should have just one input
				if (null != pEvent && "COPY" == pEvent.getEventType()){
					LOGGER.info("Found target candidate that is derived from a copy event ");
					List<ProvenanceEntity> inputs = pEvent.getInputs();
					if (null != inputs && inputs.size() > 0){
						
						// does the input UUID match source UUID? 
						ProvenanceEntity input = inputs.get(0);
						if (input.getUri() == sRoot.getUri()){
							// Yes, this is a copy of the source network
							LOGGER.info("Found direct copy of source network " + sRoot.getUri());
							targetNetwork = targetCandidate;
							
							
							// Now check the modification date...
							if(sourceNetwork.getModificationTime().after(pEvent.getEndedAtTime())){
								// The sourceNetwork is later than the end date of the copy event
								// Therefore we should update the target
								LOGGER.info("Source copy date is after target copy event"); 
								targetNetworkNeedsUpdate = true;
								
								break;
							}
						}
					}
	
				} else {
					// Most proximal event is not a copy, so this network cannot match the source,
					// Therefore do nothing
				}
			}
		}
		if (null != targetNetwork){
			if (targetNetworkNeedsUpdate){
				// overwrite target
				LOGGER.info("We have a target needing update, but update is not implemented yet.");
			} else {
				LOGGER.info("We have a target, but it does not need update, therefore not copying.");
			}
		} else {
			// no target found, copy network
			LOGGER.info("No target found, will therefore copy the network ");
			Network entireNetwork = source.getNdex().getNetwork(sourceNetwork.getExternalId().toString());
			try {
				// TODO create updated provenance history
				NetworkSummary copiedNetwork = target.getNdex().createNetwork(entireNetwork);
				LOGGER.info("Copied " + sourceNetwork.getExternalId() + " to " + copiedNetwork.getExternalId());
				ProvenanceEntity newProvananceHistory = createCopyProvenance(copiedNetwork, sourceNetwork);
				target.getNdex().setNetworkProvenance(copiedNetwork.getExternalId().toString(), newProvananceHistory);
				LOGGER.info("Set provenance for copy " + copiedNetwork.getExternalId());
			} catch (Exception e) {
				LOGGER.severe("Error attempting to copy " + sourceNetwork.getExternalId());
				e.printStackTrace();
			}
		}
	}
	
	// Attributes to be read from file

	private ProvenanceEntity createCopyProvenance(
			NetworkSummary copiedNetwork,
			NetworkSummary sourceNetwork) {
		ProvenanceEntity sourceProvenanceEntity = provenanceMap.get(sourceNetwork.getExternalId().toString());
		
		// If the source has no provenance history, we create a minimal
		// ProvenanceEntity that has the appropriate URI
		if (null == sourceProvenanceEntity){
			sourceProvenanceEntity = new ProvenanceEntity(sourceNetwork, source.getNdex().getBaseRoute());
		}
		
		// Create the history
		ProvenanceEntity copyProv = ProvenanceHelpers.createProvenanceHistory(
				copiedNetwork,
				target.getNdex().getBaseRoute(),
				"COPY", 
				new Timestamp(Calendar.getInstance().getTimeInMillis()),
				sourceProvenanceEntity
				);
		
		// Add properties based on 
		if (null != sourceNetwork.getName()){
			PropertyHelpers.addProperty("DC:Title", sourceNetwork.getName(), copyProv.getProperties());
		}
		if (null != sourceNetwork.getDescription()){
			PropertyHelpers.addProperty("DC:Description", sourceNetwork.getName(), copyProv.getProperties());
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


}
