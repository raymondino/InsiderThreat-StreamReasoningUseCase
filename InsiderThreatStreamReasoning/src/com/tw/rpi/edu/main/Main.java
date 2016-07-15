package com.tw.rpi.edu.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.openrdf.rio.RDFFormat;

import com.complexible.stardog.StardogException;
import com.tw.rpi.edu.si.strategy.SemanticImportance;
import com.tw.rpi.edu.si.utilities.SnarlClient;

public class Main {
	// global variables
	private static String serverURL = "snarl://localhost:5820/";
	private static String username = "admin";
	private static String password = "admin";
	private static String ontology = "D:/Research/github/InsiderThreatUseCase/ontology-data exfiltration alone/DataExfiltration-before import.owl";
	private static String background = "data/background/";
	private static String prefix = "http://tw.rpi.edu/ontology/DataExfiltration/";
	private static String graph = prefix + "graph";

	// the client that talks to the back-end Stardog triple-store	
	private static SnarlClient client = 
			new SnarlClient(serverURL, "db", username, password);

	// main function
	public static void main(String[] args) {
		client.emptyDB();
		// load the background ontology
		client.getANonReasoningConn().begin();
		try {
			client.getANonReasoningConn().add().io().format(RDFFormat.RDFXML)
				  .stream(new FileInputStream(ontology));
		} catch (StardogException | FileNotFoundException e) {
			System.out.println("[ERROR] ontology load failed");
			System.out.println(ontology);
			e.printStackTrace();
		}
		client.getANonReasoningConn().commit();
		System.out.println("[INFO] background ontology loaded ... ");
		
		// benchmark writer
		PrintWriter metricRecorder = null;		
	
		// close up
		metricRecorder.close();
		client.cleanUp();
	}
}
