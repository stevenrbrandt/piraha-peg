package edu.lsu.cct.piraha;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Stack;

public class Group implements Cloneable {
	String patternName, text;
	int begin, end;
	Group replacement;
	
	public void setReplacement(Group replacement) {
		if(replacement.getPatternName().equals(patternName))
			this.replacement = replacement;
		else
			throw new MatchException("replacement group does not have patternName='"+patternName+"'");
	}
	
	public int getBegin() {
		if(replacement != null)
			return replacement.getBegin();
		return begin;
	}
	public int getEnd() {
		if(replacement != null)
			replacement.getEnd();
		return end;
	}
	LinkedList<Group> subMatches;
	Stack<LinkedList<Group>> savedMatches;
	public int groupCount() {
		if(replacement != null)
			return replacement.groupCount();
		if(subMatches == null)
			return 0;
		return subMatches.size();
	}
	public Group group(int n) {
		if(replacement != null)
			return replacement.group(n);
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
		if(replacement != null)
			return replacement.group();
		return new Group(patternName,begin,end,subMatches,text);
	}

	public String substring(String text) {
		if(replacement != null)
			return replacement.substring(text);
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
		if(replacement != null)
			return replacement.substring();
		if(begin < 0 || end < 0) return "";
		return text.substring(begin,end);
	}
	public String getPatternName() {
		return patternName;
	}
	public char charAt(int i) {
		if(replacement != null)
			return replacement.charAt(i);
		return text.charAt(i);
	}
	public String getText() {
		if(replacement != null)
			return replacement.text;
		return text;
	}
	
	public String toString() {
		return substring();
	}
	public void dumpMatchesXML() {
		if(replacement != null)
			dumpMatchesXML();
		else {
			dumpMatchesXML(DebugOutput.out);
			DebugOutput.out.pw.flush();
		}
	}
	public void dumpMatchesXML(DebugOutput out) {
		if(replacement != null) {
			replacement.dumpMatches(out);
		} else {
			out.print("<");
			out.print(getPatternName());
			out.print(" startIndex='");
			out.print(begin);
			out.print("' endIndex='");
			out.print(end);
			out.print("'>");
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
	}
	public void dumpMatches() {
		if(replacement != null) {
			replacement.dumpMatches();
		} else {
			dumpMatches(DebugOutput.out);
			DebugOutput.out.pw.flush();
		}
	}
	public void dumpMatches(DebugOutput out) {
		if(replacement != null) {
			replacement.dumpMatches(out);
		} else {
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
	public void generate(PrintWriter pw) {
		if(replacement != null) {
			replacement.generate(pw);
		} else {
			int children = groupCount();
			if(children == 0) {
				pw.print(substring());
			} else {
				pw.print(text.substring(begin,group(0).begin));
				for(int i=0;i<children;i++) {
					group(i).generate(pw);
				}
				pw.print(text.substring(group(children-1).end,end));
			}
		}
	}
	public Group get(String... args) {
		return get(0,args);
	}

	private Group get(int index, String[] args) {
		if(index >= args.length)
			return this;
		String pn = args[index];
		for(int i=0;i<groupCount();i++) {
			if(pn.equals(group(i).getPatternName())) {
				Group g = group(i).get(index+1,args);
				if(g != null)
					return g;
			}
		}
		return null;
	}
}
