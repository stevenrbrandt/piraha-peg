package edu.lsu.cct.piraha;


public class Near {
	String text,rule;
	Expected expected;
	public int lineNum=1, posInLine, startOfLine, endOfLine;
	Near() {}
	public String toString() {
		String line = text.substring(startOfLine,endOfLine);
		String str = line.substring(0,posInLine)+"|"+line.substring(posInLine);
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