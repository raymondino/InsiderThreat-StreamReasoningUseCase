// this code leverages trust and provenance to detect threatening insiders

package com.tw.rpi.edu.si.strategy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import com.complexible.common.openrdf.model.Models2;
import com.complexible.common.rdf.model.Values;
import com.tw.rpi.edu.si.utilities.Action;
import com.tw.rpi.edu.si.utilities.SnarlClient;
import com.tw.rpi.edu.si.utilities.User;
import com.tw.rpi.edu.si.utilities.Window;

public class ProvTrustSI {
	private static String prefix = "http://tw.rpi.edu/ontology/DataExfiltration/";

	private SnarlClient client;
	private ArrayList<User> users;
	private Window window;
	private String currentActionGraphID; // records current action id
	private ZonedDateTime currentActioinTS; // records current action timestamp
	private BufferedReader br; 	// to read data from file for stream simulation
	private Boolean SIprov; // SI: rank by provenance
	private Boolean SItrust; // SI: rank by trust
	private PrintWriter metricwriter; // output the results
	
	// constructor
	public ProvTrustSI(String datapath, SnarlClient c){
		client = c;
		// a default window: size = 7 days, step = 1 day
		window = new Window(c);
		window.setSize(1); 
		currentActionGraphID = "";
		currentActioinTS = null;
		users = new ArrayList<User>();
		try {
			this.br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(datapath))));
		} catch (FileNotFoundException e1) {
			System.out.println("[ERROR]: streaming data path is invalid:" + datapath);
			e1.printStackTrace();
		}
		
		// semantic importance setup
		SIprov = true;
		SItrust = false;
//	    SIprov = false;
//	    SItrust = true;
		
		// setup file for suspicious actions result
		String resultPath = "suspiciousActionList_windowSize-" + window.getSize().getDays() ;
		if(SIprov) {
			resultPath = "data/result/"+resultPath +"_prov.txt";	
		}
		else {
			resultPath = "data/result/"+resultPath +"_trust.txt";
		}
		try {
			File suspiciousActionList = new File(resultPath);
			suspiciousActionList.delete();
			metricwriter = new PrintWriter(resultPath, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// run
	public void run() {
		window.setMetricWriter(metricwriter);
		// read the streaming data action by action
		System.out.println("[INFO] reading the data");
		String data = "";
		try {
			while((data = br.readLine()) != null) {
				String [] parts = data.split(" ");
				String s = parts[0];
				String p = parts[1];
				String o = parts[2];
				
				// if current data hasn't reach action data
				if(p.equals(prefix + "isInvolvedIn")) {
					client.addModel(Models2.newModel(Values.statement(Values.iri(s), Values.iri(p), Values.iri(o))), prefix+"actor-event");	
					continue;
				}		
				// if data comes with a timestamp, then this is a new action
				if(data.charAt(data.length()-1) != '.') {
					// last action info will be fully added into db when current data is read
					// so we need to construct last action, and add it into the window
					if(!currentActionGraphID.equals("")) {
						Action action = new Action(currentActionGraphID, currentActioinTS, users, client); 
						if(SIprov) { action.setRankByProv();} // rank by provenance
						if(SItrust) { action.setRankByProv();} // rank by trust
						window.load(currentActionGraphID, currentActioinTS, action);
						try {
							window.process();
						}
						catch (Exception e) {
							System.out.println();
							System.out.print("[EXCEPTION] ");
							System.out.println(action.getActionID());
							metricwriter.flush();
							e.printStackTrace();
						}
					}
					currentActionGraphID = prefix + "graph/" + o.substring(prefix.length());
					currentActioinTS = ZonedDateTime.parse(parts[4]+"-05:00"); // EST time zone
					client.addModel(Models2.newModel(Values.statement(Values.iri(s), Values.iri(p), Values.iri(o))), currentActionGraphID);					
					continue;
				}
				// keep loading data of one action
				if(o.contains("http")) { // if o is a url
					client.addModel(Models2.newModel(Values.statement(Values.iri(s), Values.iri(p), Values.iri(o))), currentActionGraphID);
				}
				else { // if o is a literal
					client.addModel(Models2.newModel(Values.statement(Values.iri(s), Values.iri(p), Values.literal(o))), currentActionGraphID);
				}
			}
			metricwriter.close();
		} catch (IOException e) { e.printStackTrace(); }
	}
}