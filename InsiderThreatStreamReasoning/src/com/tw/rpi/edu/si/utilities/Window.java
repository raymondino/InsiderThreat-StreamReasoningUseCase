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
	private Action actionBeingQueried;
	private ZonedDateTime start; // window start
	private ZonedDateTime end; // window end
	private Boolean window_start; // flag for window start
	private long totalActionProcessTime; // total time up till now to process all actions
	private Integer actionCounter; // total action number so far
	
	SnarlClient client;
	FileWriter writeSuspiciousAction;
	
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
		actionBeingQueried = null;
		content = new LinkedHashMap<String, ZonedDateTime>();
		actions = new PriorityQueue<Action>();
		writeSuspiciousAction = null;
		window_start = false;
	}	
	public Window(SnarlClient c) {
		size = Period.ofDays(7);
		step = Period.ofDays(1);
		latestActionTS = null;
		latestAction = null;
		actionBeingQueried = null;
		content = new LinkedHashMap<String, ZonedDateTime>();
		actions = new PriorityQueue<Action> ();
		client = c;
		writeSuspiciousAction = null;
		window_start = false;
		totalActionProcessTime = (long) 0.0;
		actionCounter = 0;
	}
	
	// assessor
	public ZonedDateTime getStart() { return start; }
	public ZonedDateTime getEnd() { return end; }
	public Period getStep() {return step;}
	
	// modifier
	public void setStep(int s) {step = Period.ofDays(s);}
	public void setSize(int s) {size = Period.ofDays(s);}
	public void setStart(ZonedDateTime s) {start = s; end = start.plus(size);}
	
	// function: window loads data
	public void load(String graphid, ZonedDateTime ts, Action a) {
		actionCounter ++;
		if(!window_start) {
			setStart(ts);
			window_start = true;
		}
		System.out.print("[load] " + a.getActionID() + " - " + ts + " ");
		latestActionTS = ts;
		latestAction = a;
		content.put(graphid, ts);
		actions.add(a);
	}
	
	// function: window process data
	public void process() {
		// if window is not full
		if(latestActionTS.isBefore(end)) {
			// record action process time
			long actionProcessStartTime = System.currentTimeMillis();
			String filename = "suspiciousActionList_windowSize-" + this.size.getDays() ;
			// if actions are ranked by provenance score
			if(latestAction.isRankByProv()) {
				try {
					// write suspicious action list to file
					File suspiciousActionList = new File("data/result/"+filename +"_prov.txt");
					suspiciousActionList.delete();
					writeSuspiciousAction = new FileWriter(suspiciousActionList, true);	
				} catch (IOException e) {
					e.printStackTrace();
				}
				while(actions.peek().getProvenanceScore() > 0) {
					actionBeingQueried = actions.poll();
					System.out.print("[query] ");
					query(actionBeingQueried.getActionGraphID());
				}
			}
			// if actions are ranked by trust score
			else if (latestAction.isRankByTrust()) {
				try {
					// write suspicious action list to file
					File suspiciousActionList = new File("data/result/"+filename +"_trust.txt");
					suspiciousActionList.delete();
					writeSuspiciousAction = new FileWriter(suspiciousActionList, true);	
				} catch (IOException e) {
					e.printStackTrace();
				}
				while(actions.peek().getUser().getTrustScore() < 0) {
					actionBeingQueried = actions.poll();
					System.out.println("[query] ");
					query(actionBeingQueried.getActionGraphID());
				}
			}
			totalActionProcessTime += (System.currentTimeMillis() - actionProcessStartTime);
			System.out.println(totalActionProcessTime / actionCounter + "ms");
		}
		else {
			System.out.println();
			System.out.println("[evict]");
			evict();
			System.out.println("[window moves]");
			move();
		}
	}
	
	// function: window evicts data
	// function: window moves 1 step forward
	public void move() { 
		start = start.plus(step); 
		end = end.plus(step);
	} 
	
	// function: window evicts
	private void evict() {
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
				if(a.getActionID().equals(i.substring((prefix+"graph/").length()))) {
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
	private void query(String actionGraphID) {
		String jobHuntingActionQuery = "ask from <"+prefix+"background> from <"+actionGraphID+"> where {?action a <" + prefix + "JobHuntingAction>.}";
		if(client.getAReasoningConn().ask(jobHuntingActionQuery).execute()) {
			actionBeingQueried.getUser().reduceTrustScore();
			System.out.print("job hunting action detected ");
		} 
		String suspiciousActionQuery = "select distinct ?action from <"+prefix+"background> from <"+actionGraphID+"> where {?action a <" + prefix + "SuspiciousAction>.}";
		TupleQueryResult result = client.getAReasoningConn().select(suspiciousActionQuery).execute();
		// if suspiciousActionQuery has result, that means actionBeingQueried is a suspicious action
		if(result.hasNext()) {
			// reduce user's trust score
			actionBeingQueried.getUser().reduceTrustScore();
			System.out.println();
			System.out.println("*************************************");
			System.out.println("[WARNING] suspicious action detected:");
			System.out.println("          timestamp: " + actionBeingQueried.getTimestamp());
			System.out.println("          action: " + actionBeingQueried.getActionID());
			System.out.println("          after hour action: " + actionBeingQueried.getAfterHourAction());
			System.out.println("          activity: " + actionBeingQueried.getActivity());
			if(actionBeingQueried.getActivity().contains("WWW")) {
				System.out.println("          url: " + actionBeingQueried.getUrl());
				System.out.println("          url domain type: " + actionBeingQueried.getUrlDomainType());				
			}
			else if (actionBeingQueried.getActivity().contains("Email")) {
				System.out.println("          email from: " + actionBeingQueried.getEmailFrom());
				for(String i: actionBeingQueried.getEmailRecipients()) {
					System.out.print("          email to: " + i);
					if(!i.contains("@dtaa.com")) {
						System.out.print(" (external address)");
					}
					System.out.println();
				}
				System.out.println("          email attachment: " + actionBeingQueried.getEmailAttachment());
				System.out.println("          email attachment decoy file: " + actionBeingQueried.getEmailAttachmentDecoyFile());
			} 
			else if (actionBeingQueried.getActivity().contains("File")) {
				System.out.println("          file name: " + actionBeingQueried.getFileName());
				System.out.println("          decoy file: " + actionBeingQueried.getFileADecoyFile());
				System.out.println("          from removable media: " + actionBeingQueried.getFromRemovableMedia());
				System.out.println("          to removable media: " + actionBeingQueried.getToRemovableMedia());
			}
			System.out.println("          action provenance socre: " + actionBeingQueried.getProvenanceScore());
			System.out.println();
			System.out.println("          action performed pc: " + actionBeingQueried.getPc());
			System.out.println("          user assigned pc: " + actionBeingQueried.getUser().getPC());
			System.out.println("          user id: " + actionBeingQueried.getUser().getID());
			System.out.println("          user name: " + actionBeingQueried.getUser().getName());
			System.out.println("          user role: " + actionBeingQueried.getUser().getRole());
			System.out.println("          user team: " + actionBeingQueried.getUser().getTeam());
			System.out.println("          user supervisor: " + actionBeingQueried.getUser().getSupervisor());
			System.out.println("          user resignation: " + actionBeingQueried.getUser().getResinationStatus());
			System.out.println("          user excessive removable media: " + actionBeingQueried.getUser().getExcessiveRemovableDiskUser());
			System.out.println("          user trust score: " + actionBeingQueried.getUser().getTrustScore());
			System.out.print("************************************* ");
			// write suspicious action into a file for benchmark
			try {
				this.writeSuspiciousAction.write(String.format("%s,", actionGraphID.substring((prefix+"graph/").length())));
				this.writeSuspiciousAction.write(String.format("%s, ", actionBeingQueried.getTimestamp()));
				this.writeSuspiciousAction.write(String.format("%s \n", actionBeingQueried.getUser().getID()));
				this.writeSuspiciousAction.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}	
			// move this action to suspicious action graphs
			client.getANonReasoningConn().update("add <"+actionGraphID+"> to <"+prefix+"suspicious>").execute();
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
			updateDiffIndividuals(files, mergedFile, actionBeingQueried.getActionID());
		} catch (IOException e) {
			e.printStackTrace();
		}
		// merge different individual files
		mergeFiles(files,mergedFile); 
		// load different individual files to database
		client.getANonReasoningConn().begin();
		try {
			client.getANonReasoningConn().add().io().context(Values.iri(prefix+"different-individuals")).format(RDFFormat.TURTLE).stream(new FileInputStream("data/different-individuals/different-individuals.ttl"));
		} catch (StardogException | FileNotFoundException e) {
			e.printStackTrace();
		}
		client.getANonReasoningConn().commit();
		// construct and execute the query
		String q = "select distinct ?userid from <"+prefix+"suspicious> "+"from <"+prefix+"background> from <"+prefix+"different-individuals> from <"+prefix+"actor-event> where { ?userid a <"+prefix+"PotentialThreateningInsider>.}";
		TupleQueryResult result = client.getAReasoningConn().select(q).execute();
		while(result.hasNext()) {
			BindingSet bs = result.next();
			System.out.println();
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.println("[Threatening] Data Exfiltraion Event Detected!");
			System.out.println("              potential threatening insider: " + bs.getValue("userid").toString().substring(prefix.length()));				
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.print("************************************* ");
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
