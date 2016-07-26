package com.tw.rpi.edu.si.utilities;

import java.time.LocalTime;

import org.openrdf.model.Statement;

public class ComparableAction implements Comparable<ComparableAction>{
	
	private Statement s;
	private Integer count;
	private Double generationTimestamp;
	private LocalTime reasoningParticipationTimestamp;
	private String mode;
	
	public ComparableAction(Statement st, double gts, int c, LocalTime t, String m) {
		s = st; 
		count = c; 
		generationTimestamp = gts; 
		reasoningParticipationTimestamp =t; 
		mode = m;
	}
	
	public Statement getStats() { return s; }
	
	public void increaseCounterByOne() { this.count += 1;}
	
	public void updateReasoningParticipationTime(LocalTime t) { this.reasoningParticipationTimestamp = t;}
	
	public void updateGenerationTimestamp(double ngts) { generationTimestamp = ngts;}
	
	public int getCount() { return this.count; }
	
	public double getGenerationTimestamp() { return this.generationTimestamp; }
	
	public LocalTime getReasoningTimestamp() { if(this.reasoningParticipationTimestamp == null) return LocalTime.MIN; return this.reasoningParticipationTimestamp;}

	@Override
	public int compareTo(ComparableAction entry) {
		// order in FIFO
		if(mode.contains("FIFO")) {
			if(generationTimestamp == entry.generationTimestamp) {
				return 0;
			}
			return generationTimestamp < entry.generationTimestamp ? -1 :1;
		}
		// order in LFU
		else if(mode.contains("LFU")) {
			if(count == entry.count && 
			   generationTimestamp == entry.generationTimestamp) { 
				return 0;
			}
			else if (count == entry.count) {
				return generationTimestamp < entry.generationTimestamp ? -1:1;
			}
			return count < entry.count ? -1 : 1;
		}
		// order in LRU
		else {
			if(reasoningParticipationTimestamp == null && entry.reasoningParticipationTimestamp == null) {
				return 0;
			}
			else if (reasoningParticipationTimestamp == null && entry.reasoningParticipationTimestamp != null) {
				return -1;
			}
			else if (reasoningParticipationTimestamp != null && entry.reasoningParticipationTimestamp == null) {
				return 1;
			}
			else if(reasoningParticipationTimestamp == entry.reasoningParticipationTimestamp && 
			   generationTimestamp == entry.generationTimestamp) {
				return 0;
			}
			else if (reasoningParticipationTimestamp == entry.reasoningParticipationTimestamp) {
				return generationTimestamp < entry.generationTimestamp ? -1:1;
			}
			return this.reasoningParticipationTimestamp.isBefore(entry.reasoningParticipationTimestamp) ? -1: 1;
		}
	}
}
