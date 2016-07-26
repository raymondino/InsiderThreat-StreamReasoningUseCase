// a class describes an action of each employee

package com.tw.rpi.edu.si.utilities;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.complexible.common.openrdf.model.Models2;
import com.complexible.common.rdf.model.Values;

public class Action implements Comparable<Action> {
	private static String prefix = "http://tw.rpi.edu/ontology/DataExfiltration/";

	private String actionID;
	private String actionGraphID;
	private ZonedDateTime timestamp;
	private Boolean afterHourAction; // describes an after hour action
	private User user;
	private String pc; // action is performed on a pc
	private Boolean sharedPC; // is this pc a share pc?
	private Boolean unassignedPC; // is this pc an unassigned pc?
	private String url; // http action
	private String urlDomainType; // http action
	private String emailFrom; // email action
	private ArrayList<String> emailRecipients ; // email action (to, cc, bcc)
	private String emailAttachment; // email action
	private Boolean emailAttachmentDecoyFile; // email action
	private String fileName; // file action
	private Boolean fileADecoyFile; // file action
	private Boolean toRemovableMedia; // file action
	private Boolean fromRemovableMedia; // file action
	private String activity;
	private String content; // http/email/file action content
	// a score that models the action provenance, the smaller the better
	private Integer provenanceScore; 
	private Boolean rankByProv;
	private Boolean rankByTrust;
	
