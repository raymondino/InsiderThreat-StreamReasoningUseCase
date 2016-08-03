package com.tw.rpi.edu.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;

import org.openrdf.rio.RDFFormat;

import com.complexible.common.rdf.model.Values;
import com.complexible.stardog.StardogException;
import com.tw.rpi.edu.si.strategy.ProvTrustSI;
import com.tw.rpi.edu.si.utilities.SnarlClient;

public class Main {
	// global variables
	private static String serverURL = "snarl://localhost:5820/";
	private static String username = "admin";
	private static String password = "admin";
	private static String prefix = "http://tw.rpi.edu/ontology/DataExfiltration/";
//	private static String ontology = "D:/Research/github/InsiderThreatUseCase/ontology-data exfiltration alone/DataExfiltration-before import.owl";
//  private static String ontology = "/data/InsiderThreat-StreamReasoningUseCase/ontology-data exfiltration alone/DataExfiltration-before import.owl";
	private static String ontology = "C:/Users/Rui/Documents/GitHub/InsiderThreat-StreamReasoningUseCase/ontology-data exfiltration alone/DataExfiltration-before import.owl";
	private static String background = "data/background/";
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
//			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2009-12.nq"));
//			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-01.nq"));
//			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-02.nq"));
//			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-03.nq"));
//			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-04.nq"));
//			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-05.nq"));
//			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-06.nq"));
//			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-07.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-08.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-09.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-10.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-11.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-12.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-01.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-02.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-03.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-04.nq"));
//			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-05.nq"));			
		} catch (StardogException | FileNotFoundException e) {
			System.out.println("[ERROR] background loading failed");
			e.printStackTrace();
		}
		client.getANonReasoningConn().commit();
		System.out.println("[INFO] background ontology loaded ... ");
		
		// streaming files contain 1 user each
		ArrayList<String> streamingData1 = new ArrayList<String>();
		streamingData1.add("data/streamingdata-1user/ACM2278-1userlist_annotation.txt");
		streamingData1.add("data/streamingdata-1user/CMP2946-1userlist_annotation.txt");
		streamingData1.add("data/streamingdata-1user/CDE1846-1userlist_annotation.txt");
		streamingData1.add("data/streamingdata-1user/MBG3183-1userlist_annotation.txt");

		// streaming files contain 10 users each
		ArrayList<String> streamingData10 = new ArrayList<String>();
		streamingData10.add("data/streamingdata-10user/ACM2278-10userlist_annotation.txt");
		streamingData10.add("data/streamingdata-10user/CMP2946-10userlist_annotation.txt");
		streamingData10.add("data/streamingdata-10user/CDE1846-10userlist_annotation.txt");
		streamingData10.add("data/streamingdata-10user/MBG3183-10userlist_annotation.txt");
		
		// streaming files contain 100 users each
		ArrayList<String> streamingData100 = new ArrayList<String>();
		streamingData100.add("data/streamingdata-100user/ACM2278-100userlist_annotation.txt");
		streamingData100.add("data/streamingdata-100user/CMP2946-100userlist_annotation.txt");
		streamingData100.add("data/streamingdata-100user/CDE1846-100userlist_annotation.txt");
		streamingData100.add("data/streamingdata-100user/MBG3183-100userlist_annotation.txt");
		
		// streaming files contain 1000 users each
		ArrayList<String> streamingData1000 = new ArrayList<String>();
		streamingData1000.add("data/streamingdata-1000user/ACM2278-1000userlist_annotation.txt");
		streamingData1000.add("data/streamingdata-1000user/CMP2946-1000userlist_annotation.txt");
		streamingData1000.add("data/streamingdata-1000user/CDE1846-1000userlist_annotation.txt");
		streamingData1000.add("data/streamingdata-1000user/MBG3183-1000userlist_annotation.txt");
		
		// window size
		ArrayList<Integer> windowSizes = new ArrayList<Integer>();
		windowSizes.add(1); // 1 day
		windowSizes.add(7); // 1 week
		windowSizes.add(28); // 1 month
		
		// run benchmark
		for(String s:streamingData1) {
			for(Integer w:windowSizes) {
				ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", w, 1);
				prov.run();
				ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", w, 1);
				trust.run();
				ProvTrustSI nothing = new ProvTrustSI(s, client, "", w, 1);
				nothing.run();
			}
		}
		
		for(String s:streamingData10) {
			for(Integer w:windowSizes) {
				ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", w, 10);
				prov.run();
				ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", w, 10);
				trust.run();
				ProvTrustSI nothing = new ProvTrustSI(s, client, "", w, 10);
				nothing.run();
			}
		}
		
		for(String s:streamingData100) {
			for(Integer w:windowSizes) {
				ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", w, 100);
				prov.run();
				ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", w, 100);
				trust.run();
				ProvTrustSI nothing = new ProvTrustSI(s, client, "", w, 100);
				nothing.run();
			}
		}
		
		for(String s:streamingData1000) {
			for(Integer w:windowSizes) {
				ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", w, 1000);
				prov.run();
				ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", w, 1000);
				trust.run();
				ProvTrustSI nothing = new ProvTrustSI(s, client, "", w, 1000);
				nothing.run();
			}
		}
		client.cleanUp();
	}
}