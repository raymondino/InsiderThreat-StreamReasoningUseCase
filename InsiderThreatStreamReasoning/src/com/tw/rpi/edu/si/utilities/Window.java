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
		size = Period.ofDays(1);
		step = Period.ofDays(1);
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
}
