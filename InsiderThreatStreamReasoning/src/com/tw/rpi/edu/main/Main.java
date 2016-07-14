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
	private static String backgroundOntologyPath = "files/soccer_offside.owl";
	private static String prefix = "http://tw.rpi.edu/web/Ontologies/2016/Soccer_Offside/";
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
				  .stream(new FileInputStream(backgroundOntologyPath));
		} catch (StardogException | FileNotFoundException e) {
			System.out.println("[ERROR] ontology load failed");
			System.out.println(backgroundOntologyPath);
			e.printStackTrace();
		}
		client.getANonReasoningConn().commit();
		System.out.println("[INFO] background ontology loaded ... ");
		
		// strategies configuration
		ArrayList<String> strategies = new ArrayList<String>();
		ArrayList<Double> cachePercentage = new ArrayList<Double>();
		
		strategies.add("FIFO");
		strategies.add("LFU");
		strategies.add("LRU");
		
		cachePercentage.add(0.25);
		cachePercentage.add(0.5);
		cachePercentage.add(0.75);
		
		// benchmark writer
		PrintWriter metricRecorder = null;
		
		ArrayList<String> streamingDataPaths = new ArrayList<String>();
		streamingDataPaths.add("data/sample-data-1st-02m53s");
		streamingDataPaths.add("data/sample-data-1st-04m26s");
		streamingDataPaths.add("data/sample-data-1st-07m04s");
		streamingDataPaths.add("data/sample-data-1st-12m21s");
		streamingDataPaths.add("data/sample-data-1st-12m44s");
		streamingDataPaths.add("data/sample-data-1st-15m28s");
		streamingDataPaths.add("data/sample-data-1st-20m40s");
		streamingDataPaths.add("data/sample-data-1st-21m11s");
		streamingDataPaths.add("data/sample-data-1st-22m07s");
		streamingDataPaths.add("data/sample-data-1st-23m04s");
		streamingDataPaths.add("data/sample-data-2nd-00m52s");
		streamingDataPaths.add("data/sample-data-2nd-10m30s");
		streamingDataPaths.add("data/sample-data-2nd-10m45s");
		streamingDataPaths.add("data/sample-data-2nd-12m16s");
		streamingDataPaths.add("data/sample-data-2nd-17m38s");
		streamingDataPaths.add("data/sample-data-2nd-20m25s");
		streamingDataPaths.add("data/sample-data-2nd-21m43s");
		streamingDataPaths.add("data/sample-data-2nd-24m46s");
		streamingDataPaths.add("data/sample-data-2nd-28m11s");
		streamingDataPaths.add("data/sample-data-2nd-28m24s");
		
		for(String i:streamingDataPaths) {			
			// run strategies without domain literate & cache eviction
			for(String s:strategies) {
				try {
					metricRecorder = new PrintWriter(new FileOutputStream(new File("files/" + s + "_evaluation.txt"), true));
				} catch (FileNotFoundException e) {
					System.out.println("[ERROR] cannot create benchmark result file");
					e.printStackTrace();
				}
				metricRecorder.println(s);// print out the current strategy
				System.out.println("[INFO] Start " + s);				
				SemanticImportance si = new SemanticImportance(s, client, i, metricRecorder, prefix, graph);
				si.run();
			}
			
			// run strategies with only domain literate
			for(String s:strategies) {
				try {
					metricRecorder = new PrintWriter(new FileOutputStream(new File("files/" + s + "_DL_evaluation.txt"), true));
				} catch (FileNotFoundException e) {
					System.out.println("[ERROR] cannot create benchmark result file");
					e.printStackTrace();
				}
				metricRecorder.println(s + "_DL");// print out the current strategy
				System.out.println("[INFO] Start " + s + "_DL");
				SemanticImportance si = new SemanticImportance(s, client, i, metricRecorder, "dl", prefix, graph);
				si.run();
			}
			// run strategies with only eviction
			for(String s:strategies) {
				for(double v:cachePercentage){
					try {
						metricRecorder = new PrintWriter(new FileOutputStream(new File("files/" + s + "_" + v + "_evaluation.txt"), true));
					} catch (FileNotFoundException e) {
						System.out.println("[ERROR] cannot create benchmark result file");
						e.printStackTrace();
					}
					metricRecorder.println(s + "_E_" + v);// print out the current strategy
					System.out.println("[INFO] Start " + s + "_E_" + v);
					SemanticImportance si = new SemanticImportance(s, client, i, metricRecorder, v, prefix, graph);
					si.run();
				}
			}
			
			// run strategies with domain literate and eviction
			for(String s:strategies) {
				for(double v:cachePercentage) {
					try {
						metricRecorder = new PrintWriter(new FileOutputStream(new File("files/" + s + "_DL_" + v + "_evaluation.txt"), true));
					} catch (FileNotFoundException e) {
						System.out.println("[ERROR] cannot create benchmark result file");
						e.printStackTrace();
					}
					metricRecorder.println(s + "_DL_E_" + v);// print out the current strategy
					System.out.println("[INFO] Start " + s + "_DL_E_" + v);
					SemanticImportance si = new SemanticImportance(s, client, i, metricRecorder, "dl", v, prefix, graph);
					si.run();
				}
			}
		}		
		// close up
		metricRecorder.close();
		client.cleanUp();
	}
}
