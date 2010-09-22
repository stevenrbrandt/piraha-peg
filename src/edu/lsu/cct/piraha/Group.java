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
	public Group group(int n) {
		return subMatches.get(n);
	}
	
	Group() {}
	
	public final static LinkedList<Group> emptyList = new LinkedList<Group>();

	static public Group make(String lookup, String value) {
		return new Group(lookup,0,value.length(),emptyList,value);
	}
	
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

	public Group group() {
		return new Group(patternName,begin,end,subMatches,text);
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
		return substring();
	}
	public void dumpMatchesXML() {
		dumpMatchesXML(DebugOutput.out);
		DebugOutput.out.pw.flush();
	}
	public void dumpMatchesXML(DebugOutput out) {
		out.print("<");
		out.print(getPatternName());
		out.print(">");
		if(groupCount()==0) {
			out.print(xmltext(substring()));
		} else {
			out.println();
			out.indent++;
			try {
				for(Group match : subMatches) {
					match.dumpMatchesXML(out);
				}
			} finally {
				out.indent--;
			}
		}
		out.print("</");
		out.print(getPatternName());
		out.println(">");
	}
	public void dumpMatches() {
		dumpMatches(DebugOutput.out);
		DebugOutput.out.pw.flush();
	}
	public void dumpMatches(DebugOutput out) {
		out.print(getPatternName());
		if(groupCount()==0) {
			out.print("=(");
			out.outs(substring());
			out.println(")");
		} else {
			out.println(":");
			out.indent+=2;
			try {
				for(int i=0;i<groupCount();i++) {
					Group match = group(i);
					out.print("[");
					out.print(i);
					out.print("] ");
					match.dumpMatches(out);
				}
			} finally {
				out.indent-=2;
			}
		}
	}
	private static String xmltext(String str) {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<str.length();i++) {
			char c = str.charAt(i);
			if(c == '<')
				sb.append("&lt;");
			else if(c == '>')
				sb.append("&gt;");
			else if(c == '&')
				sb.append("&amp;");
			else if(c == '"')
				sb.append("&quot;");
			else if(c <= 13 || c > 127) 
				sb.append("&#"+(int)c+";");
			else
				sb.append(c);
		}
		return sb.toString();
	}
}
