package edu.lsu.cct.piraha;

public class Near {
	String text,rule;
	int lineNum=1, posInLine, startOfLine, endOfLine;
	public String toString() {
		String line = text.substring(startOfLine,endOfLine);
		String str = line.substring(0,posInLine)+"|"+line.substring(posInLine);
		if(rule != null)
			return ""+lineNum+" in {"+rule+"}: '"+str.trim()+"'";
		else
			return ""+lineNum+": '"+str.trim()+"'";
	}
}