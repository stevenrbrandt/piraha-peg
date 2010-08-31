package edu.lsu.cct.piraha;

import java.util.LinkedList;
import java.util.Stack;

public class Group implements Cloneable {
	String patternName, text;
	int begin, end;
	
	public int getBegin() {
		return begin;
	}
	public int getEnd() {
		return end;
	}
	LinkedList<Group> subMatches;
	Stack<LinkedList<Group>> savedMatches;
	public int groupCount() {
		return subMatches.size();
	}
	public Group getMatch(int n) {
		return subMatches.get(n);
	}

	Group() {}
	
	public Group(String lookup, int before, int after, LinkedList<Group> matches,String text) {
		this.patternName = lookup;
		this.begin = before;
		this.end = after;
		this.subMatches = matches;
		this.text = text;
	}
	public Group(String patternName, Group m) {
		this.patternName = patternName;
		this.begin = m.begin;
		this.end = m.end;
		this.subMatches = m.subMatches;
		this.text = m.text;
	}

	@Override
	public Object clone() {
		return new Group(patternName,begin,end,(LinkedList<Group>)subMatches.clone(),text);
	}

	public String substring(String text) {
		return text.substring(begin,end);
	}

	public Matcher.Near near() {
		Matcher.Near near = new Matcher.Near();
		near.text = text;
		for(int i=0;i<text.length() && i<begin;i++) {
			if(text.charAt(i)=='\n') {
				near.lineNum++;
				near.startOfLine = i;
			}
		}
		near.posInLine = begin - near.startOfLine;
		for(near.endOfLine = begin; near.endOfLine < text.length();near.endOfLine++)
			if(text.charAt(near.endOfLine) == '\n')
				break;
		return near;
	}
	public String substring() {
		return text.substring(begin,end);
	}
	public String getPatternName() {
		return patternName;
	}
	public char charAt(int i) {
		return text.charAt(i);
	}
	public String getText() {
		return text;
	}
	
	public String toString() {
		return getPatternName();
	}
}
