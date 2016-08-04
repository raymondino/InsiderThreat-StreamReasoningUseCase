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
	// private static String ontology = "D:/Research/github/InsiderThreat-StreamReasoningUseCase/ontology-data exfiltration alone/DataExfiltration-before import.owl";
    // private static String ontology = "/data/InsiderThreat-StreamReasoningUseCase/ontology-data exfiltration alone/DataExfiltration-before import.owl";
	private static String ontology = "C:/Users/Rui/Documents/GitHub/InsiderThreat-StreamReasoningUseCase/ontology-data exfiltration alone/DataExfiltration-before import.owl";
	private static String background = "data/background/";
	private static String individual_file1 = "data/different-individuals/text1.txt";
	private static String individual_file2 = "data/different-individuals/text2.txt";
	private static String individual_file3 = "data/different-individuals/text3.txt";
	
	// the client that talks to the back-end Stardog triple-store	
	private static SnarlClient client = new SnarlClient(serverURL, "db", username, password);

	// main function
	public static void main(String[] args) {
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
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
		
		// // ACM2278 date range: 8/18/2010 - 8/24/2010
		// ArrayList<String> ACM2278streamingdata = new ArrayList<String>();
		// ACM2278streamingdata.add("data/streamingdata-1user/1userlist-ACM2278_annotation.txt");
		// ACM2278streamingdata.add("data/streamingdata-10user/10userlist-ACM2278_annotation.txt");
		// ACM2278streamingdata.add("data/streamingdata-100user/100userlist-ACM2278_annotation.txt");
		// ACM2278streamingdata.add("data/streamingdata-1000user/1000userlist-ACM2278_annotation.txt");

		// // CMP2946 data range: 2/2/2011 - 3/30/2011
		// ArrayList<String> CMP2946streamingdata = new ArrayList<String>();
		// CMP2946streamingdata.add("data/streamingdata-1user/1userlist-CMP2946_annotation.txt");
		// CMP2946streamingdata.add("data/streamingdata-10user/10userlist-CMP2946_annotation.txt");
		// CMP2946streamingdata.add("data/streamingdata-100user/100userlist-CMP2946_annotation.txt");
		// CMP2946streamingdata.add("data/streamingdata-1000user/1000userlist-CMP2946_annotation.txt");

		// // CDE1846 data range: 2/21/2011 - 4/25/2011
		// ArrayList<String> CDE1846streamingdata = new ArrayList<String>();
		// CDE1846streamingdata.add("data/streamingdata-1user/1userlist-CDE1846_annotation.txt");
		// CDE1846streamingdata.add("data/streamingdata-10user/10userlist-CDE1846_annotation.txt");
		// CDE1846streamingdata.add("data/streamingdata-100user/100userlist-CDE1846_annotation.txt");
		// CDE1846streamingdata.add("data/streamingdata-1000user/1000userlist-CDE1846_annotation.txt");

		// // MBG3183 data range: 10/12/2010 - 10/13/2010
		// ArrayList<String> MBG3183streamingdata = new ArrayList<String>();
		// MBG3183streamingdata.add("data/streamingdata-1user/1userlist-MBG3183_annotation.txt");
		// MBG3183streamingdata.add("data/streamingdata-10user/10userlist-MBG3183_annotation.txt");
		// MBG3183streamingdata.add("data/streamingdata-100user/100userlist-MBG3183_annotation.txt");
		// MBG3183streamingdata.add("data/streamingdata-1000user/1000userlist-MBG3183_annotation.txt");

		// // window size = 1 day bench
		// for(String s:ACM2278streamingdata) {
		// 	int users = 1;
		// 	if(s.contains("1000")) {users = 1000; }
		// 	else if(s.contains("100")) {users = 100;}
		// 	else if(s.contains("10")) {users = 10;}
		// 	ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", 1, users); prov.run();
		// 	ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", 1, users); trust.run();
		// 	ProvTrustSI nothing = new ProvTrustSI(s, client, "", 1, users); nothing.run();
		// }
		// for(String s:MBG3183streamingdata) {
		// 	int users = 1;
		// 	if(s.contains("1000")) {users = 1000; }
		// 	else if(s.contains("100")) {users = 100;}
		// 	else if(s.contains("10")) {users = 10;}
		// 	ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", 1, users); prov.run();
		// 	ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", 1, users); trust.run();
		// 	ProvTrustSI nothing = new ProvTrustSI(s, client, "", 1, users); nothing.run();
		// }
		// for(String s:CMP2946streamingdata) {
		// 	int users = 1;
		// 	if(s.contains("1000")) {users = 1000; }
		// 	else if(s.contains("100")) {users = 100;}
		// 	else if(s.contains("10")) {users = 10;}
		// 	ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", 1, users); prov.run();
		// 	ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", 1, users); trust.run();
		// 	ProvTrustSI nothing = new ProvTrustSI(s, client, "", 1, users); nothing.run();		
		// }
		// for(String s:CDE1846streamingdata) {
		// 	int users = 1;
		// 	if(s.contains("1000")) {users = 1000; }
		// 	else if(s.contains("100")) {users = 100;}
		// 	else if(s.contains("10")) {users = 10;}
		// 	ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", 1, users); prov.run();
		// 	ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", 1, users); trust.run();
		// 	ProvTrustSI nothing = new ProvTrustSI(s, client, "", 1, users); nothing.run();		
		// }

		// // window size = 7 days bench
		// for(String s:CMP2946streamingdata) {
		// 	int users = 1;
		// 	if(s.contains("1000")) {users = 1000; }
		// 	else if(s.contains("100")) {users = 100;}
		// 	else if(s.contains("10")) {users = 10;}
		// 	ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", 7, users); prov.run();
		// 	ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", 7, users); trust.run();
		// 	ProvTrustSI nothing = new ProvTrustSI(s, client, "", 7, users); nothing.run();		
		// }
		// for(String s:CDE1846streamingdata) {
		// 	int users = 1;
		// 	if(s.contains("1000")) {users = 1000; }
		// 	else if(s.contains("100")) {users = 100;}
		// 	else if(s.contains("10")) {users = 10;}
		// 	ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", 7, users); prov.run();
		// 	ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", 7, users); trust.run();
		// 	ProvTrustSI nothing = new ProvTrustSI(s, client, "", 7, users); nothing.run();		
		// }

		// // window size = 1 month bench
		// for(String s:CMP2946streamingdata) {
		// 	int users = 1;
		// 	if(s.contains("1000")) {users = 1000; }
		// 	else if(s.contains("100")) {users = 100;}
		// 	else if(s.contains("10")) {users = 10;}
		// 	ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", 28, users); prov.run();
		// 	ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", 28, users); trust.run();
		// 	ProvTrustSI nothing = new ProvTrustSI(s, client, "", 28, users); nothing.run();		
		// }
		// for(String s:CDE1846streamingdata) {
		// 	int users = 1;
		// 	if(s.contains("1000")) {users = 1000; }
		// 	else if(s.contains("100")) {users = 100;}
		// 	else if(s.contains("10")) {users = 10;}
		// 	ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", 28, users); prov.run();
		// 	ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", 28, users); trust.run();
		// 	ProvTrustSI nothing = new ProvTrustSI(s, client, "", 28, users); nothing.run();		
		// }

		// streaming files contain 1 user each
		 ArrayList<String> streamingData1 = new ArrayList<String>();
		 streamingData1.add("data/streamingdata-1user/1userlist-ACM2278_annotation.txt");
		 streamingData1.add("data/streamingdata-1user/1userlist-CMP2946_annotation.txt");
		 streamingData1.add("data/streamingdata-1user/1userlist-CDE1846_annotation.txt");
		 streamingData1.add("data/streamingdata-1user/1userlist-MBG3183_annotation.txt");

		// // streaming files contain 10 users each
		 ArrayList<String> streamingData10 = new ArrayList<String>();
		 streamingData10.add("data/streamingdata-10user/10userlist-ACM2278_annotation.txt");
		 streamingData10.add("data/streamingdata-10user/10userlist-CMP2946_annotation.txt");
		 streamingData10.add("data/streamingdata-10user/10userlist-CDE1846_annotation.txt");
		 streamingData10.add("data/streamingdata-10user/10userlist-MBG3183_annotation.txt");
		
		 // streaming files contain 100 users each
		 ArrayList<String> streamingData100 = new ArrayList<String>();
		 streamingData100.add("data/streamingdata-100user/100userlist-ACM2278_annotation.txt");
		 streamingData100.add("data/streamingdata-100user/100userlist-CMP2946_annotation.txt");
		 streamingData100.add("data/streamingdata-100user/100userlist-CDE1846_annotation.txt");
		 streamingData100.add("data/streamingdata-100user/100userlist-MBG3183_annotation.txt");
		
		// streaming files contain 1000 users each
		ArrayList<String> streamingData1000 = new ArrayList<String>();
		streamingData1000.add("data/streamingdata-1000user/1000userlist-ACM2278_annotation.txt");
		streamingData1000.add("data/streamingdata-1000user/1000userlist-CMP2946_annotation.txt");
		streamingData1000.add("data/streamingdata-1000user/1000userlist-CDE1846_annotation.txt");
		streamingData1000.add("data/streamingdata-1000user/1000userlist-MBG3183_annotation.txt");

		// window size
		ArrayList<Integer> windowSizes = new ArrayList<Integer>();
		windowSizes.add(1); // 1 day
		windowSizes.add(7); // 1 week
		windowSizes.add(28); // 1 month
		
		// run benchmark
		 for(String s:streamingData1) {
		 	for(Integer w:windowSizes) {
		 		if((w == 7 || w == 28) && (s.contains("ACM2278") || s.contains("MBG3183"))) {
		 			continue;
		 		}
		 		client.emptyDB(); loadBackground();
		 		ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", w, 1); prov.run();
		 		client.emptyDB(); loadBackground();
		 		ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", w, 1); trust.run();
		 		client.emptyDB(); loadBackground();
		 		ProvTrustSI nothing = new ProvTrustSI(s, client, "", w, 1); nothing.run();
		 	}
		 }
		
		 for(String s:streamingData10) {
		 	for(Integer w:windowSizes) {
		 		if((w == 7 || w == 28) && (s.contains("ACM2278") || s.contains("MBG3183"))) {
		 			continue;
		 		}				
		 		client.emptyDB(); loadBackground();
		 		ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", w, 10); prov.run();
		 		client.emptyDB(); loadBackground();
		 		ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", w, 10); trust.run();
		 		client.emptyDB(); loadBackground();
		 		ProvTrustSI nothing = new ProvTrustSI(s, client, "", w, 10); nothing.run();
		 	}
		 }
		
		 for(String s:streamingData100) {
		 	for(Integer w:windowSizes) {
		 		if((w == 7 || w == 28) && (s.contains("ACM2278") || s.contains("MBG3183"))) {
		 			continue;
		 		}				
		 		client.emptyDB(); loadBackground();
		 		ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", w, 100); prov.run();
		 		client.emptyDB(); loadBackground();
		 		ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", w, 100); trust.run();
		 		client.emptyDB(); loadBackground();
		 		ProvTrustSI nothing = new ProvTrustSI(s, client, "", w, 100); nothing.run();
		 	}
		 }
		
		for(String s:streamingData1000) {
			for(Integer w:windowSizes) {
				if((w == 7 || w == 28) && (s.contains("ACM2278") || s.contains("MBG3183"))) {
					continue;
				}				
				client.emptyDB(); loadBackground();
				ProvTrustSI prov = new ProvTrustSI(s, client, "[prov]", w, 1000); prov.run();
				client.emptyDB(); loadBackground();
				ProvTrustSI trust = new ProvTrustSI(s, client, "[prov,trust]", w, 1000); trust.run();
				client.emptyDB(); loadBackground();
				ProvTrustSI nothing = new ProvTrustSI(s, client, "", w, 1000); nothing.run();
			}
		}
		client.cleanUp();
	}
	
	// load background
	public static void loadBackground() {
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
			// client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2009-12.nq"));
			// client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-01.nq"));
			// client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-02.nq"));
			// client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-03.nq"));
			// client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-04.nq"));
			// client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-05.nq"));
			// client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-06.nq"));
			// client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-07.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-08.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-09.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-10.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-11.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2010-12.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-01.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-02.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-03.nq"));
			client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-04.nq"));
			// client.getANonReasoningConn().add().io().format(RDFFormat.NQUADS).stream(new FileInputStream(background+"2011-05.nq"));			
		} catch (StardogException | FileNotFoundException e) {
			System.out.println("[ERROR] background loading failed");
			e.printStackTrace();
		}
		client.getANonReasoningConn().commit();
		System.out.println("[INFO] background ontology loaded ... ");
	}
}