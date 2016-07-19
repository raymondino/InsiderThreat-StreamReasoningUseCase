package com.tw.rpi.edu.si.strategy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.complexible.common.rdf.model.Values;
import com.tw.rpi.edu.si.utilities.SnarlClient;
import com.tw.rpi.edu.si.utilities.Window;

public class SemanticImportance {
	
	private String path; // streaming data path
	private String data; // each line in the streaming data
	private String prefix;
	private String currentUserId;
	private SnarlClient client;	
	private LinkedHashMap<String, ZonedDateTime> actionTimePair;
	private HashMap<String, Double> employeeTrust;
	private Window window;
	
	// to read data from file for stream simulation
	private BufferedReader br; 
	
	// constructor
	public SemanticImportance(String d, SnarlClient c, String p) {
		path = d; 
		client = c; 
		prefix = p; 
		window = new Window(); // an empty window 
		window.setSize(7); // set window size to be 7 days
		window.setStep(1); // set window step to be 1 day
		actionTimePair = new LinkedHashMap<String, ZonedDateTime>();
		employeeTrust = new HashMap<String, Double>();
		try {
			this.br = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(path))));
		} catch (FileNotFoundException e1) {
			System.out.println("[ERROR]: streaming data path is invalid:" + path);
			e1.printStackTrace();
		}
	}
	
	// run function
	public void run() {
		// read the streaming data action by action
		try {
			while((data = br.readLine()) != null) {
				String [] parts = data.split(" ");
				String s = parts[0];
				String p = parts[1];
				String o = parts[2];				
				if(o.contains("http")) { // if object is a url
					client.addStatement(Values.statement(Values.iri(s), Values.iri(p), Values.iri(o)));
					if(data.charAt(data.length()-1) != '.') { // if data has a time-stamp
						
						// check if an employee is in the employeeTrust HashMap
						currentUserId = s.substring(prefix.length(), prefix.length()+7);
						if(!employeeTrust.containsKey(currentUserId)) {
							// initially every employee's trust score is 1.0
							employeeTrust.put(currentUserId, 1.0);
						}
						
						// put action-timestamp pair into actionTimePair
						ZonedDateTime timestamp = ZonedDateTime.parse(parts[4]+"-05:00"); // EST time zone
						actionTimePair.put(o, timestamp);
						
						// constantly update the different action individuals for cardinality reasoning
						String filePath1 = "data/different-individuals/text1.txt";
						String filePath2 = "data/different-individuals/text2.txt";
						String filePath3 = "data/different-individuals/text3.txt";
						String outFilePath = "data/different-individuals/different-individuals.ttl";
						update(filePath1, filePath2, filePath3, outFilePath, "<"+o+">");
					}
				}
				else { // if object is a literal
					client.addStatement(Values.statement(Values.iri(s), Values.iri(p), Values.literal(o)));
				}
			}			
		} catch (IOException e) {
			System.out.println("[ERROR] cannot read the streaming file" + data);
			e.printStackTrace();
		}
		
		// unassigned pc annotation
		String query = "select distinct ?pc "
				+ "where { graph <" + prefix + "pc> {"
				+ "<" + prefix + currentUserId + "> "
				+ "<" + prefix + "hasAccessToPC> ?pc}}";
		// todo: 
		// check ?pc equals to current pc <-- need to record from the streaming data
		// if not match, annotate: action isPerformedOnUnassignedPC ?pc.
	}
	
	// update different individuals
	public static void update(String filePath1, String filePath2, String filePath3,	
			String outFilePath, String action) {
		File[] files = new File[3];
		File outFile = null;
		try {
			files[0] = new File(filePath1);
			files[1] = new File(filePath2);
			files[2] = new File(filePath3);
			outFile = new File(outFilePath);
		}
		catch(Exception e) { e.printStackTrace(); }
		try { outFile.delete(); }
		catch(Exception e) { e.printStackTrace(); }
		addContents(files[0],files[1],action);
		mergeFiles(files,outFile);
	}
	
	// add different individuals content
	public static void addContents(File f1, File f2, String action) {
		try {
			FileWriter fw1 = new FileWriter(f1,true); //true appends the new data
			FileWriter fw2 = new FileWriter(f2,true); 
			fw1.write(String.format("%s a owl:NamedIndividual .\n",action));
			fw2.write(String.format("   %s\n",action));
			fw1.close();
			fw2.close();
		}
		catch(IOException ioe) { 
			System.err.println("IOException: " + ioe.getMessage());
		}
	}
	
	// merge different individuals file into one turtle file <-- merge file only when query is executed (need to update)
	public static void mergeFiles(File[] files, File mergedFile) {
		FileWriter fstream = null;
		BufferedWriter out = null;
		try {
			fstream = new FileWriter(mergedFile, true);
			out = new BufferedWriter(fstream);
		} catch (IOException e1) { e1.printStackTrace(); }
		for (File f : files) {
			System.out.println("merging: " + f.getName());
			FileInputStream fis;
			try {
				fis = new FileInputStream(f);
				BufferedReader in = new BufferedReader(new InputStreamReader(fis));

				String aLine;
				while ((aLine = in.readLine()) != null) {
					out.write(aLine);
					out.newLine();
				}
				in.close();
			} catch (IOException e) { e.printStackTrace();}
		}
		try {
			out.close();
		} catch (IOException e) { e.printStackTrace(); }
	}	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
