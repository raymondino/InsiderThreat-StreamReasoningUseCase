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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.rio.RDFFormat;

import com.complexible.common.openrdf.model.Models2;
import com.complexible.common.rdf.model.Values;
import com.complexible.stardog.StardogException;
import com.tw.rpi.edu.si.utilities.SnarlClient;
import com.tw.rpi.edu.si.utilities.Window;

public class SemanticImportance {
	
	private String path; // streaming data path
	private String data; // each line in the streaming data
	private String prefix;
	private String lastGraphID;
	private String currentUserId;
	private String currentPC;
	private String currentGraphID;
	private String query;
	private Boolean window_start;
	private SnarlClient client;	
	private LinkedHashMap<String, ZonedDateTime> actionTimePair;
	private ArrayList<String> suspiciousActionList;
	private HashMap<String, Double> employeeTrust;
	private Window window;
	
	// to read data from file for stream simulation
	private BufferedReader br; 
	
	// constructor
	public SemanticImportance(String d, SnarlClient c, String p) {
		path = d; 
		client = c; 
		prefix = p; 
		lastGraphID = "";
		currentGraphID = "";
		window = new Window(); // a default window: size = 7days, step = 1day
		window_start = false;
		actionTimePair = new LinkedHashMap<String, ZonedDateTime>();
		suspiciousActionList = new ArrayList<String>();
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
		System.out.println("[INFO] reading the data");
		try {
			while((data = br.readLine()) != null) {
				String [] parts = data.split(" ");
				String s = parts[0];
				String p = parts[1];
				String o = parts[2];	
				
				// grab current PC
				if(p.equals(prefix + "isPerformedOnPC")) {
					currentPC = o;
					// unassigned PC annotation
					query = "select distinct ?pc "
							+ "where { graph <" + prefix + "pc> {"
							+ "<" + prefix + currentUserId + "> "
							+ "<" + prefix + "hasAccessToPC> ?pc}}";
					TupleQueryResult result = client.getANonReasoningConn().select(query).execute();
					String currentUserAssignedPC = null;
					while(result.hasNext()) {
						currentUserAssignedPC = result.next().getValue("pc").toString();					
					}
					// if current PC is not the user's assigned pc, continue to check if it's a shared pc
					if(currentUserAssignedPC != null && !currentUserAssignedPC.equals(currentPC)) {
						query = "ask { graph <" + prefix + "pc> { <" + currentUserAssignedPC +"> a <" + prefix + "SharedPC>.}}";
						// if currentPC is not an SharedPC, then annotate as:
						if(!client.getANonReasoningConn().ask(query).execute()) { 
							client.addStatement(Values.statement(Values.iri(s),Values.iri(prefix + "isPerformedOnUnassignedPC "), Values.iri(currentPC)));
						}
					}
				}				
				
				// read the data in
				if(o.contains("http")) { // if object is a URL	
					if(data.charAt(data.length()-1) != '.') { // if data has a time-stamp				
						// every action is added to a unique graph
						lastGraphID = currentGraphID;
						currentGraphID = o + "/graph";
						client.addModel(Models2.newModel(Values.statement(Values.iri(s), Values.iri(p), Values.iri(o))),currentGraphID);

						// check if an employee is in the employeeTrust HashMap
						currentUserId = s.substring(prefix.length(), prefix.length()+7);
						if(!employeeTrust.containsKey(currentUserId)) {
							// initially every employee's trust score is 1.0
							employeeTrust.put(currentUserId, 1.0);
						}
						
						// put action-timestamp pair into actionTimePair
						ZonedDateTime timestamp = ZonedDateTime.parse(parts[4]+"-05:00"); // EST time zone
						actionTimePair.put(currentGraphID, timestamp);
						
						// set window start time
						if(!window_start) {
							window.setStart(timestamp);
							window_start = true;
						}
						
						// constantly update the different action individuals for cardinality reasoning
						File[] files = new File[3];
						files[0] = new File("data/different-individuals/text1.txt");
						files[1] = new File("data/different-individuals/text2.txt");
						files[2] = new File("data/different-individuals/text3.txt");
						File outFile = new File("data/different-individuals/different-individuals.ttl");
						outFile.delete(); 		
						
						FileWriter fw1 = new FileWriter(files[0],true); //true appends the new data
						FileWriter fw2 = new FileWriter(files[1],true); 
						fw1.write(String.format("%s a owl:NamedIndividual .\n","<"+o+">"));
						fw2.write(String.format("   %s\n","<"+o+">"));
						fw1.close();
						fw2.close();
						
						// fire query when every action is read
						System.out.println("[info] querying... " + timestamp);
						fireQuery(files, outFile);
						
						// check if window is full
						if(timestamp.isAfter(window.getEnd())) { // if window is full
							evictData();							
							window.move();
							actionTimePair.put(currentGraphID, timestamp);
						}
					}
					else if(!currentGraphID.equals("")) {
						client.addModel(Models2.newModel(Values.statement(Values.iri(s), Values.iri(p), Values.iri(o))),currentGraphID);
					}					
					else { // add some data into default graph as they are not required to be in a named graph
						client.addStatement(Values.statement(Values.iri(s), Values.iri(p), Values.iri(o)));						
					}
				}
				else { // if object is a literal
					client.addModel(Models2.newModel(Values.statement(Values.iri(s), Values.iri(p), Values.literal(o))), currentGraphID);
				}
			}			
		} catch (IOException e) {
			System.out.println("[ERROR] cannot read the streaming file" + data);
			e.printStackTrace();
		}
	}
	
