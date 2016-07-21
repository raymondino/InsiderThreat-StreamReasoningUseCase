package com.tw.rpi.edu.si.utilities;

import java.time.Period;
import java.time.ZonedDateTime;

public class Window {
	
	private Period size;
	private Period step;
	
	// window start and end
	private ZonedDateTime start;
	private ZonedDateTime end;
	
	// constructor
	public Window() {
		start = null; end = null;
	}
	
	// assessor
	public ZonedDateTime getStart() { return start; }
	public ZonedDateTime getEnd() { return end; }
	public Period getStep() {return step;}
	
	// modifier
	public void setStep(int s) {size = Period.ofDays(s);}
	public void setSize(int s) {step = Period.ofDays(s);}
	
	// function: window moves 1 step forward
	public void move() { 
		start = start.plus(step); 
		end = end.plus(size);
	} 
}
