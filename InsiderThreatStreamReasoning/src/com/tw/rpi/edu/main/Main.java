package com.tw.rpi.edu.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.openrdf.rio.RDFFormat;

import com.complexible.stardog.StardogException;
import com.tw.rpi.edu.si.strategy.SemanticImportance;
import com.tw.rpi.edu.si.utilities.SnarlClient;

public class Main {
	// global variables
	private static String serverURL = "snarl://localhost:5820/";
	private static String username = "admin";
	private static String password = "admin";
	private static String prefix = "http://tw.rpi.edu/ontology/DataExfiltration/";
	private static String ontology = "D:/Research/github/InsiderThreatUseCase/ontology-data exfiltration alone/DataExfiltration-before import.owl";
	private static String background = "data/background/";
	private static String data_scenario1 = "data/ACM2278-annotation.txt";
	private static String data_scenario2 = "data/CMP2946-annotation.txt";
	private static String data_scenario4 = "data/CDE1846-annotation.txt";
	private static String data_scenario5 = "data/MBG3183-annotation.txt";
	
	// the client that talks to the back-end Stardog triple-store	
	private static SnarlClient client = 
			new SnarlClient(serverURL, "db", username, password);

	// main function
	public static void main(String[] args) {
		client.emptyDB();
		
		// load backgrounds
		client.getANonReasoningConn().begin();
		try {
			// ontology
			client.getANonReasoningConn().add().io().format(RDFFormat.RDFXML)
			  .stream(new FileInputStream(ontology));
			// decoy file info
			client.getANonReasoningConn().add().io().format(RDFFormat.N3)
			  .stream(new FileInputStream(background+"decoy.nt"));
			// pc-employee pair info
			client.getANonReasoningConn().add().io().format(RDFFormat.N3)
			  .stream(new FileInputStream(background+"pc.nt"));
			// LDAP info
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2009-12.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-01.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-02.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-03.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-04.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-05.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-06.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-07.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-08.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-09.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-10.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-11.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2010-12.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2011-01.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2011-02.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2011-03.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2011-04.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS)
			  .stream(new FileInputStream(background+"2011-05.nq"));			
		} catch (StardogException | FileNotFoundException e) {
			System.out.println("[ERROR] background loading failed");
			e.printStackTrace();
		}
		client.getANonReasoningConn().commit();
		System.out.println("[INFO] background ontology loaded ... ");
		
		// streaming file
		String data = data_scenario1;
		
		// run:
		SemanticImportance si = new SemanticImportance(data, client, prefix);
		si.run();
		
		// benchmark writer
		PrintWriter metricRecorder = null;		
	
		// close up
		metricRecorder.close();
		client.cleanUp();
	}
}
