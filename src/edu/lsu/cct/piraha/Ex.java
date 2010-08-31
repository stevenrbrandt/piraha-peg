package edu.lsu.cct.piraha;

public class Ex {
	public static void main(String[] args)  {
		Grammar g = new Grammar();
		String patternName = "bx";
		String pattern = "<{bx}>|x";
		String textToMatch = "<<x>>";
		g.compile(patternName,pattern);
		Matcher matcher = g.matcher(patternName,textToMatch);
		if(matcher.matches()) {
			for(Group m : matcher.subMatches) {
				System.out.println(m.patternName+", "+m.substring(matcher.text));
			}   
		}
	}
}
