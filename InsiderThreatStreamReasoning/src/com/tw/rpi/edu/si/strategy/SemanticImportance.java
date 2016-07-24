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
	private HashMap<String, Double> employeeTrust;
	private Window window;	
	private BufferedReader br; 	// to read data from file for stream simulation
	
	// constructor
	public SemanticImportance(String d, SnarlClient c, String p) {
		path = d; 
		client = c; 
		prefix = p; 
		lastGraphID = "";
		currentGraphID = "";
		window = new Window(); // a default window: size = 2 days, step = 1day
		window_start = false;
		actionTimePair = new LinkedHashMap<String, ZonedDateTime>();
		//suspiciousActionList = new ArrayList<String>();
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
						currentGraphID = prefix + "graph/" + o.substring(prefix.length());
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
						
						// create file for different individuals update
						File[] files = new File[3];
						files[0] = new File("data/different-individuals/text1.txt");
						files[1] = new File("data/different-individuals/text2.txt");
						files[2] = new File("data/different-individuals/text3.txt");
						File outFile = new File("data/different-individuals/different-individuals.ttl");
						
						// fire query when every action is read
						System.out.println("[query] " + o.substring(prefix.length()) + " " + timestamp);
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
						client.addModel(Models2.newModel(Values.statement(Values.iri(s), Values.iri(p), Values.iri(o))), prefix+"actor-event");						
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
	private void fireQuery(File[] files, File mergedFile) throws StardogException, IOException {
		if(lastGraphID.equals("")) return;
		String q = "select distinct ?userid ?action from <"+prefix+"background> from <"+lastGraphID+"> where {?action <" + prefix + "hasActor> ?userid. ?action a <" + prefix + "SuspiciousAction>.}";
		TupleQueryResult r = client.getAReasoningConn().select(q).execute();
		while(r.hasNext()) {
			BindingSet bs = r.next();
			client.getANonReasoningConn().update("add <"+lastGraphID+"> to <"+prefix+"suspicious>").execute();
			String u = bs.getValue("userid").toString(); // user id
			String a = bs.getValue("action").toString(); // action
			System.out.println("[WARNING] suspicious action detected:");
			System.out.println("          action:     " + bs.getValue("action").toString().substring(prefix.length()));
			System.out.println("          timestamp:  " + actionTimePair.get(lastGraphID));
			System.out.println("          user id:    " + bs.getValue("userid").toString().substring(prefix.length()));
			String statusquery = null;
			if(actionTimePair.get(lastGraphID).getMonthValue() / 10 == 0) {
				statusquery = "select ?name ?role ?team ?supervisor from <"+prefix+actionTimePair.get(lastGraphID).getYear()+"-0"+actionTimePair.get(lastGraphID).getMonthValue()+"> "
						+ "where {<"+u+"> <"+prefix+"hasName> ?name; <"+prefix + "hasRole> ?role; <"+prefix+"hasTeam> ?team; <"+prefix + "hasSupervisor> ?supervisor.}";
			}
			else {
				 statusquery = "select ?name ?role ?team ?supervisor from <"+prefix+actionTimePair.get(lastGraphID).getYear()+"-"+actionTimePair.get(lastGraphID).getMonthValue()+"> "
							+ "where {<"+u+"> <"+prefix+"hasName> ?name; <"+prefix + "hasRole> ?role; <"+prefix+"hasTeam> ?team; <"+prefix + "hasSupervisor> ?supervisor.}";
			}
			TupleQueryResult r1 = client.getANonReasoningConn().select(statusquery).execute();
			if(r1.hasNext()) {
				BindingSet bs1 = r1.next();
				System.out.println("          user name:  " + bs1.getValue("name").toString().substring(prefix.length()));
				System.out.println("          role:       " + bs1.getValue("role").toString().substring(prefix.length()));
				System.out.println("          team:       "+bs1.getValue("team").toString());
				System.out.println("          supervisor: " + bs1.getValue("supervisor").toString().substring(prefix.length()));
			}
			else {
				System.out.println("          this user has already resigned.");
			}
			String afterhourquery = "ask from <"+ lastGraphID +"> {<" + a + "> a <" + prefix + "AfterHourAction>.}";
			String userAssignedPC = "select ?pc where {graph <" + prefix + "pc> { <" + u + "> <" + prefix + "hasAccessToPC> ?pc}}";
			System.out.println("          details:");
			System.out.println("                  after hour: " + (client.getANonReasoningConn().ask(afterhourquery).execute()));
			System.out.println("                  PC logon:   " + currentPC.substring(prefix.length()));
			System.out.println("                  PC assigned:" + client.getANonReasoningConn().select(userAssignedPC).execute().next().getValue("pc").toString().substring(prefix.length()));			
			if (lastGraphID.contains("email_")) {
				String activity = "select distinct ?o from <"+lastGraphID+"> where {?s a ?o.}";
				String from = "select distinct ?o from <"+lastGraphID+"> where {?s <"+prefix+"from> ?o.}";
				String to = "select distinct ?o from <"+lastGraphID+"> where {?s <"+prefix+"to> ?o.}";
				String cc = "select distinct ?o from <"+lastGraphID+"> where {?s <"+prefix+"cc> ?o.}";
				String bcc = "select distinct ?o from <"+lastGraphID+"> where {?s <"+prefix+"bcc> ?o.}";
				String attachment = "select distinct ?o from <"+lastGraphID+"> where {?s <"+prefix+"hasEmailAttachment> ?o.}";
				TupleQueryResult re = client.getANonReasoningConn().select(from).execute();
				while(re.hasNext()) {
					BindingSet x = re.next();
					System.out.println("                  from        :" + x.getValue("o").toString().substring(prefix.length()));
				}
				re = client.getANonReasoningConn().select(to).execute();
				while(re.hasNext()) {
					BindingSet x = re.next();
					System.out.println("                  to          :" + x.getValue("o").toString().substring(prefix.length()));					
				}
				re = client.getANonReasoningConn().select(cc).execute();
				while(re.hasNext()) {
					BindingSet x = re.next();
					System.out.println("                  cc          :" + x.getValue("o").toString().substring(prefix.length()));					
				}
				re = client.getANonReasoningConn().select(bcc).execute();
				while(re.hasNext()) {
					BindingSet x = re.next();
					System.out.println("                  bcc         :" + x.getValue("o").toString().substring(prefix.length()));					
				}				
				re = client.getANonReasoningConn().select(attachment).execute();
				while(re.hasNext()) {
					BindingSet x = re.next();
					System.out.print("                    attachment  :" + x.getValue("o").toString().substring(prefix.length()));
					if(client.getANonReasoningConn().ask("ask from <"+prefix+"decoy> {<"+x.getBinding("o").toString()+"> a <" + prefix + "DecoyFile>}").execute()) {
						System.out.println(" <-- a decoy file");						
					}
				}
				re = client.getANonReasoningConn().select(activity).execute();
				while(re.hasNext()) {
					BindingSet x = re.next();
					if(x.getValue("o").toString().contains("Action")) {
						System.out.println("                  activity:   " + x.getValue("o").toString().substring(prefix.length()));						
					}
				}
			}
			else if (lastGraphID.contains("http_")) {
				String url = "select distinct ?url ?dn from <"+lastGraphID+"> where {?s <"+prefix+"hasURL> ?url. ?url <"+prefix+"whoseDomainNameIsA> ?dn.}";
				String activity = "select distinct ?o from <"+lastGraphID+"> where {?s a ?o.}";
				TupleQueryResult re = client.getANonReasoningConn().select(url).execute();
				while(re.hasNext()){
					BindingSet x = re.next();
					System.out.println("                url         :" + x.getValue("url").toString());
					System.out.println("                url domain  :" + x.getBinding("dn").toString().substring(prefix.length()));
				}
				re = client.getANonReasoningConn().select(activity).execute();
				while(re.hasNext()) {
					BindingSet x = re.next();
					if(x.getValue("o").toString().contains("WWW")) {
						System.out.println("                  activity:   " + x.getValue("o").toString().substring(prefix.length()));						
					}
				}				
			}
			else if (lastGraphID.contains("file_")) {
				String activity = "select distinct ?o from <"+lastGraphID+"> where {?s a ?o.}";
				String file = "select distinct ?fn from <"+lastGraphID+"> where {?s <"+prefix+"hasFile> ?fn.}";
				String filetype = "select distinct ?type from <"+lastGraphID+"> where {?s <"+prefix+"hasFile> ?fn. ?fn a ?type}";
				TupleQueryResult re = client.getANonReasoningConn().select(file).execute();
				while(re.hasNext()){
					BindingSet x = re.next();
					System.out.print("                    file name :" + x.getValue("fn").toString().substring(prefix.length()));
					if(client.getANonReasoningConn().ask("ask from <"+prefix+"decoy> {<"+x.getBinding("o").toString()+"> a <" + prefix + "DecoyFile>}").execute()) {
						System.out.println(" <-- a decoy file");						
					}
				}
				re = client.getANonReasoningConn().select(filetype).execute();
				while(re.hasNext()){
					BindingSet x = re.next();
					String type = x.getValue("type").toString().substring(prefix.length());
					if(type.equals("NotFileToRemovableMedia")) {
						System.out.println("                    copied from PC: no");
					}
					else if (type.equals("NotFileFromRemovableMedia")) {
						System.out.println("                    copied from usb drive: no");
					}
					else if (type.equals("FileToRemovableMedia")) {
						System.out.println("                    copied from PC: yes");
					}
					else {
						System.out.println("                    copied from usb drive: yes");
					}
				}
				re = client.getANonReasoningConn().select(activity).execute();
				while(re.hasNext()) {
					BindingSet x = re.next();
					if(x.getValue("o").toString().contains("Action")) {
						System.out.println("                  activity:   " + x.getValue("o").toString().substring(prefix.length()));						
					}
				}
			}
			else if (lastGraphID.contains("device_")){
				String activity = "select distinct ?o from <"+lastGraphID+"> where {?s a ?o.}";
				TupleQueryResult re = client.getANonReasoningConn().select(activity).execute();
				while(re.hasNext()) {
					BindingSet x = re.next();
					if(x.getValue("o").toString().contains("Action")) {
						System.out.println("                  activity:   " + x.getValue("o").toString().substring(prefix.length()));						
					}
				}
			}
			updateDiffIndividuals(files, mergedFile, lastGraphID.substring((prefix+"graph/").length()));
			dataExfiltrationQuery(files, mergedFile);
		}
	}
	private void dataExfiltrationQuery(File[] files, File mergedFile) throws StardogException, FileNotFoundException {
		// merge different individual files
		mergeFiles(files,mergedFile); 
		// load different individual files to db
		client.getANonReasoningConn().begin();
		client.getANonReasoningConn().add().io().context(Values.iri(prefix+"different-individuals")).format(RDFFormat.TURTLE).stream(new FileInputStream("data/different-individuals/different-individuals.ttl"));
		client.getANonReasoningConn().commit();
		// construct and execute the query
		String q = "select distinct ?userid ";
		q += "from <"+prefix+"suspicious> "+"from <"+prefix+"background> from <"+prefix+"different-individuals> from <"+prefix+"actor-event> where { ?userid a <"+prefix+"PotentialThreateningInsider>.}";
		TupleQueryResult result = client.getAReasoningConn().select(q).execute();
		while(result.hasNext()) {
			BindingSet bs = result.next();
			System.out.println("[Threatening] Data Exfiltraion Event Detected!");
			System.out.println("              potential threatening insider: " + bs.getValue("userid").toString());				
		}
		// delete different-individuals graph
		client.getANonReasoningConn().update("drop graph <" + prefix + "different-individuals>").execute();
	}
	
	// evict the data
	private void evictData() {
		System.out.println("[evict]");
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
			actionTimePair.remove(i);
		}
		// perform drop query to delete data
		if(!dropQuery.equals("")) {
			client.getANonReasoningConn().update(dropQuery.substring(0, dropQuery.length() - 1)).execute();	
		}
	}
	
	// write different individuals
	private void updateDiffIndividuals(File[] files, File mergedFile, String action) throws IOException{
		mergedFile.delete();
		FileWriter fw1 = new FileWriter(files[0],true); //true appends the new data
		FileWriter fw2 = new FileWriter(files[1],true); 
		fw1.write(String.format("%s a owl:NamedIndividual .\n","<"+prefix + action+">"));
		fw2.write(String.format("   %s\n","<"+prefix + action+">"));
		fw1.close();
		fw2.close();
	}
	
	// merge different individuals file into a single turtle file 
	private void mergeFiles(File[] files, File mergedFile) {
		FileWriter fstream = null;
		BufferedWriter out = null;
		try {
			fstream = new FileWriter(mergedFile, true);
			out = new BufferedWriter(fstream);
		} catch (IOException e1) { e1.printStackTrace(); }		
		for (File f : files) {
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