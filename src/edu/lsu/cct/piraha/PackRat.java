package edu.lsu.cct.piraha;

import java.util.LinkedList;

/*
public static void main(String[] args) {
	Grammar g = new Grammar();
	g.compile("test","short");
	Matcher m = g.matcher("test", "short short short");
	m.startReplacement();
	while(m.find()) {
		m.appendReplacement("long");
	}
	String result = m.appendTail();
	System.out.println(result);
	System.out.println(m.mappings);
	for(int i=0;i<m.text.length();i++) {
		System.out.println(m.text.charAt(i)+" :> "+result.charAt(m.mapping(i)));
	}
}
*/
public class PackRat {
	final String name;
	final int pos;
	final int hc;
	public boolean matched;
	public int after;
	public boolean filled;
	public LinkedList<Group> subMatches;
	PackRat(String name,int pos) {
		this.name = name;
		this.pos = pos;
		this.hc = pos ^ name.hashCode();
	}
	
	@Override
	public int hashCode() { return hc; }
	
	@Override
	public boolean equals(Object o) {
		PackRat pr = (PackRat)o;
		return pr.pos == pos && pr.name.equals(name);
	}
	
	public String toString() {
		int n = -1;
		String m = "";
		if(subMatches != null) {
			n = subMatches.size();
		}
		return "PackRat("+name+","+pos+","+matched+","+after+","+n+")";
	}
}
