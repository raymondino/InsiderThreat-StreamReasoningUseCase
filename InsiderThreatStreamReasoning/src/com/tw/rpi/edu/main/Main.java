package com.tw.rpi.edu.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import org.openrdf.rio.RDFFormat;

import com.complexible.common.rdf.model.Values;
import com.complexible.stardog.StardogException;
import com.tw.rpi.edu.si.strategy.ProvTrustSI;
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
	private static String individual_file1 = "data/different-individuals/text1.txt";
	private static String individual_file2 = "data/different-individuals/text2.txt";
	private static String individual_file3 = "data/different-individuals/text3.txt";
	
	// the client that talks to the back-end Stardog triple-store	
	private static SnarlClient client = new SnarlClient(serverURL, "db", username, password);

	// main function
	public static void main(String[] args) {
		// get prepared for a new run
		client.emptyDB(); // clean the db
		// clean the different individual files because reasoning requires to 
		// explicitly claim all actions to be different.
		File file1 = new File(individual_file1);
		File file2 = new File(individual_file2);
		File file3 = new File(individual_file3);
		try {
			file1.delete();
			file2.delete();
			file3.delete();
			BufferedWriter out1 = new BufferedWriter(new FileWriter(file1));
			BufferedWriter out2 = new BufferedWriter(new FileWriter(file2));
			BufferedWriter out3 = new BufferedWriter(new FileWriter(file3));
			out1.write("@prefix owl: <http://www.w3.org/2002/07/owl#> .");
			out1.newLine();
			out1.write("<http://tw.rpi.edu/ontology/DataExfiltration/> a owl:Ontology .");
			out1.newLine(); 
			out1.close();
			out2.write("[]");
			out2.newLine();
			out2.write("  a owl:AllDifferent ;");
			out2.newLine();
			out2.write("  owl:distinctMembers (");
			out2.newLine();
			out2.close();
			out3.write(" ) .");
			out3.close();
		} catch (Exception e) { e.printStackTrace(); }
				
		// load backgrounds
		client.getANonReasoningConn().begin();
		try {
			// ontology
			client.getANonReasoningConn().add().io().context(Values.iri(prefix+"background")).format(RDFFormat.RDFXML).stream(new FileInputStream(ontology));
			// decoy file info into prefix/decoy graph
			client.getANonReasoningConn().add().io().context(Values.iri(prefix+"decoy")).format(RDFFormat.N3).stream(new FileInputStream(background+"decoy.nt"));
			// pc-employee pair info into prefix/pc graph
			client.getANonReasoningConn().add().io().context(Values.iri(prefix+"pc")).format(RDFFormat.N3).stream(new FileInputStream(background+"pc.nt"));
			// LDAP info
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2009-12.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-01.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-02.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-03.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-04.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-05.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-06.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-07.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-08.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-09.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-10.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-11.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-12.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-01.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-02.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-03.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-04.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-05.nq"));			
		} catch (StardogException | FileNotFoundException e) {
			System.out.println("[ERROR] background loading failed");
			e.printStackTrace();
		}
		client.getANonReasoningConn().commit();
		System.out.println("[INFO] background ontology loaded ... ");
		
		// streaming file
		String data = data_scenario1;
		
		// run:
		//SemanticImportance si = new SemanticImportance(data, client, prefix);
		//si.run();
		ProvTrustSI p = new ProvTrustSI(data, client);
		p.run();
		
		// benchmark writer
		//PrintWriter metricRecorder = null;		
	
		// close up
		//metricRecorder.close();
		client.cleanUp();
	}
}
