package top;


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
			copier.runPlans();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
