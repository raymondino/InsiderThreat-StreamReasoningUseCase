package com.tw.rpi.edu.si.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;

import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.rio.RDFFormat;

import com.complexible.common.rdf.model.Values;
import com.complexible.stardog.StardogException;

public class Window {
	private static String prefix = "http://tw.rpi.edu/ontology/DataExfiltration/";

	private Period size;
	private Period step;
	private ZonedDateTime latestActionTS;
	private Action latestAction;
	private String latestActionGraphID;
	private ZonedDateTime start; // window start
	private ZonedDateTime end; // window end
	SnarlClient client;
	
	// pair of action graph id and timestamp
	private LinkedHashMap<String, ZonedDateTime> content; 
	// rank actions
	private PriorityQueue<Action> actions; 
	
	// constructor
	public Window() {
		size = Period.ofDays(7);
		step = Period.ofDays(1);
		latestActionTS = null;
		latestAction = null;
		latestActionGraphID = "";
		content = new LinkedHashMap<String, ZonedDateTime>();
		actions = new PriorityQueue<Action>();
	}	
	public Window(SnarlClient c) {
		size = Period.ofDays(7);
		step = Period.ofDays(1);
		latestActionTS = null;
		latestAction = null;
		latestActionGraphID = "";
		content = new LinkedHashMap<String, ZonedDateTime>();
		actions = new PriorityQueue<Action> ();
		client = c;
	}
	
	// assessor
	public ZonedDateTime getStart() { return start; }
	public ZonedDateTime getEnd() { return end; }
	public Period getStep() {return step;}
	
	// modifier
	public void setStep(int s) {step = Period.ofDays(s);}
	public void setSize(int s) {size = Period.ofDays(s);end = start.plus(size);}
	public void setStart(ZonedDateTime s) {start = s; end = start.plus(size);}
	
	// function: window moves 1 step forward
	public void move() { 
		start = start.plus(step); 
		end = end.plus(size);
	} 
	// function: window loads data
	public void load(String graphid, ZonedDateTime ts, Action a) {
		latestActionTS = ts;
		latestAction = a;
		latestActionGraphID = graphid;
		content.put(graphid, ts);
		actions.add(a);
	}
	// function: window process data
	public void process() {
		// if window is not full
		if(latestActionTS.isBefore(end)) {
			query();
		}
		else {
			// rank
			evict();
		}
	}
	// function: window evicts data
	public void evict() {
		System.out.println("[evict]");
		Iterator<Entry<String, ZonedDateTime>> itr = content.entrySet().iterator();
		ArrayList<String> toDelete = new ArrayList<String>();
		while(itr.hasNext()) {
			Map.Entry<String, ZonedDateTime> entry = itr.next();
			if(entry.getValue().isAfter(start.plus(step))) {
				break;
			}
			else {
				toDelete.add(entry.getKey());
			}
		}
		String dropQuery = "";
		for(String i:toDelete) {
			dropQuery += "drop graph <" + i + ">;";
			content.remove(i);
			for(Action a:actions) {
				if(a.getActionID().equals(i.substring(prefix.length()))) {
					actions.remove(a);
					break;
				}
			}
		}

		// perform drop query to delete data
		if(!dropQuery.equals("")) {
			client.getANonReasoningConn().update(dropQuery.substring(0, dropQuery.length() - 1)).execute();	
		}
	}
	
