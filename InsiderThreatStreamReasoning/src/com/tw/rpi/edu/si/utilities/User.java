// a class describes a user/employee

package com.tw.rpi.edu.si.utilities;

import java.time.ZonedDateTime;

import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

public class User {
	private static String prefix = "http://tw.rpi.edu/ontology/DataExfiltration/";

	private String userID;
	private String userName;
	private String role;
	private String pc;
	private String team;
	private String supervisor;
	private Boolean resigned; // true for resignation, false for at work
	private Double trustScore;
	private Boolean excessiveRemovableDiskUser;
	
	private String statusquery;
	
	public User(String id, ZonedDateTime actionTS, SnarlClient client) {
		userID = id;
		userName = "";
		// initially, every employee has 1.0 trust score
		trustScore = 1.0;
		// construct status query based on the action timestamp
		if(actionTS.getMonthValue() / 10 == 0) {
			statusquery = "select ?name ?role ?team ?supervisor from <"+prefix+actionTS.getYear()+"-0"+actionTS.getMonthValue()+"> where {<"+prefix+id+"> <"+prefix+"hasName> ?name; <"+prefix + "hasRole> ?role; <"+prefix+"hasTeam> ?team; <"+prefix + "hasSupervisor> ?supervisor.}";
		}
		else {
			statusquery = "select ?name ?role ?team ?supervisor from <"+prefix+actionTS.getYear()+"-"+actionTS.getMonthValue()+"> where {<"+prefix+id+"> <"+prefix+"hasName> ?name; <"+prefix + "hasRole> ?role; <"+prefix+"hasTeam> ?team; <"+prefix + "hasSupervisor> ?supervisor.}";
		}
		TupleQueryResult result = client.getANonReasoningConn().select(statusquery).execute(); 
		while(result.hasNext()) {
			BindingSet bindingset = result.next();
			userName = bindingset.getValue("name").toString().substring(prefix.length());
			role = bindingset.getValue("role").toString().substring(prefix.length());
			team = bindingset.getValue("team").toString();
			supervisor = bindingset.getValue("supervisor").toString().substring(prefix.length());
		}
		result = client.getANonReasoningConn().select("select ?pc where {graph <" + prefix + "pc> { <" + prefix + id + "> <" + prefix + "hasAccessToPC> ?pc}}").execute();
		pc = result.next().getValue("pc").toString().substring(prefix.length());
		resigned = false;
		excessiveRemovableDiskUser = false; // by default		
		// if userName is empty, this user is not in current LDAP list, could resign from work
		if(userName.equals("")) {
			resigned = true;
		}
	}
	
	public void reduceTrustScore() { if(trustScore > 0) {trustScore -= 0.1;}}
	public void setExcessiveRemovableDiskUser(Boolean flag) { excessiveRemovableDiskUser = flag; }

	
	public String getID() { return userID; }
	public String getName() { return userName; }
	public String getRole() { return role; }
	public String getPC() { return pc; }
	public String getTeam() { return team; }
	public String getSupervisor() { return supervisor; }
	public Boolean getResinationStatus() { return resigned; }
	public Double getTrustScore() { return trustScore; }
	public Boolean getExcessiveRemovableDiskUser() { return excessiveRemovableDiskUser; }
}
