package com.tw.rpi.edu.si.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.complexible.common.openrdf.model.Models2;
import com.complexible.common.openrdf.util.Expression;
import com.complexible.common.rdf.model.StardogValueFactory.RDF;
import com.complexible.common.rdf.model.Values;
import com.complexible.stardog.reasoning.Proof;
import com.complexible.stardog.reasoning.ProofType;
import com.tw.rpi.edu.si.utilities.ComparableStatement;
import com.tw.rpi.edu.si.utilities.ComparableStatementComparator;
import com.tw.rpi.edu.si.utilities.SnarlClient;

public class SemanticImportance {

	private static int personSampling = 20; // person sensor 200 Hz, sampling is 20 makes it down to 10Hz
	private static int ballSampling = 100; // ball sensor 2000Hz, sampling is 100 makes it down to 20Hz
	private static int count = 0; // reasoning participation frequency
	private static LocalTime rts = null; // reasoning participation recency;
	private static float evictionUpperLimit = (float) 0.1; // controls the irrelevent data eviction
	private static int cacheSize = 55; // we did experiment, and 54 triples are the max number of different triples. 
	
	// strategy control
	private Boolean dl; // domain literate
	private Boolean e; // eviction
	
	// strategy variables
	private String mode;
	private PrintWriter metricRecorder;
	private double cachePercentage;
	private String prefix;
	private String graph;
	
	// data management
	private String data;
	private Map<String, Integer> sampling;	
	private PriorityQueue<ComparableStatement> cache;
	private Set<Statement> uniqueData;
	private String ballToucher; // keep track of who touches the ball
	private String inFieldBall; // keep track of the current ball being played
	private List<String> teamA; // players in TeamA
	private List<String> teamB; // players in TeamB
	private Map<String, String> involver_pos;
	private Map<String, String> team_slp; // keep track of the second last defender
	private Map<String, String> player_ownhalf;	// keep track of who is (not) at own half 
	
	// the client that talks to the back-end Stardog triple-store
	private SnarlClient client;
	
	// to read data from file for stream simulation
	private BufferedReader br; 	
	// evaluation metrics
	private long runningTime; // record the system running time	
	private int tripleCount; // record the number of triples not sampled. 
	private long evictionTotalTime; // record the total time of data eviction
	private int evictionCount; // record the total number of triples evicted
	private long queryTotalTime; // record the total time of SPARQL query
	private int queryCount; // record the query execution times
	private long explanationTime; // record the total reasoning explanation time
	private int explanationCount; // record the reasoning explanation times
	private long filterTotalTime; // record the total domain literate filtering time
	private int filterCount; // record the domain filter times
	private int filteredTriples; // record the total filtered triple count
	private int maxTripleUsed;
	
