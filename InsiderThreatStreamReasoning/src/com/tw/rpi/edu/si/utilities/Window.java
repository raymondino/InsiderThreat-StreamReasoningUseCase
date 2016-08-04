package com.tw.rpi.edu.si.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;

import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.rio.RDFFormat;

import com.complexible.common.rdf.model.StardogValueFactory.RDF;
import com.complexible.common.openrdf.model.Models2;
import com.complexible.common.rdf.model.Values;
import com.complexible.stardog.StardogException;

public class Window {
	private static String prefix = "http://tw.rpi.edu/ontology/DataExfiltration/";

	private Period size;
	private Period step;
	private ZonedDateTime latestActionTS;
	private Action latestAction;
	private Action actionBeingQueried;
	private ZonedDateTime endOfDay; // end of day
	private ZonedDateTime lastEndOfDay; // last end of day
	private ZonedDateTime start; // window start
	private ZonedDateTime end; // window end
	private Boolean window_start; // flag for window start
	private long totalActionProcessTime; // total time up till now to process all actions
	private Integer actionCounter; // total action number so far
	private LinkedHashMap<String, String> otherSuspiciousActionsAtTheEndOfDay;	
	private SnarlClient client;
	private PrintWriter writeSuspiciousAction;
	
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
		window_start = false;
		otherSuspiciousActionsAtTheEndOfDay = new LinkedHashMap<String, String> ();
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
		window_start = false;
		totalActionProcessTime = (long) 0.0;
		actionCounter = 0;
		otherSuspiciousActionsAtTheEndOfDay = new LinkedHashMap<String, String> ();
	}
	
	// assessor
	public ZonedDateTime getStart() { return start; }
	public ZonedDateTime getEnd() { return end; }
	public Period getStep() {return step;}
	public Period getSize() { return size;}
	
	// modifier
	public void setStep(int s) {step = Period.ofDays(s);}
	public void setWeeklySize(int s) {size = Period.ofDays(s);}
	public void setMonthlySize(int s) {size = Period.ofMonths(s);}
	public void setStart(ZonedDateTime s) {start = s; end = start.plus(size); lastEndOfDay = start; endOfDay = ZonedDateTime.of(start.getYear(), start.getMonthValue(), start.getDayOfMonth(), 23, 59, 59, 0, ZoneId.of("US/Eastern"));}
	private void updateEndOfDay(ZonedDateTime s) {endOfDay = ZonedDateTime.of(s.getYear(), s.getMonthValue(), s.getDayOfMonth(), 23, 59, 59, 0, ZoneId.of("US/Eastern"));}
	public void setMetricWriter(PrintWriter pw) { this.writeSuspiciousAction = pw;}
	
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
		// check suspicious device actions at the end of every day
		if(latestAction.getUser().getExcessiveRemovableDiskUser() && latestActionTS.isAfter(endOfDay)) {
			// add ExcessiveRemovableDriveUser info into actor-event graph
			client.addModel(Models2.newModel(Values.statement(Values.iri(prefix+latestAction.getUser().getID()),RDF.TYPE, Values.iri(prefix+"ExcessiveRemovableDriveUser"))), prefix+"actor-event");
			// form device action query
			String fromGraph = "from <" + prefix + "background> from <" + prefix + "actor-event> ";
			for(Action a: actions) {
				ZonedDateTime aTS = a.getTimestamp();
				// we only need to query today's device actions
				if((aTS.isAfter(lastEndOfDay)) && (aTS.isBefore(endOfDay)) && a.getActionID().contains("device")) {
					fromGraph += ("from <" + prefix + "graph/" + a.getActionID() + "> ");
				}
			}
			String deviceQuery = "select distinct ?action ?actor " + fromGraph + "where { ?action a <" + prefix + "SuspiciousAction>. ?action <"+prefix+"hasActor> ?actor}";
			TupleQueryResult result = client.getAReasoningConn().select(deviceQuery).execute();
			while(result.hasNext()) {
				BindingSet bs = result.next();
				otherSuspiciousActionsAtTheEndOfDay.put(bs.getValue("action").toString().substring(prefix.length()), bs.getValue("actor").toString().substring(prefix.length()));
			}
			// delete ExcessiveRemovableDriveUser from actor-event graph
			String deleteData =  "delete data { graph <"+prefix+"actor-event> {<"+prefix+latestAction.getUser().getID()+"> a <"+prefix+"ExcessiveRemovableDriveUser>}}";
			client.getANonReasoningConn().update(deleteData);
			// update the new end of day
			lastEndOfDay = endOfDay;
		}
		// update endOfDay everyday
		if(latestActionTS.isAfter(endOfDay)) {
			updateEndOfDay(latestActionTS);
		}
		// if window is not full
		if(latestActionTS.isBefore(end)) {
			// record action process time
			long actionProcessStartTime = System.currentTimeMillis();
			// if actions are ranked by [prov]
			if(latestAction.isRankByProv()) {
				while(actions.size() > 0 && actions.peek().getProvenanceScore() > 0) {
					actionBeingQueried = actions.poll();
					// ITAdmins are OK
					if(actionBeingQueried.getUser().getRole().equals("ITAdmin")) {
						System.out.print("<- ITAdmin ");
						continue;
					}
					// do not process an action that is only after hour
					if(actionBeingQueried.getAfterHourAction() && actionBeingQueried.getProvenanceScore() == 1) {
						actionBeingQueried.getUser().reduceTrustScore();
						System.out.print("<- after hour ");	
						continue;
					}
					// process suspicious-looking actions
					System.out.print("[prov][query] ");
					if(query(actionBeingQueried.getActionGraphID())) {
						totalActionProcessTime += (System.currentTimeMillis() - actionProcessStartTime);
						writeSuspiciousAction.println(totalActionProcessTime / actionCounter + "ms");
					}
				}
			}
			// if actions are ranked by [prov, trust]
			else if (latestAction.isRankByProvTrust()) {
				while(actions.size() > 0 && (
					  actions.peek().getProvenanceScore() > 0 || (
					  actions.peek().getProvenanceScore() == 0 && 
				      actions.peek().getUser().getTrustScore() < 50))) {
						actionBeingQueried = actions.poll();
						// ITAdmins are OK
						if(actionBeingQueried.getUser().getRole().equals("ITAdmin")) {
							System.out.print("<- ITAdmin ");
							continue;
						}
						// do not process an action that is only after hour
						if(actionBeingQueried.getAfterHourAction() && actionBeingQueried.getProvenanceScore() == 1) {
							actionBeingQueried.getUser().reduceTrustScore();
							continue;
						}
						// process suspicious-looking actions
						System.out.print("[prov,trust][query] ");
						if(query(actionBeingQueried.getActionGraphID())) {
							totalActionProcessTime += (System.currentTimeMillis() - actionProcessStartTime);
							writeSuspiciousAction.println(totalActionProcessTime / actionCounter + "ms");
						}
					}
			}
			// if no SI is used
			else {
				System.out.print("[no SI][query] ");
				actionBeingQueried = actions.peek();
				// ITAdmins are OK
				if(actionBeingQueried.getUser().getRole().equals("ITAdmin")) {
					System.out.print("<- ITAdmin ");
				}
				else if(query(actionBeingQueried.getActionGraphID())) {
					totalActionProcessTime += (System.currentTimeMillis() - actionProcessStartTime);
					writeSuspiciousAction.println(totalActionProcessTime / actionCounter + "ms");
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
	private boolean query(String actionGraphID) {
		String jobHuntingActionQuery = "ask from <"+prefix+"background> from <"+actionGraphID+"> where {?action a <" + prefix + "JobHuntingAction>.}";
		if(client.getAReasoningConn().ask(jobHuntingActionQuery).execute()) {
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
			System.out.println("          action provenance score: " + actionBeingQueried.getProvenanceScore());
			System.out.println();
			System.out.println("          action performed pc: " + actionBeingQueried.getPc());
			System.out.println("          user assigned pc: " + actionBeingQueried.getUser().getPC());
			System.out.println("          user id: " + actionBeingQueried.getUser().getID());
			System.out.println("          user name: " + actionBeingQueried.getUser().getName());
			System.out.println("          user role: " + actionBeingQueried.getUser().getRole());
			System.out.println("          user team: " + actionBeingQueried.getUser().getTeam());
			System.out.println("          user supervisor: " + actionBeingQueried.getUser().getSupervisor());
			System.out.println("          user resignation: " + actionBeingQueried.getUser().getResinationStatus());
			System.out.println("          user excessive removable media user: " + actionBeingQueried.getUser().getExcessiveRemovableDiskUser());
			System.out.println("          user trust score: " + actionBeingQueried.getUser().getTrustScore());
			System.out.print("************************************* ");
			if(otherSuspiciousActionsAtTheEndOfDay.size() != 0) {
				Set<String> keys = otherSuspiciousActionsAtTheEndOfDay.keySet();
				for(String k:keys) {
					// didn't output timestamp for end of day suspicious device action, I can do it but I am lazy.
					this.writeSuspiciousAction.println(k + ",," + otherSuspiciousActionsAtTheEndOfDay.get(k));
				}
				this.otherSuspiciousActionsAtTheEndOfDay.clear();	
			}
			this.writeSuspiciousAction.print(actionGraphID.substring((prefix+"graph/").length()) + "," + actionBeingQueried.getTimestamp() + "," + actionBeingQueried.getUser().getID() + ",");
			this.writeSuspiciousAction.flush();
			// move this action to suspicious action graphs
			client.getANonReasoningConn().update("add <"+actionGraphID+"> to <"+prefix+"suspicious>").execute(); 
			dataExfiltraion();
			return true;
		}
		return false;
	}
	
	// data exfiltration query
	private void dataExfiltraion() {
		if(actionBeingQueried.getUser().getPotentialThreateningInsider()) {
			System.out.println();
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.println("[Threatening] Data Exfiltraion Event Detected!");
			System.out.println("              potential threatening insider: " + actionBeingQueried.getUser().getID());				
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.print("************************************* ");
			return;
		}
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
		String q = "ask from <"+prefix+"suspicious> "+"from <"+prefix+"background> from <"+prefix+"different-individuals> from <"+prefix+"actor-event> where { <"+prefix+actionBeingQueried.getUser().getID()+"> a <"+prefix+"PotentialThreateningInsider>.}";
		if(client.getAReasoningConn().ask(q).execute()) {
			System.out.println();
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.println("[Threatening] Data Exfiltraion Event Detected!");
			System.out.println("              potential threatening insider: " + actionBeingQueried.getUser().getID());				
			System.out.println("*************************************");
			System.out.println("*************************************");
			System.out.print("************************************* ");
			actionBeingQueried.getUser().setPotentialThreateningInsider();			
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