	// suspicious action query
	private void query() {
		String suspiciousActionQuery = "select distinct ?action from <"+prefix+"background> from <"+latestActionGraphID+"> where {?action a <" + prefix + "SuspiciousAction>.}";
		TupleQueryResult result = client.getAReasoningConn().select(suspiciousActionQuery).execute();
		// if suspiciousActionQuery has result, that means latestAction is a suspicious action
		if(result.hasNext()) {
			// reduce user's trust score
			latestAction.getUser().reduceTrustScore();
			
			System.out.println("*************************************");
			System.out.println("[WARNING] suspicious action detected:");
			System.out.println("          timestamp:  " + latestAction.getTimestamp());
			System.out.println("          action:     " + latestAction.getActionID());
			System.out.println("          after hour action: " + latestAction.getAfterHourAction());
			System.out.println("          activity:  " + latestAction.getActivity());
			if(latestAction.getActivity().contains("WWW")) {
				System.out.println("          url:  " + latestAction.getUrl());
				System.out.println("          url domain type: " + latestAction.getUrlDomainType());				
			}
			else if (latestAction.getActivity().contains("Email")) {
				System.out.println("          email from: " + latestAction.getEmailFrom());
				for(String i: latestAction.getEmailRecipients()) {
					System.out.print("          email to: " + i);
					if(!i.contains("@dtaa.com")) {
						System.out.print(" (external address)");
					}
					System.out.println();
				}
				System.out.println("          email attachment: " + latestAction.getEmailAttachment());
				System.out.println("          email attachment decoy file: " + latestAction.getEmailAttachmentDecoyFile());
			} 
			else if (latestAction.getActivity().contains("File")) {
				System.out.println("          file name: " + latestAction.getFileName());
				System.out.println("          decoy file: " + latestAction.getFileADecoyFile());
				System.out.println("          from removable media: " + latestAction.getFromRemovableMedia());
				System.out.println("          to removable media: " + latestAction.getToRemovableMedia());
			}
			System.out.println("          action provenance socre: " + latestAction.getProvenanceScore());
			System.out.println();
			System.out.println("          action performed pc: " + latestAction.getPc());
			System.out.println("          user assigned pc: " + latestAction.getUser().getPC());
			System.out.println("          user id:    " + latestAction.getUser().getID());
			System.out.println("          user name:  " + latestAction.getUser().getName());
			System.out.println("          user role:  " + latestAction.getUser().getRole());
			System.out.println("          user team:  " + latestAction.getUser().getTeam());
			System.out.println("          user supervisor: " + latestAction.getUser().getSupervisor());
			System.out.println("          user resignation: " + latestAction.getUser().getResinationStatus());
			System.out.println("          user excessive removable media: " + latestAction.getUser().getExcessiveRemovableDiskUser());
			System.out.println("          user trust score:    " + latestAction.getUser().getTrustScore());
			System.out.println("*************************************");

			// move this action to suspicious action graphs
			client.getANonReasoningConn().update("add <"+latestActionGraphID+"> to <"+prefix+"suspicious>").execute();
			dataExfiltraion();
		}
	}
	
	// data exfiltration query
	private void dataExfiltraion() {
		// create file for different individuals update
		File[] files = new File[3];
		files[0] = new File("data/different-individuals/text1.txt");
		files[1] = new File("data/different-individuals/text2.txt");
		files[2] = new File("data/different-individuals/text3.txt");
		File mergedFile = new File("data/different-individuals/different-individuals.ttl");
		try {
			updateDiffIndividuals(files, mergedFile, latestAction.getActionID());
		} catch (IOException e) {
			e.printStackTrace();
		}
		// merge different individual files
		mergeFiles(files,mergedFile); 
		// load different individual files to db
		client.getANonReasoningConn().begin();
		try {
			client.getANonReasoningConn().add().io().context(Values.iri(prefix+"different-individuals")).format(RDFFormat.TURTLE).stream(new FileInputStream("data/different-individuals/different-individuals.ttl"));
		} catch (StardogException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		client.getANonReasoningConn().commit();
		// construct and execute the query
		String q = "select distinct ?userid from <"+prefix+"suspicious> "+"from <"+prefix+"background> from <"+prefix+"different-individuals> from <"+prefix+"actor-event> where { ?userid a <"+prefix+"PotentialThreateningInsider>.}";
		TupleQueryResult result = client.getAReasoningConn().select(q).execute();
		while(result.hasNext()) {
			BindingSet bs = result.next();
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.println("[Threatening] Data Exfiltraion Event Detected!");
			System.out.println("              potential threatening insider: " + bs.getValue("userid").toString());				
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.println("*************************************");
		}
		// delete different-individuals graph
		client.getANonReasoningConn().update("drop graph <" + prefix + "different-individuals>").execute();
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
		} catch (IOException e1) { 
			e1.printStackTrace(); 
		}		
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
			} catch (IOException e) { 
				e.printStackTrace();
			}
		}
		try { 
			out.close(); 
		} catch (IOException e) { 
			e.printStackTrace(); 
		}
	}
}
