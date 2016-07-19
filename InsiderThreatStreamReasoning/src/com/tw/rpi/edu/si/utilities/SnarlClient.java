package com.tw.rpi.edu.si.utilities;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;

import com.complexible.common.rdf.model.Values;
import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionConfiguration;
import com.complexible.stardog.api.admin.AdminConnection;
import com.complexible.stardog.api.admin.AdminConnectionConfiguration;
import com.complexible.stardog.api.reasoning.ReasoningConnection;

public class SnarlClient {
	private AdminConnection adminConn;
	private ReasoningConnection aReasoningConn;
	private Connection aNonReasoningConn;
	private String serverURL;
	private String dbName;
	private String password;
	private String username;
	
	// constructor
	public SnarlClient(String serverURL_, String dbName_, 
			String username_, String password_) {
		serverURL = serverURL_;
		dbName = dbName_;
		username = username_;
		password = password_;
		
		adminConn = AdminConnectionConfiguration
				.toServer(serverURL)
				.credentials(username, password)
				.connect();
		
		aReasoningConn = ConnectionConfiguration
				.to(dbName)
				.server(serverURL)
				.credentials(username, password)
				.reasoning(true)
				.connect()
				.as(ReasoningConnection.class);
		
		aNonReasoningConn = ConnectionConfiguration
				.to(dbName)
				.server(serverURL)
				.credentials(username, password)
				.reasoning(false)
				.connect();

		System.out.println("[INFO] Connected to " + serverURL + dbName);
	}
	
	// connection getter
	public ReasoningConnection getAReasoningConn() { return aReasoningConn;}
	
	public Connection getANonReasoningConn() { return aNonReasoningConn;}
	
	// add model
	public void addModel(Model m, String graph_id) {
		aNonReasoningConn.begin();
		aNonReasoningConn.add().graph(m, Values.iri(graph_id));
		aNonReasoningConn.commit();
	}
	
	public void addStatement(Statement s) {
		aNonReasoningConn.begin();
		aNonReasoningConn.add().statement(s);
		aNonReasoningConn.commit();
	}
	
	// empty triplestore
	public void clearGraph() {
		String clear = "delete {graph ?g {?s ?p ?o}} "
				+ "where {graph ?g {?s ?p ?o}}";
		this.aNonReasoningConn.update(clear).execute();
	}
	
	public void emptyDB() {
		clearGraph();
		String delete = "delete{?s ?p ?o} where {?s ?p ?o}";
		this.aNonReasoningConn.update(delete).execute();
	}
	
	// drop triplestore
	public void dropDB() {
		adminConn.drop(dbName);
	}
	
	// clean up everything
	public void cleanUp() {
		adminConn.close();
		aNonReasoningConn.close();
		aReasoningConn.close();
	}
}