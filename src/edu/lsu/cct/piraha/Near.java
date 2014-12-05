package edu.lsu.cct.piraha;


public class Near {
	public String text,rule;
	public Expected expected;
	public int lineNum=1, pos, startOfLine, endOfLine;
	public Near() {}
	public String toString() {
		int lstart = startOfLine - 50;
		if(lstart < 0) lstart = 0;
		int npos = pos + startOfLine - lstart;
		String line = text.substring(lstart,endOfLine);
		String str = line.substring(0,npos)+"|"+line.substring(npos);
		String base = "";
		if(expected != null) {
			if(expected.possibilities.size()==1) {
				base = "Expected: "+expected+" ";
			} else {
				base = "Expected one of: "+expected+" ";
			}
		}
		if(rule != null)
			return base+lineNum+" in {"+rule+"}: '"+str.trim()+"'";
		else
			return base+lineNum+": '"+str.trim()+"'";
	}
}