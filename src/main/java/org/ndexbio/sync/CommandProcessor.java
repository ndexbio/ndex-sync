package org.ndexbio.sync;


public class CommandProcessor {
	
	
	
	public CommandProcessor (){
		
	}

	public static void main(String[] args) {
		// expects one argument - a directory in which to find the copy plans
		//CommandProcessor cp = new CommandProcessor();
		Copier copier = new Copier();
		try {
			if (args.length == 0) {
				System.out.println("NDEx Copier requires a directory of copy plans as an argument");
			} else {
				copier.runPlans(args[0]);
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
