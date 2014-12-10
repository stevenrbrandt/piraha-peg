package edu.lsu.cct.piraha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Matcher extends Group {
	int maxId;
	private int textPos;
	int maxTextPos;
	Pattern pattern;
	public String lookup, maxLookup;
	public boolean didMatch;
	Stack<String> lookStack = new Stack<String>();
	
	public boolean find() {
		if(end == 0)
			return find(0);
		else if(begin == end)
			return find(end+1);
		else
			return find(end);
	}
	public boolean find(int pos) {
		for(int i=pos;i<text.length();i++) {
			if(match(i))
				return true;
		}
		return false;
	}
	public boolean matches() {
		return match(0);
	}
	public final int getTextPos() {
		return textPos;
	}
	public final void setTextPos(int newTextPos) {
		if(newTextPos > maxTextPos) {
			maxTextPos = newTextPos;
			maxLookup = lookStack.toString();
			expected = null;
		}
		textPos = newTextPos;
	}
	public final void incrTextPos(int incr) {
		setTextPos(getTextPos()+incr);
	}
	public boolean match(int pos) {
		expected = null;
		textPos = pos;
		maxTextPos = pos;
		savedMatches = new Stack<LinkedList<Group>>();
		rats = new HashMap<PackRat, PackRat>();
		subMatches = new LinkedList<Group>();
		if(matchAll(pattern,this)) {
			begin = pos;
			end = textPos;
			didMatch = true;
			return true;
		}
		begin = -1;
		end = -1;
		didMatch = false;
		return false;
	}
	static boolean matchAll(Pattern pattern, Matcher m) {
		/*for(Pattern x = pattern;x != null;x = x.next) {
			if(!x.match(m))
				return false;
		}
		return true;*/
		return pattern.match(m);
	}
	
	/**
	 * Just here to make things more similar to java.util.Regex
	 * @return
	 */
	public Group group() {
		return new Group(patternName,getBegin(),getEnd(),subMatches,text);
	}

	public String getText() {
		return text;
	}
	
	public Near near() {
		Near near = new Near();
		near.text = text;
		near.rule = maxLookup;
		near.expected = expected;
		for(int i=0;i<text.length() && i<maxTextPos;i++) {
			if(text.charAt(i)=='\n') {
				near.lineNum++;
				near.startOfLine = i;
			}
		}
		near.pos = maxTextPos - near.startOfLine;
		near.endOfLine = maxTextPos;
		for(; near.endOfLine < text.length();near.endOfLine++)
			if(text.charAt(near.endOfLine) == '\n')
				break;
		return near;
	}
	
	List<Mapping> mappings = null;
	StringBuffer sb = null;
	int lastAppend = 0, lastDelta = 0;
	public void startReplacement() {
		sb = new StringBuffer();
		mappings = new ArrayList<Mapping>();
		lastAppend = 0;
		lastDelta = 0;
	}
	private void addMapping(int from,int to) {
		if(from < 0) return;
		int delta = to - from;
		if(delta != lastDelta) {
			mappings.add(new Mapping(from,delta));
			lastDelta = delta;
		}
	}
	public void appendReplacement(String s) {
		sb.append(text.substring(lastAppend,begin));
		addMapping(begin,sb.length());
		addMapping(end-1,sb.length()+s.length()-1);
		sb.append(s);
		lastAppend = end;
	}
	public String appendTail() {
		if(lastAppend < text.length())
			sb.append(text.substring(lastAppend));
		lastAppend = text.length();
		return sb.toString();
	}
	public int mapping(int from) {
		int delta = 0;
		for(Mapping m : mappings) {
			if(m.from > from)
				break;
			delta = m.delta;
		}
		//System.out.println("delta["+from+"]="+delta);
		return from+delta;
	}
	public Pattern getPattern() {
		return pattern;
	}
	Map<PackRat,PackRat> rats;
	
	public PackRat find(String name,int pos) {
		PackRat pr = new PackRat(name,pos);
		PackRat res = rats.get(pr);
		if(res == null) {
			rats.put(pr, pr);
			return pr;
		} else {
			res.filled = true;
			return res;
		}
	}
	
	public void addPackRat(PackRat pr,boolean b, int after,
			LinkedList<Group> subMatches) {
		pr.matched = b;
		if(b) {
			pr.subMatches = subMatches;
		}
		pr.after = after;
	}
	
	// data for white space patterns
	public StringBuilder white = new StringBuilder();
	public List<Integer> whiteThresholds = new ArrayList<Integer>();
	
	Expected expected;
	
	public void expected(Expected ex) {
		if(textPos == maxTextPos || ex.epos == maxTextPos) {
			ex.build(maxTextPos);
			if(ex.possibilities.size()>0)
				expected = ex;
		}
	}
}