	public Action(String graphID, ZonedDateTime ts, ArrayList<User> users, SnarlClient client) {
		actionID = "";
		actionGraphID = graphID;
		timestamp = ts;
		afterHourAction = false;
		user = null;
		pc = "";
		sharedPC = false;
		unassignedPC = false;
		url = "";
		urlDomainType = "";
		emailFrom = "";
		emailRecipients = new ArrayList<String>();
		emailAttachment = "";
		emailAttachmentDecoyFile = false;
		fileName = "";
		fileADecoyFile = false;
		toRemovableMedia = false;
		fromRemovableMedia = false;
		activity = "";
		content = "";
		/**
		 * provenanceScore increments when: 
		 * 1. after hour action 
		 * 2. user is an excessive usb user
		 * 3. action is performed at an unassigned pc
		 * 4. urlDomainType is cloudstorage/hactivist/jobhunting website
		 * 5. email address not ending with dtaa.com
		 * 6. email attachment is a decoy file
		 * 7. file in file_action is a decoy file
		 * 8. file copied to/from disk/pc
		 */
		provenanceScore = 0;
		rankByProv = false;
		rankByTrust = false;
		
		String infoQuery = "select distinct ?action ?userid ?pc from <"+graphID+"> where {?action <"+prefix+"hasActor> ?actor; <"+prefix+"isPerformedOn> ?pc.}";
		TupleQueryResult result = client.getANonReasoningConn().select(infoQuery).execute();
		while(result.hasNext()) {
			BindingSet bindingset = result.next();
			// get action id
			actionID = bindingset.getValue("action").toString().substring(prefix.length());
			// get pc
			pc = bindingset.getValue("pc").toString().substring(prefix.length());
			// get user
			Boolean userExist = false;
			for(User u:users) {
				if(u.getID().equals(bindingset.getValue("userid").toString().substring(prefix.length()))) {
					user = u;
					userExist = true;
				}
			}
			if(!userExist) {
				user = new User(bindingset.getValue("userid").toString().substring(prefix.length()), ts, client);
				users.add(user);
			}
			if(client.getANonReasoningConn().ask("ask from<" + prefix + "> { <"+prefix + user.getID()+"> a <"+prefix+"ExcessiveRemovableDriveUser>}").execute()) {
				user.setExcessiveRemovableDiskUser(true);
				provenanceScore++;
			}
			if(client.getANonReasoningConn().ask("ask from <"+ graphID +"> {<" + prefix + actionID + "> a <" + prefix + "AfterHourAction>.}").execute()) {
				afterHourAction = true;
				provenanceScore++;
			}
		}
		// if an action pc is different from the user's assigned pc
		if(!pc.equals(user.getPC())) {
			if(client.getANonReasoningConn().ask("ask { graph <" + prefix + "pc> { <" + prefix + pc +"> a <" + prefix + "SharedPC>.}}").execute()) {
				sharedPC = true;
			}
			else {
				unassignedPC = true;
				// if it is an unassigned pc, add this statement into the action graph
				client.addModel(Models2.newModel(Values.statement(Values.iri(prefix+user.getID()),Values.iri(prefix + "isPerformedOnUnassignedPC "), Values.iri(prefix+pc))), graphID);
				provenanceScore++;
			}
		} 			
		// for http action
		if (actionID.contains("http_")) {
			String url = "select distinct ?url ?dn from <"+graphID+"> where {?s <"+prefix+"hasURL> ?url. ?url <"+prefix+"whoseDomainNameIsA> ?dn.}";
			String activity = "select distinct ?o from <"+graphID+"> where {<"+prefix+actionID+"> a ?o.}";
			TupleQueryResult re = client.getANonReasoningConn().select(url).execute();
			while(re.hasNext()){
				BindingSet x = re.next();
				url = x.getValue("url").toString();
				urlDomainType = x.getValue("dn").toString().substring(prefix.length());
				if(urlDomainType.equals("cloudstoragewebsite") || urlDomainType.equals("hacktivistwebsite") || urlDomainType.equals("jobhuntingwebsite")) {
					provenanceScore++;
				}
			}
			re = client.getANonReasoningConn().select(activity).execute();
			while(re.hasNext()) {
				BindingSet x = re.next();
				if(x.getValue("o").toString().contains("WWW")) {
					activity = x.getValue("o").toString().substring(prefix.length());
				}
			}		
		}
		// for email action
		else if (actionID.contains("email_")) {
			String activity = "select distinct ?o from <"+graphID+"> where {<"+prefix+actionID+"> a ?o.}";
			String from = "select distinct ?o from <"+graphID+"> where {?s <"+prefix+"from> ?o.}";
			String to = "select distinct ?o from <"+graphID+"> where {?s <"+prefix+"to> ?o.}";
			String cc = "select distinct ?o from <"+graphID+"> where {?s <"+prefix+"cc> ?o.}";
			String bcc = "select distinct ?o from <"+graphID+"> where {?s <"+prefix+"bcc> ?o.}";
			String attachment = "select distinct ?o from <"+graphID+"> where {?s <"+prefix+"hasEmailAttachment> ?o.}";
			TupleQueryResult re = client.getANonReasoningConn().select(from).execute();
			while(re.hasNext()) {
				BindingSet x = re.next();
				emailFrom = x.getValue("o").toString().substring(prefix.length());
				if(!x.getValue("o").toString().substring(prefix.length()).contains("@dtaa.com")) {
					provenanceScore++;
				}
			}
			re = client.getANonReasoningConn().select(to).execute();
			while(re.hasNext()) {
				BindingSet x = re.next();
				emailRecipients.add(x.getValue("o").toString().substring(prefix.length()));
				if(!x.getValue("o").toString().substring(prefix.length()).contains("@dtaa.com")) {
					provenanceScore++;
				}
			}
			re = client.getANonReasoningConn().select(cc).execute();
			while(re.hasNext()) {
				BindingSet x = re.next();
				emailRecipients.add(x.getValue("o").toString().substring(prefix.length()));
				if(!x.getValue("o").toString().substring(prefix.length()).contains("@dtaa.com")) {
					provenanceScore++;
				}
			}
			re = client.getANonReasoningConn().select(bcc).execute();
			while(re.hasNext()) {
				BindingSet x = re.next();
				emailRecipients.add(x.getValue("o").toString().substring(prefix.length()));
				if(!x.getValue("o").toString().substring(prefix.length()).contains("@dtaa.com")) {
					provenanceScore++;
				}
			}				
			re = client.getANonReasoningConn().select(attachment).execute();
			while(re.hasNext()) {
				BindingSet x = re.next();
				emailAttachment = x.getValue("o").toString().substring(prefix.length());
				if(client.getANonReasoningConn().ask("ask from <"+prefix+"decoy> {<"+x.getValue("o").toString()+"> a <" + prefix + "DecoyFile>}").execute()) {
					this.emailAttachmentDecoyFile = true;
					provenanceScore++;
				}
			}
			re = client.getANonReasoningConn().select(activity).execute();
			while(re.hasNext()) {
				BindingSet x = re.next();
				if(x.getValue("o").toString().contains("Email")) {
					activity = x.getValue("o").toString().substring(prefix.length());
				}
			}
		}
		// for file action
		else if (actionID.contains("file_")) {
			String activity = "select distinct ?o from <"+graphID+"> where {<"+prefix+actionID+"> a ?o.}";
			String file = "select distinct ?fn from <"+graphID+"> where {?s <"+prefix+"hasFile> ?fn.}";
			String filetype = "select distinct ?type from <"+graphID+"> where {?s <"+prefix+"hasFile> ?fn. ?fn a ?type}";
			TupleQueryResult re = client.getANonReasoningConn().select(file).execute();
			while(re.hasNext()){
				BindingSet x = re.next();
				fileName = x.getValue("fn").toString().substring(prefix.length());
				if(client.getANonReasoningConn().ask("ask from <"+prefix+"decoy> {<"+x.getValue("fn").toString()+"> a <" + prefix + "DecoyFile>}").execute()) {
					fileADecoyFile = true;	
					provenanceScore++;
				}
			}
			re = client.getANonReasoningConn().select(filetype).execute();
			while(re.hasNext()){
				BindingSet x = re.next();
				String type = x.getValue("type").toString().substring(prefix.length());
				if (type.equals("FileToRemovableMedia")) {
					toRemovableMedia = true;
					provenanceScore++;
				}
				else if(type.equals("FileFromRemovableMedia")){
					fromRemovableMedia = true;
					provenanceScore++;
				}
			}
			re = client.getANonReasoningConn().select(activity).execute();
			while(re.hasNext()) {
				BindingSet x = re.next();
				if(x.getValue("o").toString().contains("File")) {
					activity = x.getValue("o").toString().substring(prefix.length());						
				}
			}			
		}
	}