	// fire the query
	public void fireQuery(File[] files, File mergedFile) throws StardogException, FileNotFoundException {
		if(lastGraphID.equals("")) return;
		String q1 = "select distinct ?userid ?action from <"+prefix+"background> from <"+lastGraphID+"> where {?action <" + prefix + "hasActor> ?userid. ?action a <" + prefix + "SuspiciousLoginAction> }";
		String q2 = "select distinct ?userid ?action from <"+prefix+"background> from <"+lastGraphID+"> where {?action <" + prefix + "hasActor> ?userid. ?action a <" + prefix + "SuspiciousEmailSendAction> }";
		String q3 = "select distinct ?userid ?action from <"+prefix+"background> from <"+lastGraphID+"> where {?action <" + prefix + "hasActor> ?userid. ?action a <" + prefix + "SuspiciousFileCopyAction> }";
		String q4 = "select distinct ?userid ?action from <"+prefix+"background> from <"+lastGraphID+"> where {?action <" + prefix + "hasActor> ?userid. ?action a <" + prefix + "SuspiciousWWWUploadAction> }";
		TupleQueryResult result1 = client.getAReasoningConn().select(q1).execute();
		TupleQueryResult result2 = client.getAReasoningConn().select(q2).execute();
		TupleQueryResult result3 = client.getAReasoningConn().select(q3).execute();
		TupleQueryResult result4 = client.getAReasoningConn().select(q4).execute();
		while(result1.hasNext()) {
			BindingSet bs = result1.next();
			String u = bs.getValue("userid").toString(); // user id
			String a = bs.getValue("action").toString(); // action
			String afterhourquery = "ask from <"+ lastGraphID +"> {<" + a + "> a <" + prefix + "AfterHourAction>.}";
			String userAssignedPC = "select ?pc where {graph <" + prefix + "pc> { <" + u + "> <" + prefix + "hasAccessToPC> ?pc}}";

			System.out.println("[WARNING] suspicious login action detected:");
			System.out.println("          user id: " + bs.getValue("userid").toString());
			System.out.println("          action: " + bs.getValue("action").toString());
			System.out.println("                after hour: " + (client.getANonReasoningConn().ask(afterhourquery).execute()));
			System.out.println("          PC used: " + currentPC);
			System.out.println("          PC assigned: " + client.getANonReasoningConn().select(userAssignedPC).execute().next().getValue("pc").toString());
			System.out.println("          timestamp: " + actionTimePair.get(lastGraphID));
		}
		while(result2.hasNext()) {
			BindingSet bs = result2.next();
			suspiciousActionList.add(lastGraphID);
			System.out.println("[WARNING] suspicious email send action detected:");
			System.out.println("          user id: " + bs.getValue("userid").toString());
			System.out.println("          action: " + bs.getValue("action").toString());
			System.out.println("          timestamp: " + actionTimePair.get(bs.getValue("action").toString()));
			dataExfiltrationQuery(files, mergedFile);
		}
		while(result3.hasNext()) {
			BindingSet bs = result3.next();
			suspiciousActionList.add(lastGraphID);
			System.out.println("[WARNING] suspicious file copy action detected:");
			System.out.println("          user id: " + bs.getValue("userid").toString());
			System.out.println("          action: " + bs.getValue("action").toString());
			System.out.println("          timestamp: " + actionTimePair.get(bs.getValue("action").toString()+"/graph"));
			dataExfiltrationQuery(files, mergedFile);
		}
		while(result4.hasNext()) {
			BindingSet bs = result4.next();
			suspiciousActionList.add(lastGraphID);
			System.out.println("[WARNING] suspicious www upload action detected:");
			System.out.println("          user id: " + bs.getValue("userid").toString());
			System.out.println("          action: " + bs.getValue("action").toString());
			System.out.println("          timestamp: " + actionTimePair.get(bs.getValue("action").toString()+"/graph"));
			dataExfiltrationQuery(files, mergedFile);
		}
	}
	public void dataExfiltrationQuery(File[] files, File mergedFile) throws StardogException, FileNotFoundException {
		// merge different individual files
		mergeFiles(files,mergedFile); 
		// load different individual files to db
		client.getANonReasoningConn().begin();
		client.getANonReasoningConn().add().io().context(Values.iri(prefix+"different-individuals")).format(RDFFormat.TURTLE).stream(new FileInputStream("data/different-individuals/different-individuals.ttl"));
		client.getANonReasoningConn().commit();
		// construct and execute the query
//		String q = "select distinct ?userid ";
//		for(String i:suspiciousActionList) {
//			q += ("from <" + i + "> ");
//		}
//		q += "from <"+prefix+"background> from <"+prefix+"different-individuals> where { ?event a <"+prefix+"DataExfiltrationEvent>. ?userid <"+prefix+"isInvolvedIn> ?event.}";
		String q = "select distinct ?userid where { ?event a <"+prefix+"DataExfiltrationEvent>. ?userid <"+prefix+"isInvolvedIn> ?event.}";
		TupleQueryResult result = client.getAReasoningConn().select(q).execute();
		while(result.hasNext()) {
			System.out.println("[Threatening] Data Exfiltraion Event Detected!");
			System.out.println("              potential threatening insider: " + result.next().getValue("userid").toString());			
		}
		
		// delete different-individuals graph
		client.getANonReasoningConn().update("drop graph <" + prefix + "different-individuals>").execute();
	}
	
