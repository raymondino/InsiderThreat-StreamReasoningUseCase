package com.tw.rpi.edu.si.utilities;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.PriorityQueue;

public class Window {
	private static String prefix = "http://tw.rpi.edu/ontology/DataExfiltration/";

	private Period size;
	private Period step;
	// pair of action graph id and timestamp
	private LinkedHashMap<String, ZonedDateTime> content; 
	private PriorityQueue<Action> actions; // rank actions
	SnarlClient client;
	
	// window start and end
	private ZonedDateTime start;
	private ZonedDateTime end;
	
	// constructor
	public Window() {
		size = Period.ofDays(7);
		step = Period.ofDays(1);
		content = new LinkedHashMap<String, ZonedDateTime>();
		actions = new PriorityQueue<Action>();
	}	
	public Window(SnarlClient c) {
		size = Period.ofDays(7);
		step = Period.ofDays(1);
		content = new LinkedHashMap<String, ZonedDateTime>();
		client = c;
	}
	
	// assessor
	public ZonedDateTime getStart() { return start; }
	public ZonedDateTime getEnd() { return end; }
	public Period getStep() {return step;}
	
	// modifier
	public void setStep(int s) {step = Period.ofDays(s);}
	public void setSize(int s) {size = Period.ofDays(s); end = start.plus(size);}
	public void setStart(ZonedDateTime s) {start = s; end = start.plus(size);}
	
	// function: window moves 1 step forward
	public void move() { 
		start = start.plus(step); 
		end = end.plus(size);
	} 
	// function: window reads data
	public void load(String graphid, ZonedDateTime ts, Action a) {
		content.put(graphid, ts);
		actions.add(a);
	}
	// function: window process data
	public void process() {
		
	}
	// function: window evicts data
	public void evict() {
		
	}
}
