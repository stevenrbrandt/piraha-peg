package edu.lsu.cct.piraha;


public class Near {
	public String text,rule;
	public Expected expected;
	public int lineNum=1, pos, startOfLine, endOfLine;
	public Near() {}
	public String toString() {
		String line = text.substring(startOfLine,endOfLine);
		String str = line.substring(0,pos)+"|"+line.substring(pos);
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