	public void setRankByProv() { rankByProv = true; rankByTrust = false; }
	public void setRankByTrust() { rankByProv = false; rankByProv = true; }
	
	public String getActionID() { return actionID; }
	public String getActionGraphID() { return actionGraphID; }
	public ZonedDateTime getTimestamp() { return timestamp; }
	public Boolean getAfterHourAction() { return afterHourAction; }
	public User getUser() { return user; }
	public String getPc() { return pc; }
	public Boolean getSharedPC() { return sharedPC; }
	public Boolean getUnassignedPC() { return unassignedPC; }
	public String getUrl() { return url; }
	public String getUrlDomainType() { return urlDomainType; }
	public String getEmailFrom() { return emailFrom; }
	public ArrayList<String> getEmailRecipients() { return emailRecipients; }
	public String getEmailAttachment() { return emailAttachment; }
	public Boolean getEmailAttachmentDecoyFile() { return emailAttachmentDecoyFile; }
	public String getFileName() { return fileName; }
	public Boolean getFileADecoyFile() { return fileADecoyFile; }
	public Boolean getToRemovableMedia() { return toRemovableMedia; }
	public Boolean getFromRemovableMedia() { return fromRemovableMedia; }
	public String getActivity() { return activity; }
	public String getContent() { return content; }
	public Integer getProvenanceScore() { return provenanceScore; }
	public Boolean isRankByProv() { return rankByProv; }
	public Boolean isRankByTrust() { return rankByProv = true; }

	@Override
	public int compareTo(Action a) {
		// rank by action's provenance score
		if(rankByProv && !rankByTrust) { // a max heap
			if(provenanceScore == a.getProvenanceScore()) {
				return 0;
			}
			else
				return provenanceScore > a.getProvenanceScore() ? -1 : 1;
		}
		// rank by action's user's trust score
		else if(rankByTrust && !rankByProv) { // a min heap
			if(user.getTrustScore() == a.getUser().getTrustScore()) {
				return 0;
			}
			else 
				return user.getTrustScore() < a.getUser().getTrustScore() ? -1 : 1;
		}
		return 0;
	}
}

