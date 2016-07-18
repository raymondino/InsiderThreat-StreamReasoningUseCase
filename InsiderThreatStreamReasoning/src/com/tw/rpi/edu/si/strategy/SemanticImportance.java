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
	
	private String data;
	private String prefix;
	private SnarlClient client;	
	
	// constructor
	public SemanticImportance(String d, SnarlClient c, String p) {
		data = d; client = c; prefix = p;
	}
	
	// run function
	public void run() {
		// read the streaming data action by action
	}
}
