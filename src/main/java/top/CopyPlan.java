package top;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.ProvenanceEvent;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.core.JsonProcessingException;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "planType")
@JsonSubTypes(value = { @Type(value = QueryCopyPlan.class, name = "QueryCopyPlan"), @Type(value = IdCopyPlan.class, name = "IdCopyPlan") })
public abstract class CopyPlan {
	private final static Logger LOGGER = Logger.getLogger(CopyPlan.class.getName());
	
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
		findSourceNetworks();
		//getAllSourceProvenance();
		findTargetCandidates();
		//getAllTargetProvenance();
		for (NetworkSummary network: sourceNetworks){
			processSourceNetwork(network);
		}
		
	}

	private void findTargetCandidates() throws JsonProcessingException, IOException {
		// Find networks in target NDEx in the target account.
		// ...for the moment...
		//        this is always the target user account.
		//        and the number of networks queried is limited to 100
		List<NetworkSummary> targetCandidates = target.getNdex().findNetworks("", target.getUsername(), 0, 100);
		LOGGER.info("Found " + targetCandidates.size() + " networks in target NDEx under  " + target.getUsername());
		
	}

	public void findSourceNetworks(){
		System.out.println("Default no-op method for finding source networks");
		sourceNetworks = new ArrayList<NetworkSummary>();
	}
	
	private void getAllTargetProvenance() throws JsonProcessingException, IOException {
		// Get the provenance structure for each candidate network
		// store by UUID in the provenance map
		getAllProvenance(target, targetCandidates);
	}

	private void getAllSourceProvenance() throws JsonProcessingException, IOException {
		// Get the provenance structure for each candidate network
		// Store by UUID in the provenance map
		getAllProvenance(source, sourceNetworks);
	}
	
	private void getAllProvenance(NdexServer server, List<NetworkSummary> networks) throws JsonProcessingException, IOException{
		for (NetworkSummary network : networks){
			ProvenanceEntity provenance = server.getNdex().getNetworkProvenance(network.getExternalId().toString());
			if (null != provenance){
				provenanceMap.put(network.getExternalId().toString(), provenance);
			}
		}	
	}

	private void processSourceNetwork(NetworkSummary sourceNetwork) throws JsonProcessingException, IOException {
		LOGGER.info("Processing source network " + sourceNetwork.getName() + " last modified " + sourceNetwork.getModificationDate());
		// Get the provenance of the source
		ProvenanceEntity sRoot = provenanceMap.get(sourceNetwork.getExternalId().toString());
		
		// for targetCandidate, get provenance and determine whether the target candidate
		// is a first generation copy of the source network.
		
		NetworkSummary targetNetwork = null;
		
		for (NetworkSummary targetCandidate : targetCandidates){
			ProvenanceEntity pRoot = provenanceMap.get(targetCandidate.getExternalId().toString());
			
			if (null != pRoot){
				// no provenance, hence unknown status
				
			} else {
				ProvenanceEvent pEvent = pRoot.getCreationEvent();
				
				// is the creation event a copy?
				// TODO: checking for valid copy event: should have just one input
				if ("COPY" == pEvent.getEventType()){
					
					List<ProvenanceEntity> inputs = pEvent.getInputs();
					if (null != inputs && inputs.size() > 0){
						
						// does the input UUID match source UUID? 
						ProvenanceEntity input = inputs.get(0);
						if (input.getUri() == sRoot.getUri()){
							// Yes, this is a copy of the source network
							
							
							// Now check the modification date...
							if(sourceNetwork.getModificationDate().after(pEvent.getEndDate())){
								// The sourceNetwork is later than the end date of the copy event
								// Therefore we should update the target
							
								targetNetwork = targetCandidate;
								
								break;
							}
						}
					}
	
				} else {
					// Most proximal event is not a copy, so this network cannot match the source
				}
			}
		}
		if (null != target){
			// overwrite target
		} else {
			// no target found, copy network
			Network entireNetwork = source.getNdex().getNetwork(sourceNetwork.getExternalId().toString());
			try {
				target.getNdex().createNetwork(entireNetwork);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// Attributes to be read from file

	private String getNetworkURI(NetworkSummary sourceNetwork) {
		// TODO Auto-generated method stub
		return null;
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
