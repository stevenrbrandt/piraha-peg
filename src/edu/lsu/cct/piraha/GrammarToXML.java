package edu.lsu.cct.piraha;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class GrammarToXML {
	public static void main(String[] args) throws Exception {
		if(args.length < 4 || !args[0].endsWith(".peg") || !args[2].endsWith(".xml")) {
			System.err.println("usage: GrammarToXML pegfile rule outputxmlfile srcfiles");
			System.exit(1);
		}
		Grammar g = new Grammar();
		g.compileFile(new File(args[0]));
		//g.diag(new DebugOutput());
		String rule = args[1];
		String xmlFile = args[2];
		DebugOutput out = new DebugOutput(new PrintWriter(new BufferedWriter(new FileWriter(xmlFile))));
		for(int i=3;i<args.length;i++) {
			String src = args[i];
			System.out.println("src "+src);
			Matcher m = g.matchFile(src,rule);
			if(!m.didMatch) {
				throw new Exception("Syntax error near line: "+m.near());
			}
			m.dumpMatchesXML(out);
		}
		out.flush();
	}
}
