package top;

import java.io.InputStream;
import java.net.HttpURLConnection;

import org.ndexbio.model.object.Network;

import com.fasterxml.jackson.databind.JsonNode;


public class CommandProcessor {
	
	
	
	public CommandProcessor (){
		
	}

	public static void main(String[] args) {
		// For the moment, as a proof of concept, 
		// we will ignore the arguments and just run
		// a pre-packaged set of queries to the local NDEx,
		// assuming that it is running with the default database.
		CommandProcessor cp = new CommandProcessor();
		Copier copier = new Copier();
		try {
			copier.testGet();
			copier.testCopyBELNetworkInBlocks();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//cp.readSyncFiles();
		//cp.processTasks();

	}
	
	private void readSyncFiles(){
		try {
			//printTermsInNetworkByNamespace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void processTasks(){
		try {
			//printTermsInNetworkByNamespace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	

}