	// basic strategies constructor
	public SemanticImportance(String m, SnarlClient c, String path, 
			PrintWriter mr, String prefix_, String graph_) {
		mode = m; metricRecorder = mr; dl = false; e =false;
		prefix = prefix_; graph = graph_; client = c;
		initialization();
		try {
			this.br = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(path))));
		} catch (FileNotFoundException e1) {
			System.out.println("[ERROR]: streaming data path is invalid:" + path);
			e1.printStackTrace();
		}
		metricRecorder.println(path);
	}
	
	// domain literate strategies constructor
	public SemanticImportance(String m, SnarlClient c, String path, 
			PrintWriter mr, String dl_, String prefix_, String graph_) {
		mode = m; metricRecorder = mr; dl = true; e = false;
		prefix = prefix_; graph = graph_; client = c;
		initialization();
		try {
			this.br = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(path))));
		} catch (FileNotFoundException e1) {
			System.out.println("[ERROR]: streaming data path is invalid:" + path);
			e1.printStackTrace();
		}
		metricRecorder.println(path);
	}
	
	// cache eviction strategies constructor
	public SemanticImportance(String m, SnarlClient c, String path, 
			PrintWriter mr, double p, String prefix_, String graph_) {
		mode = m; metricRecorder = mr; dl = false; e = true; cachePercentage = p;
		prefix = prefix_; graph = graph_; client = c;
		initialization();
		try {
			this.br = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(path))));
		} catch (FileNotFoundException e1) {
			System.out.println("[ERROR]: streaming data path is invalid:" + path);
			e1.printStackTrace();
		}
		metricRecorder.println(path);
	}
	
	// domain literate & cache eviction constructor
	public SemanticImportance(String m, SnarlClient c, String path, 
			PrintWriter mr, String dl_, double p, String prefix_, String graph_) {
		mode = m; metricRecorder = mr; dl = true; e = true; cachePercentage = p;
		prefix = prefix_; graph = graph_; client = c;
		initialization();
		try {
			this.br = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(path))));
		} catch (FileNotFoundException e1) {
			System.out.println("[ERROR]: streaming data path is invalid:" + path);
			e1.printStackTrace();
		}
		metricRecorder.println(path);
	}
	
	private void initialization() {
		data = null;
		sampling = new HashMap<String, Integer>();
		cache = new PriorityQueue<ComparableStatement>();
		uniqueData = new HashSet<Statement>();		
		ballToucher = null;
		inFieldBall = null;
		teamA = new ArrayList<String>();
		teamB = new ArrayList<String>();
		involver_pos = new HashMap<String, String>();
		team_slp = new HashMap<String, String>();
		player_ownhalf = new HashMap<String, String>();
		
		// initialize players for teams
		teamA.add("goalkeeper_A");
		teamB.add("goalkeeper_B"); 
		for(int i = 1; i < 8; ++i) { teamA.add("player_A" + i); }
		for(int i = 0; i < 8; ++i) { teamB.add("player_B" + i);	}		
		
		// own half player information
		team_slp.put("TeamA", "");
		team_slp.put("TeamB", "");		
		for(int i = 0; i < teamA.size(); ++i) { player_ownhalf.put(teamA.get(i), null);}
		for(int i = 0; i < teamB.size(); ++i) {	player_ownhalf.put(teamB.get(i), null);}		

		// initialize the evaluation metrics
		this.runningTime = (long) 0.0;
		this.tripleCount = 0;
		this.evictionTotalTime = (long) 0.0;
		this.evictionCount = 0;
		this.queryTotalTime = (long) 0.0;
		this.queryCount = 0;
		this.explanationTime = (long) 0.0;
		this.explanationCount = 0;
		this.filterTotalTime = (long) 0.0;
		this.filteredTriples = 0;
		this.filterCount = 0;
		this.maxTripleUsed = 0;
	}
	
	public void run() {
		long runStartTime = System.currentTimeMillis(); // system timer starts
		client.clearGraph(); // empty the database before starts
		
		// read in the streaming data
		try {
			while((data = br.readLine()) != null) {
				String[] parts = data.split(",");
				String s = parts[0];
				String p = parts[1];				
				String o = parts[2];
				double gts = Double.parseDouble(parts[3]);
				String ts = parts[4];
				
				// data sampling: only sampling hasPosition data
				// if s is a person
				if(p.equals("hasPosition") && (s.contains("player") || s.contains("goalkeeper") || s.contains("referee"))) {
					// no record in the sampling map, add the new record
					if(sampling.get(s) == null) {
						sampling.put(s, 1);
						if(e && cache.size() >= (int) cacheSize*cachePercentage) {
							//System.out.println("[INFO] data evicting ...");
							dataEviction();
						}
						loadStreamingData(s, p, o, gts, ts);
					}
					// has record, check if it reaches to the sampling limit
					else if (sampling.get(s) <= personSampling) {
						if(e && cache.size() >= (int) cacheSize*cachePercentage) {
							//System.out.println("[INFO] data evicting ...");
							dataEviction();
						}
						sampling.put(s, sampling.get(s) + 1);
					}
					// reaches the sampling limit, read the data, then clear the record
					else {
						sampling.put(s, 1);
						if(e && cache.size() >= (int) cacheSize*cachePercentage) {
							//System.out.println("[INFO] data evicting ...");
							dataEviction();
						}
						loadStreamingData(s, p, o, gts, ts);
					}
				}
				// else if s is a ball
				else if(p.equals("hasPosition") && s.contains("ball")){
					// no record in the sampling map, add the new record
					if(sampling.get(s) == null) {
						sampling.put(s, 1);
						if(e && cache.size() >= (int) cacheSize*cachePercentage) {
							//System.out.println("[INFO] data evicting ...");
							dataEviction();
						}
						loadStreamingData(s, p, o, gts, ts);
					}
					// has record, check if it reaches to the sampling limit
					else if (sampling.get(s) <= ballSampling) {
						sampling.put(s, sampling.get(s) + 1);
					}
					// reaches the sampling limit, read the data, then clear the record
					else {
						sampling.put(s, 1);
						if(e && cache.size() >= (int) cacheSize*cachePercentage) {
							//System.out.println("[INFO] data evicting ...");
							dataEviction();
						}
						loadStreamingData(s, p, o, gts, ts);
					}			
				}
				else {
					// else read in the data
					if(e && cache.size() >= (int) cacheSize*cachePercentage) {
						// evict the data
						//System.out.println("[INFO] data evicting ...");
						dataEviction();
					}
					loadStreamingData(s, p, o, gts, ts);					
				}
			}			
		} catch (IOException e) {
			System.out.println("[ERROR] cannot read the streaming file" + data);
			e.printStackTrace();
		}
		
		// writing benchmarks
		runningTime = (System.currentTimeMillis() - runStartTime);
		metricRecorder.println("throughtput = " + (tripleCount/(runningTime/1000.0)) + " triples/second");
		metricRecorder.println("average sparql time = " + ((this.queryTotalTime/1000.0)/this.queryCount + " second"));
		if(!mode.contains("FIFO")) { // if LRU or LFU
			metricRecorder.println("average explanation time = " + (this.explanationTime/1000.0)/this.explanationCount + " second");			
		}
		metricRecorder.println("eviction rate = " + (evictionCount/(this.evictionTotalTime/1000.0)) + " triples/second");			
		if(dl){ // if domain literate
			metricRecorder.println("average filter time = " + (this.filterTotalTime/1000.0)/this.filterCount + " second");
			metricRecorder.println("average filtered triples = " + this.filteredTriples/this.filterCount * 1.0);
		}
		metricRecorder.println("maxTripleUsed = " + maxTripleUsed);
		metricRecorder.println();
		metricRecorder.flush();
	}
	
	// load the streaming data
	private void loadStreamingData(String s, String p, String o, double gts, String ts) {
		if(maxTripleUsed < cache.size()) {maxTripleUsed = cache.size();}
		tripleCount++;
		// create current statement
		Statement newStat = null;
		if(p.equals("a")) newStat = Values.statement(Values.iri(prefix + s), RDF.TYPE, Values.iri(prefix + o));
		else newStat = Values.statement(Values.iri(prefix + s), Values.iri(prefix + p), Values.iri(prefix + o));
		// create a ComparableStatement
		ComparableStatement newCS = new ComparableStatement(newStat, gts, count, rts, mode);
		// hasPosition data
		if(p.equals("hasPosition") || s.contains("Position")) {
			posWindow(s, p, o, newStat, newCS, gts);
		}
		// ball data
		else if (o.equals("BackupBall") || o.equals("InFieldBall")) {
			ballWindow(s, o, newStat, newCS, gts);
		}
		// <player, a, SecondLastPlayer>
		else if(o.equals("SecondLastPlayer")) {
			slpWindow(s, newStat, newCS, gts);
		}
		// if player is (not) in own half
		else if(o.equals("PlayerInOwnHalf") || o.equals("PlayerNotInOwnHalf")){
			pohWindow(s, o, newStat, newCS, gts);
		}
		//<player, touches, ball>
		else if(p.equals("touches")) {
			btWindow(s, p, o, newStat, newCS, gts, ts);
		}
		// get opponent challenge data or inNearer 
		else if (o.equals("opponent_challenge") || p.equals("isNearerToDefenderGoalLineThan")) {
			if(uniqueData.add(newStat)) {
				cache.add(newCS);
				client.addModel(Models2.newModel(newStat), graph);
			}
			else {
				for(ComparableStatement i:cache) {
					if(i.getStats().equals(newStat)) {
						i.updateGenerationTimestamp(gts);
						return;
					}
				}				
			}
		}
	}
	
	// manage position data by either adding new data or replacing expired data
	private void posWindow(String s, String p, String o, Statement newStat, ComparableStatement newCS, double gts) {
		// <player/ball/glove/referee, hasPosition, position12345678>
		if(p.equals("hasPosition")) {
			// if it's the first position for an involver
			if(involver_pos.get(s) == null) {				
				involver_pos.put(s, o); // update data management
				uniqueData.add(newStat); // add it into the uniqueData
				cache.add(newCS); // add it into the cache
				client.addModel(Models2.newModel(newStat), graph); // update triple-store
			}
			// if the position has been updated
			else if (!involver_pos.get(s).equals(o)) {
				// create a previous position statement
				Statement oldPos = Values.statement(Values.iri(prefix + s), Values.iri(prefix + p), Values.iri(prefix + involver_pos.get(s)));
				// if cache can remove, then need to delete it from triple-store
				if(uniqueData.remove(oldPos)){
					for(ComparableStatement i:cache) {
						if(i.getStats().equals(oldPos)){
							cache.remove(i);
							break;
						}
					}
					String deleteQuery = "delete data { "
							+ "graph <" + graph + "> {"
							+ "<" + prefix + s + "> "
							+ "<" + prefix + p + "> "
							+ "<" + prefix + involver_pos.get(s)+">}}; ";
					this.delete(deleteQuery, true); // update in triple-store
				}
				// if cache can add, then need to add it to triple-store
				if(uniqueData.add(newStat)){
					cache.add(newCS);
					client.addModel(Models2.newModel(newStat), graph); // update triple-store
				}
				involver_pos.put(s, o);	// update the data management
			}
		}
		// <position,a,offsideIrrelevantPosition> when all balls are out of bounds
		else {
			// check if newCS is already in the uniqueData, then add new ones
			if(uniqueData.add(newStat)) {
				cache.add(newCS);
				client.addModel(Models2.newModel(newStat), graph);	
			}			
		}		
	}
	
	// manage ball data by adding new or replacing expired
	private void ballWindow(String s, String o, Statement newStat, ComparableStatement newCS, double gts) {
		// <ball, a, BackupBall>
		if (o.equals("BackupBall")) {
			if(inFieldBall == null || !s.equals(inFieldBall)) { 
				if(uniqueData.add(newStat)) { // add into cache or update the generation time-stamp
					cache.add(newCS);
					client.addModel(Models2.newModel(newStat), graph); // add into triple-store
				}
				else { // update generation time-stamp
					for(ComparableStatement i:cache) {
						if(i.getStats().equals(newStat)){
							i.updateGenerationTimestamp(gts);
							break;
						}
					}
				}
			}
			/*
			 * if InFieldBall becomes a BackupBall, that means all balls are out bound before. 
			 * So need to delete <ball, a, InFieldBall>, 
			 *                   <player, isInvolvedIn, ActivePlay>
			 *                   <player, isNearerToDefenderGoalLineThan, player/ball>
			 *                   <player, touches, ball> 
			 * Add: <ball, a, BackupBall>
			 */
			else if (inFieldBall.equals(s)) {
				System.out.println("[BALL] the previously InFieldBall " + inFieldBall + " becomes a BackupBall");
				Statement prevInFieldBall = Values.statement(Values.iri(prefix + s), RDF.TYPE, Values.iri(prefix + "InFieldBall"));
				if(uniqueData.remove(prevInFieldBall)) {
					for(ComparableStatement i:cache){
						if(i.getStats().equals(prevInFieldBall)){
							cache.remove(i);
							break;
						}
					}
					String deleteQuery = "delete data {"
							+ "graph <" + graph + "> {"
							+ "<" + prefix + s + "> "
							+ "<" + RDF.TYPE + "> "
							+ "<" + prefix + "InFieldBall>}};";
					this.delete(deleteQuery, true);
				}
				if(uniqueData.add(newStat)) {
					cache.add(newCS);
					String insertQuery = "insert data {"
							+ "graph <" + graph + "> {"
							+ "<" + prefix + s + "> "
							+ "<" + RDF.TYPE + "> "
							+ "<" + prefix + "BackupBall>}}";
					this.insert(insertQuery);
				}				
				// we then need to delete all previous ball toucher information
				String btQuery = "select ?s ?o where {graph <" + graph + "> {?s <" + prefix + "touches> ?o.}}";
				TupleQueryResult result = client.getANonReasoningConn().select(btQuery).execute();
				while(result.hasNext()) {
					 BindingSet bs = result.next();
					 Statement oldTouchesBall = Values.statement(Values.iri(bs.getValue("s").toString()), Values.iri(prefix + "touches"), Values.iri(bs.getValue("o").toString()));
					 Statement oldball_touch = Values.statement(Values.iri(bs.getValue("s").toString()), Values.iri(prefix + "isInvolvedIn"), Values.iri(prefix + "ball_touch"));
					 if(uniqueData.remove(oldTouchesBall)) {
							for(ComparableStatement i:cache){
								if(i.getStats().equals(oldTouchesBall)){
									cache.remove(i);
									break;
								}
							}
						 String deleteQuery = "delete data {"
							 		+ "graph <" + graph + "> {"
									+ "<" + bs.getValue("s").toString()+ "> "
									+ "<" + prefix +"touches> "
									+ "<" + bs.getValue("o").toString() + ">}}";
						delete(deleteQuery, true);
					 }
					 if(uniqueData.remove(oldball_touch)) {
							for(ComparableStatement i:cache){
								if(i.getStats().equals(oldball_touch)){
									cache.remove(i);
									break;
								}
							}
							String deleteQuery = "delete data {"
							 		+ "graph <" + graph + "> {"
									+ "<" + bs.getValue("s").toString() + "> "
									+ "<" + prefix + "isInvolvedIn> "
									+ "<" + prefix + "ball_touch>}}"; 
						delete(deleteQuery, true);
					 }					
				}
				result.close();
				// then delete all opponent_challenge data
				String ocQuery = "select distinct ?s where { graph <" + graph + "> {?s <" + prefix + "isInvolvedIn> <" + prefix + "opponent_challenge>}}";
				TupleQueryResult ocResultSet = client.getANonReasoningConn().select(ocQuery).execute();
				while(ocResultSet.hasNext()) {
					evictionCount++;
					Statement toRemove = Values.statement(Values.iri(ocResultSet.next().getValue("s").toString()), Values.iri(prefix + "isInvolvedIn"), Values.iri(prefix + "opponent_challenge"));
					 if(uniqueData.remove(toRemove)) {
							for(ComparableStatement i:cache){
								if(i.getStats().equals(toRemove)){
									cache.remove(i);
									break;
								}
							}
					 }
				}
				String ocClearQuery = "delete { "
						+ "graph <" + graph +"> {"
						+ "?s "
						+ "<" + prefix + "isInvolvedIn> "
						+ "<" + prefix + "opponent_challenge>}} "
						+ "where { graph <" + graph + ">"
						+ " {?s "
						+ "<" + prefix + "isInvolvedIn> "
						+ "<" + prefix + "opponent_challenge>}}";				
				this.delete(ocClearQuery, false); // update the triple-store
				ocResultSet.close();
				// then delete all inNearer data
				String nearQuery = "select distinct ?s ?o where {graph<" + graph + ">{?s <" + prefix + "isNearerToDefenderGoalLineThan> ?o}}";
				TupleQueryResult nearResultSet = client.getANonReasoningConn().select(nearQuery).execute();
				while(nearResultSet.hasNext()) {
					this.evictionCount++;
					BindingSet result1 = nearResultSet.next();
					Statement toRemove = Values.statement(Values.iri(result1.getValue("s").toString()), Values.iri(prefix+"isNearerToDefenderGoalLineThan"), Values.iri(result1.getValue("o").toString()));
					if(uniqueData.remove(toRemove)) {
						for(ComparableStatement i:cache){
							if(i.getStats().equals(toRemove)){
								cache.remove(i);
								break;
							}
						}
					}
				}
				String nearClearQuery = "delete { "
						+ "graph <" + graph + "> {"
						+ "?s "
						+ "<" + prefix + "isNearerToDefenderGoalLineThan> "
						+ "?o}} "
						+ "where { graph <" + graph + "> {"
						+ "?s "
						+ "<" + prefix + "isNearerToDefenderGoalLineThan> "
						+ "?o}}";				
				this.delete(nearClearQuery, false); // update the triple-store
				nearResultSet.close();				
				inFieldBall = ""; // update the data management
				ballToucher = null; // update the data management
			}
		}
		// <ball, a, InFieldBall>
		else {
			if(inFieldBall == null || s.equals(inFieldBall)) {
				if(uniqueData.add(newStat)) { // add into cache or update the generation time-stamp
					cache.add(newCS);
					client.addModel(Models2.newModel(newStat), graph); // add into triple-store
					inFieldBall = s;
				}
				else { // update generation time-stamp
					for(ComparableStatement i:cache) {
						if(i.getStats().equals(newStat)) {
							i.updateGenerationTimestamp(gts);
							break;
						}
					}
				}
			}
			// if inFieldBall is empty, then this ball is previously a BackupBall
			else if(inFieldBall.equals("")){
				System.out.println("[BALL] the previously BackupBall " + s + " becomes an InFieldBall");
				Statement prevBackupBall = Values.statement(Values.iri(prefix + s), RDF.TYPE, Values.iri(prefix + "BackupBall"));
				if(uniqueData.remove(prevBackupBall)) {
					for(ComparableStatement i:cache){
						if(i.getStats().equals(prevBackupBall)){
							cache.remove(i);
							break;
						}
					}
					String deleteQuery = "delete data {"
							+ "graph <" + graph +"> {"
							+ "<" + prefix + s +"> "
							+ "<" + RDF.TYPE +"> "
							+ "<" + prefix + "BackupBall> }};";
					this.delete(deleteQuery, true);
				}
				if(uniqueData.add(newStat)) {
					cache.add(newCS);
					String insertQuery = "insert data {"
							+ "graph <" + graph + "> {"
							+ "<" + prefix + s + "> "
							+ "<" + RDF.TYPE + "> "
							+ "<" + prefix + "InFieldBall>}}";
					this.insert(insertQuery);
				}
				inFieldBall = s; // update data management
			}
			// if BackupBall becomes InFieldBall 
			else if(!inFieldBall.equals(s)) {
				System.out.println("[BALL] the previously InFieldBall " + inFieldBall + " becomes a BackupBall");
				System.out.println("       the previously BackupBall " + s + " becomes an InFieldBall");
				Statement preBackupBall = Values.statement(Values.iri(prefix + s), RDF.TYPE, Values.iri(prefix + "BackupBall"));
				Statement preInFieldBall = Values.statement(Values.iri(prefix + inFieldBall), RDF.TYPE, Values.iri(prefix + "InFieldBall"));
				Statement newBackupBall = Values.statement(Values.iri(prefix + inFieldBall), RDF.TYPE, Values.iri(prefix + "BackupBall"));
				if(uniqueData.remove(preBackupBall)) {
					for(ComparableStatement i:cache){
						if(i.getStats().equals(preBackupBall)){
							cache.remove(i);
							break;
						}
					}
					String deleteQuery = "delete data {"
							+ "graph <" + graph + "> {"
							+ "<" + prefix + s + "> "
							+ "<" + RDF.TYPE + "> "
							+ "<" + prefix + "BackupBall>.}};";
					this.delete(deleteQuery, true);
				}
				if(uniqueData.remove(preInFieldBall)) {
					for(ComparableStatement i:cache){
						if(i.getStats().equals(preInFieldBall)){
							cache.remove(i);
							break;
						}
					}
					String deleteQuery = "delete data {"
							+ "graph <" + graph + "> {"
							+ "<" + prefix + inFieldBall + "> "
							+ "<" + RDF.TYPE + "> "
							+ "<" + prefix + "InFieldBall>}}; ";
					this.delete(deleteQuery, true);
				}
				if(uniqueData.add(newBackupBall)) {
					cache.add(new ComparableStatement(newBackupBall, gts, count, rts, mode));
					String insertQuery = "insert data {"
							+ "graph <" + graph + "> {"
							+ "<" + prefix + inFieldBall + "> "
							+ "<" + RDF.TYPE + "> "
							+ "<" + prefix + "BackupBall>}}";
					this.insert(insertQuery);
				}
				if(uniqueData.add(newStat)) {
					cache.add(newCS);
					client.addModel(Models2.newModel(newStat), graph);
				}
				inFieldBall = s;
			}
		}
	}

	// manage second last player data by either adding new or replacing expired data
	private void slpWindow(String s, Statement newStat, ComparableStatement newCS, double gts) {
		// TeamA's second last player
		if(s.contains("A")) slpUpdate("TeamA", s, newStat, newCS, gts);
		// TeamB's second last player
		else slpUpdate("TeamB", s, newStat, newCS, gts);
	}
	
	// helper function to process second last player data
	private void slpUpdate(String team, String s, Statement newStat, ComparableStatement newCS, double gts) {
		if(team_slp.get(team).equals((s)) || team_slp.get(team).equals("")) {
			if(uniqueData.add(newStat)) { // add to cache or update the generation time-stamp
				cache.add(newCS);
				client.addModel(Models2.newModel(newStat), graph);
				team_slp.put(team, s);
			} 
			else { // update generation time-stamp
				for(ComparableStatement i:cache) {
					if(i.getStats().equals(newStat)) {
						i.updateGenerationTimestamp(gts);
						break;					
					}
				}
			}
		} 
		else { // team slp data needs to be updated
			System.out.println("[SLP] " + team + " slp changes from " + team_slp.get(team) + " to " + s);
			Statement oldslp = Values.statement(Values.iri(prefix + team_slp.get(team)), RDF.TYPE, Values.iri(prefix + "SecondLastPlayer"));
			Statement oldNslp = Values.statement(Values.iri(prefix + s), RDF.TYPE, Values.iri(prefix + "NotSecondLastPlayer"));
			Statement newNslp = Values.statement(Values.iri(prefix + team_slp.get(team)), RDF.TYPE, Values.iri(prefix + "NotSecondLastPlayer"));
			if(uniqueData.remove(oldslp)) {
				for(ComparableStatement i:cache){
					if(i.getStats().equals(oldslp)) {
						cache.remove(i);
						break;						
					}
				}
				String deleteQuery = "delete data {"
						+ "graph <" + graph + "> { "
						+ "<" + prefix + team_slp.get(team) + "> "
						+ "<" + RDF.TYPE + "> "
						+ "<" + prefix + "SecondLastPlayer>}};";
				this.delete(deleteQuery, true);
			}
			if(uniqueData.remove(oldNslp)) {
				for(ComparableStatement i:cache){
					if(i.getStats().equals(oldNslp)) {
						cache.remove(i);
						break;						
					}
				}
				String deleteQuery = "delete data {"
						+ "graph <" + graph + "> { "
						+ "<" + prefix + s + "> "
						+ "<" + RDF.TYPE + "> "
						+ "<" + prefix + "NotSecondLastPlayer>}};";				
				this.delete(deleteQuery, true);
			}
			if(uniqueData.add(newNslp)) {
				cache.add(new ComparableStatement(newNslp, gts, count, rts, mode));
				String insertQuery  = "insert data {"
						+ "graph <" + graph + "> { "
						+ "<" + prefix + team_slp.get(team) + "> "
						+ "<" + RDF.TYPE + "> "
						+ "<" + prefix + "NotSecondLastPlayer>.}}";	
				this.insert(insertQuery);
			}
			if(uniqueData.add(newStat)) {
				cache.add(newCS);
				client.addModel(Models2.newModel(newNslp), graph);
			}
			team_slp.put(team, s);
		}
	}
	
	// manage player (not) in own half, either add new or replace expired data
	private void pohWindow(String s, String o, Statement newStat, ComparableStatement newCS, double gts) {
		if(player_ownhalf.get(s) == null || player_ownhalf.get(s).equals(o) ) {
			if(uniqueData.add(newStat)){ // add into cache or update generation time-stamp
				cache.add(newCS);
				client.addModel(Models2.newModel(newStat), graph);
				player_ownhalf.put(s, o);
			}
			else { // update generation time-stamp
				for(ComparableStatement i:cache) {
					if(i.getStats().equals(newStat)){
						i.updateGenerationTimestamp(gts);
						break;					
					}
				}
			}
		}
		// this player's own half status changes
		else {			
			System.out.println("[POH] player " + s + " changes from " + player_ownhalf.get(s) + " to " + o);
			Statement oldpoh = Values.statement(Values.iri(prefix + s), RDF.TYPE, Values.iri(prefix + player_ownhalf.get(s)));
			if(uniqueData.remove(oldpoh)) {
				for(ComparableStatement i:cache){
					if(i.getStats().equals(oldpoh)){
						cache.remove(i);
						break;	
					}
				}
				String deleteQuery = "delete data {"
						+ "graph <" + graph + "> {"
						+ "<" + prefix + s + "> "
						+ "<" + RDF.TYPE + "> "
						+ "<" + prefix + player_ownhalf.get(s) + ">.}}; ";
				this.delete(deleteQuery, true);
			}
			if(uniqueData.add(newStat)) {
				cache.add(newCS);
				client.addModel(Models2.newModel(newStat), graph);				
			}
			player_ownhalf.put(s, o);
		}
	}

	// manage player touching the ball
	// manage ball toucher data, adding new or replacing expired data
	private void btWindow(String s, String p, String o, Statement newStat, ComparableStatement newCS, double gts, String ts) {
		Statement newball_touch = Values.statement(Values.iri(prefix + s), Values.iri(prefix + "isInvolvedIn"), Values.iri(prefix + "ball_touch"));
		// <player, touches, ball>
		if( ballToucher == null || ballToucher.equals(s)) { // if the same ball toucher, update the time-stamps
			if(uniqueData.add(newStat)) {
				cache.add(newCS);
				client.addModel(Models2.newModel(newStat), graph);
				if(ballToucher == null) {
					ballToucher = s;
					if(dl) { // if domain literate filter is on
						System.out.println("[DomainLiterate] domain literate filtering ...");
						long filterStartTime = System.currentTimeMillis();
						filter();
						this.filterTotalTime += (System.currentTimeMillis() - filterStartTime);
						this.filterCount++;						
					}
					System.out.println("[Query] who commits offside offence ?");
					queryExecution(ts); // ballToucher changes, need to run query
				}
			}
			else { // update generation time-stamp
				for(ComparableStatement i:cache) {
					if(i.getStats().equals(newStat)) {
						i.updateGenerationTimestamp(gts);
						break;					
					}
				}
			}			
			if(uniqueData.add(newball_touch)) {
				cache.add(new ComparableStatement(newball_touch, gts, count, rts, mode));
				client.addModel(Models2.newModel(newball_touch), graph);
			}
			else { // update generation time-stamp
				for(ComparableStatement i:cache) {
					if(i.getStats().equals(newball_touch)) {
						i.updateGenerationTimestamp(gts);
						break;						
					}
				}
			}
		}
		// ball toucher changes 
		else {
			System.out.println("[BallToucher] ballToucher changes from previous " + ballToucher + " to " + s);
			Statement preBallToucher = Values.statement(Values.iri(prefix + ballToucher), Values.iri(prefix + "touches"), Values.iri(prefix + inFieldBall));
			Statement preActivePlay = Values.statement(Values.iri(prefix + ballToucher), Values.iri(prefix + "isInvolvedIn"), Values.iri(prefix + "ball_touch"));
			Statement newActivePlay = Values.statement(Values.iri(prefix + s), Values.iri(prefix + "isInvolvedIn"), Values.iri(prefix + "ball_touch"));			
			if(uniqueData.remove(preBallToucher)) {
				for(ComparableStatement i:cache) {
					if(i.getStats().equals(preBallToucher)) {
						cache.remove(i);
						break;						
					}
				}
				String deleteQuery = "delete data {"
						+ "graph <" + graph + "> { "
						+ "<" + prefix + ballToucher + "> "
						+ "<" + prefix + "touches> "
						+ "<" + prefix + inFieldBall + ">. }};";
				this.delete(deleteQuery, true);					
			}
			if(uniqueData.remove(preActivePlay)) {
				for(ComparableStatement i:cache) {
					if(i.getStats().equals(preActivePlay)) {
						cache.remove(i);
						break;
					}						
				}
				String deleteQuery = "delete data {"
						+ "graph <" + graph + "> { "
						+ "<" + prefix + ballToucher + "> "
						+ "<" + prefix + "isInvolvedIn> "
						+ "<" + prefix + "ball_touch>.}};";	
				this.delete(deleteQuery, true);					
			}
			if(uniqueData.add(newActivePlay)) {
				cache.add(new ComparableStatement(newActivePlay, gts, count, rts, mode));
				String insertQuery = "insert data {"
						+ "graph <" + graph + "> {"
						+ "<" + prefix + s + "> "
						+ "<" + prefix + "isInvolvedIn> "
						+ "<" + prefix + "ball_touch>.}}";				
				this.insert(insertQuery);
			}
			if(uniqueData.add(newStat)) {
				cache.add(newCS);
				client.addModel(Models2.newModel(newStat), graph);				
			}
			if(dl){ // if domain literate filter is on
				System.out.println("[DomainLiterate] domain literate filtering ...");
				long filterStartTime = System.currentTimeMillis();
				filter();
				this.filterTotalTime += (System.currentTimeMillis() - filterStartTime);
				this.filterCount++;
			}	
			System.out.println("[Query] who commits offside offence ?");
			queryExecution(ts);	// it's important to execute query before delete opponent_challenge/isNearer data
			ballToucher = s; // update data management		
		}		
		// after every ball touch, we need to delete all opponent_challenge data
		String ocQuery = "select distinct ?s where { graph <" + graph + "> {?s <" + prefix + "isInvolvedIn> <" + prefix + "opponent_challenge>}}";
		TupleQueryResult ocResultSet = client.getANonReasoningConn().select(ocQuery).execute();
		while(ocResultSet.hasNext()) {
			evictionCount++;
			Statement toRemove = Values.statement(Values.iri(ocResultSet.next().getValue("s").toString()), Values.iri(prefix + "isInvolvedIn"), Values.iri(prefix + "opponent_challenge"));
			if(uniqueData.remove(toRemove)) {
				for(ComparableStatement i:cache) {
					if(i.getStats().equals(toRemove)){
						cache.remove(i);
						break;
					}
				}
			}
		}
		String ocClearQuery = "delete { "
				+ "graph <" + graph +"> {"
				+ "?s "
				+ "<" + prefix + "isInvolvedIn> "
				+ "<" + prefix + "opponent_challenge>}} "
				+ "where { graph <" + graph + ">"
				+ " {?s "
				+ "<" + prefix + "isInvolvedIn> "
				+ "<" + prefix + "opponent_challenge>}}";				
		this.delete(ocClearQuery, false); // update the triple-store
		ocResultSet.close();
		// then delete all inNearer data
		String nearQuery = "select distinct ?s ?o where {graph<" + graph + ">{?s <" + prefix + "isNearerToDefenderGoalLineThan> ?o}}";
		TupleQueryResult nearResultSet = client.getANonReasoningConn().select(nearQuery).execute();
		while(nearResultSet.hasNext()) {
			this.evictionCount++;
			BindingSet result1 = nearResultSet.next();
			Statement toRemove = Values.statement(Values.iri(result1.getValue("s").toString()), Values.iri(prefix+"isNearerToDefenderGoalLineThan"), Values.iri(result1.getValue("o").toString()));
			if(uniqueData.remove(toRemove)) {
				for(ComparableStatement i:cache){
					if(i.getStats().equals(toRemove)){
						cache.remove(i);
						break;
					}
				}
			}
		}
		String nearClearQuery = "delete { "
				+ "graph <" + graph + "> {"
				+ "?s "
				+ "<" + prefix + "isNearerToDefenderGoalLineThan> "
				+ "?o}} "
				+ "where { graph <" + graph + "> {"
				+ "?s "
				+ "<" + prefix + "isNearerToDefenderGoalLineThan> "
				+ "?o}}";				
		this.delete(nearClearQuery, false); // update the triple-store
		nearResultSet.close();	
	}
	
	// SPARQL delete update
	private void delete (String query, Boolean increment) {
		long evictStartTime = System.currentTimeMillis();
		client.getANonReasoningConn().update(query).execute();
		this.evictionTotalTime += (System.currentTimeMillis() - evictStartTime);
		if(increment) {
			this.evictionCount++;			
		}
	}
	
	// SPARQL insert update
	private void insert(String query) {
		client.getANonReasoningConn().update(query).execute();
	}

	// execute the continuous query
	private void queryExecution(String ts) {
		// flag to indicate if the offside query gives the result or not
		Boolean hasResult = false;		
		String offsideQuery = "select ?s where {?s <" + RDF.TYPE + "> <" + prefix + "PlayerCommitsOffsideOffence>.}";		
		long queryStartTime = System.currentTimeMillis();
		TupleQueryResult resultSet = client.getAReasoningConn().select(offsideQuery).execute();
		this.queryTotalTime += (System.currentTimeMillis() - queryStartTime);
		this.queryCount++;		
		while(resultSet.hasNext()) {
			hasResult = true;
			metricRecorder.println(resultSet.next().getValue("s").toString() + " commits offside foul at " + ts);	
			metricRecorder.flush();
			// reasoning explanation for LRU/LFU
			if(!mode.contains("FIFO")) {
				long explanationStartTime = System.currentTimeMillis();
				reasoningExplanation(offsideQuery, "PlayerCommitsOffsideOffence");
				this.explanationTime += (System.currentTimeMillis() - explanationStartTime);
				this.explanationCount++;
			}
		}
		resultSet.close();		
		// if the original offside query doesn't give the result, collect all triples that are contribute to the query
		// in order to answer the query, all required graph patterns should be presented. 
		// however, not all of them are going to arrive the window at the same time. 
		// this requires the system should be able to preserve the important ones, 
		// by considering not only the actual query participation status, but also
		// the possibility to contribute to the query. 
		// for example, in order to answer player commits offside offence, we need to know
		// that his player is at offside position when his teammate pass the ball, 
		// and that this player is involved in some active play. 
		// if only triple <player, isInovled, ball_touch/opponent_challenge> is in the 
		// window, we need to preserve, although it won't give you an answer yet, but it
		// has the potential as it is one of the required triple.
		// In order to preserve them, we should relax our query when the initial query doesn't 
		// give the result. That's why we present the following three queries, as they
		// ask 1. who is involved active play; 2, who is nearer to defender goal line than whom;
		// 3. who touches the ball. All of these kinds of info are required triples. 
		// 4. which ball is the in filed ball
		if(!mode.contains("FIFO") && !hasResult) {
			queryRelex();
		}
	}

	// explain the reasoning, update the reasoning participation counter for each triple
	private void reasoningExplanation(String query, String namedClass) {
		// execute the query
		TupleQueryResult aResult = this.client.getAReasoningConn().select(query).execute();
		// get the query answer
		while(aResult.hasNext()){
			Value s = aResult.next().getValue("s");
			// get a list of proofs that explains the query answer
			Iterable<Proof> proofs = this.client.getAReasoningConn().explain(Values.statement(Values.iri(s.toString()), RDF.TYPE, Values.iri(prefix + namedClass))).computeNamedGraphs().proofs();
			Iterator<Proof> proof_itr = proofs.iterator();
			while(proof_itr.hasNext()) { // iterate all proofs				
				Proof aProof = proof_itr.next(); // get one proof
				// get all explicit triples that participate in the reasoning
				Iterator<Expression> expression_itr = aProof.getExpressions(ProofType.ASSERTED).iterator();
				while(expression_itr.hasNext()) {
					Expression e = expression_itr.next();
					Iterator<Statement> e_itr = e.iterator();
					while(e_itr.hasNext()) {
						Statement aStat = e_itr.next();
						// for each statement, check if the cache contains it, then update its counter
						for(ComparableStatement i:cache) {
							if(i.getStats().equals(aStat)) {
								if(mode.contains("LFU")) {
									i.increaseCounterByOne();
									break;
								}
								else if (mode.contains("LRU")) {
									i.updateReasoningParticipationTime(LocalTime.now());
									break;
								}
							}
						}						
					}
				}
			}
		}
	}	
	
	// another overload reasoningExplanation function
	private void reasoningExplanation(String query) {
		// execute the query
		TupleQueryResult aResult = this.client.getAReasoningConn().select(query).execute();
		// get the query answer
		while(aResult.hasNext()) {
			BindingSet bs = aResult.next();
			Value s = bs.getValue("s");
			Value p = bs.getValue("p");
			Value o = bs.getValue("o");
			Iterable<Proof> proofs = this.client.getAReasoningConn()
					.explain(Values.statement(
							Values.iri(s.toString()), 
							Values.iri(p.toString()), 
							Values.iri(o.toString())))
					.computeNamedGraphs().proofs();
			Iterator<Proof> proof_itr = proofs.iterator();
			// iterate all proofs
			while(proof_itr.hasNext()) {
				// get one proof
				Proof aProof = proof_itr.next();
				// get all explicit triples that parcipated in the reasoning
				Iterator<Expression> expression_itr = aProof.getExpressions(ProofType.ASSERTED).iterator();
				while(expression_itr.hasNext()) {
					Expression e = expression_itr.next();
					Iterator<Statement> e_itr = e.iterator();
					while(e_itr.hasNext()) {
						Statement aStat = e_itr.next();
						// for each statement, check if the cache contains it, then update its counter
						for(ComparableStatement i:cache) {
							if(i.getStats().equals(aStat)) {
								if(mode.contains("LFU")) {
									i.increaseCounterByOne();
									break;
								}
								else if (mode.contains("LRU")) {
									i.updateReasoningParticipationTime(LocalTime.now());
									break;
								}
							}
						}							
					}
				}
			}
		}
	}
	
	// domain literate filter 
	private void filter() {
		String queryString = "select distinct ?s where {?s a <" + prefix +"OffsideIrrelevantPosition>.}";
		TupleQueryResult results = client.getAReasoningConn().select(queryString).execute();
		while(results.hasNext()) {
			this.filteredTriples++;
			String result = results.next().getValue("s").toString();
			Statement toFilter = Values.statement(Values.iri(result.substring(0, result.indexOf("position"))), Values.iri(prefix + "hasPosition"), Values.iri(result));
			if(uniqueData.remove(toFilter)) {
				for(ComparableStatement i:cache){
					if(i.getStats().equals(toFilter)){
						cache.remove(i);
						break;
					}
				}
				String deleteQuery = "delete data { graph <" + graph + "> {<" + result.substring(0, result.indexOf("position")) + "> <" + prefix +"hasPosition> <" + result + ">.}}";
				this.delete(deleteQuery, true);
			}
		}
	}
	
	// query relaxation
	private void queryRelex() {
		String activePlayQuery = "select ?s ?p ?o where "
				+ "{graph <" + graph + "> "
				+ "{ ?s <" + prefix +"isInvolvedIn> ?o. "
				+ "Bind(IRI(\"" + prefix + "isInvolvedIn\") as ?p)}}";
		String isNearQuery = "select ?s ?p ?o where "
				+ "{graph <" + graph + "> "
				+ "{ ?s <" + prefix +"isNearerToDefenderGoalLineThan> ?o. "
				+ "Bind(IRI(\"" + prefix + "isNearerToDefenderGoalLineThan\") as ?p)}}";
		String ballTouchQuery = "select ?s ?p ?o where "
				+ "{graph <" + graph + "> "
				+ "{ ?s <" + prefix +"touches> ?o. "
				+ "Bind(IRI(\"" + prefix + "touches\") as ?p)}}";
		String slpQuery = "select ?s ?p ?o where "
				+ "{graph <" + graph + "> "
				+ "{ ?s <" + RDF.TYPE +"> <" + prefix +"SecondLastPlayer>. "
				+ "Bind(IRI(\"" + RDF.TYPE + "\") as ?p)"
				+ "Bind(IRI(\"" + prefix + "SecondLastPlayer\") as ?o)}}";
		String inFieldBallQuery = "select ?s ?p ?o where "
				+ "{graph <" + graph + "> "
				+ "{ ?s <" + RDF.TYPE +"> <" + prefix +"InFieldBall>. "
				+ "Bind(IRI(\"" + RDF.TYPE + "\") as ?p)"
				+ "Bind(IRI(\"" + prefix + "InFieldBall\") as ?o)}}";
		
		long explanationStartTime = System.currentTimeMillis();
		reasoningExplanation(activePlayQuery);
		this.explanationCount++;			
		reasoningExplanation(isNearQuery);
		this.explanationCount++;			
		reasoningExplanation(ballTouchQuery);
		this.explanationCount++;			
		reasoningExplanation(slpQuery);
		this.explanationCount++;			
		reasoningExplanation(inFieldBallQuery);
		this.explanationTime += (System.currentTimeMillis() - explanationStartTime);
		this.explanationCount++;
	}
	
	// data eviction
	private void dataEviction() {
		if(!mode.contains("FIFO")) {
			queryRelex();		
		}
/*		try{
			Collections.sort(cache, new ComparableStatementComparator());
		}
		catch(Exception e){
			e.printStackTrace();
		}
*/		int evictionLimit = (int) (cache.size() * evictionUpperLimit);
		int currentEviction = 0;
		while(currentEviction <= evictionLimit){
			Statement toEvict = cache.poll().getStats();
			uniqueData.remove(toEvict); // delete from uniqueData
			// update the program status
			if(toEvict.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
				if(toEvict.getObject().toString().substring(prefix.length()).equals("InFieldBall")) {
					inFieldBall = ""; // if inFieldBall info is to evict, evict the program record
				}
				else if (toEvict.getObject().toString().substring(prefix.length()).equals("PlayerInOwnHalf") || toEvict.getObject().toString().substring(prefix.length()).equals("PlayerNotInOwnHalf")) {
					String player = toEvict.getSubject().toString().substring(prefix.length());
					player_ownhalf.put(player, null); // clear this player's own half information
				}
				else if (toEvict.getObject().toString().substring(prefix.length()).equals("SecondLastPlayer")) {
					String player = toEvict.getSubject().toString().substring(prefix.length());
					if(player.contains("A")) { team_slp.put("TeamA", ""); } // update team A slp
					else { team_slp.put("TeamB", ""); } // update team B slp
				}
			}
			else if (ballToucher != null && toEvict.getPredicate().toString().substring(prefix.length()).equals("touches") || toEvict.getObject().toString().substring(prefix.length()).equals("BallTouch")) {
				ballToucher = null; // update the ball toucher program status
			}
			// delete from triple-store
			String updateQuery = "delete data {graph <" + graph + "> {<" + toEvict.getSubject() + "> <" + toEvict.getPredicate() + "> <" + toEvict.getObject() + ">}}";
			long evictStartTime = System.currentTimeMillis();
			client.getANonReasoningConn().update(updateQuery).execute();
			this.evictionTotalTime += (System.currentTimeMillis() - evictStartTime);
			this.evictionCount++;
			++currentEviction;
		}
	}
}