	// evict the data
	public void evictData() {
		Iterator<Entry<String, ZonedDateTime>> itr = actionTimePair.entrySet().iterator();
		ArrayList<String> toDelete = new ArrayList<String>();
		while(itr.hasNext()) {
			Map.Entry<String, ZonedDateTime> entry = itr.next();
			if(entry.getValue().isAfter(window.getStart().plus(window.getStep()))) {
				break;
			}
			else {
				toDelete.add(entry.getKey());
			}
		}
		String dropQuery = "";
		for(String i:toDelete) {
			dropQuery += "drop graph <" + i + ">;";
		}
		// perform drop query to delete data
		if(!dropQuery.equals("")) {
			client.getANonReasoningConn().update(dropQuery.substring(0, dropQuery.length() - 1)).execute();	
		}
	}
	
	// merge different individuals file into one turtle file 
	public static void mergeFiles(File[] files, File mergedFile) {
		FileWriter fstream = null;
		BufferedWriter out = null;
		try {
			fstream = new FileWriter(mergedFile, true);
			out = new BufferedWriter(fstream);
		} catch (IOException e1) { e1.printStackTrace(); }
		
		for (File f : files) {
			System.out.println("[INFO]: individual files merging: " + f.getName());
			FileInputStream fis;
			try {
				fis = new FileInputStream(f);
				BufferedReader in = new BufferedReader(new InputStreamReader(fis));
				String aLine;
				while ((aLine = in.readLine()) != null) {
					out.write(aLine); out.newLine();
				}
				in.close();
			} catch (IOException e) { e.printStackTrace();}
		}
		try {out.close(); } catch (IOException e) { e.printStackTrace(); }
	}
}