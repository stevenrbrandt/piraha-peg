package edu.lsu.cct.piraha;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Grammar {
	Map<String,Pattern> patterns = new HashMap<String,Pattern>();
	Map<String,Grammar> grammars = null;
	String lastCompiled;
	
	public void importGrammar(String name,Grammar g) {
		if(grammars == null) {
			grammars = new HashMap<String,Grammar>();
		}
		if(grammars.containsKey(name)) {
			throw new ParseException("Grammar '"+name+"' is already defined");
		}
		grammars.put(name, g);
	}
	
	public Pattern compile(String name,String pattern) {
		if(name.indexOf(':') >= 0)
			throw new ParseException("Illegal character ':' in pattern name");
		if(patterns.containsKey(name))
			throw new ParseException("Rule '"+name+"' is already defined.");
		Pattern pat = null;
		pat = new ReParse().compile(pattern,this);
		patterns.put(name, pat);
		lastCompiled = name;
		return pat;
	}
	
	// TODO: Make import work
	// TODO: Make a decompile work
	// TODO: Add "Grammar name" sections
	// TODO: Make a more accurate rule to match regex's
    public void compileFile(File filename) throws IOException {
        String contents = readContents(filename).trim();
        compileFile(contents);
    }
    public void compileFile(InputStream in) throws IOException {
    	StringBuilder sb = new StringBuilder();
    	BufferedInputStream bin = new BufferedInputStream(in);
    	InputStreamReader isr = new InputStreamReader(bin);
    	int c=0;
    	//while((c = isr.read()) != -1)
    	while(true) {
    		c = isr.read();
    		if(c == -1)
    			break;
    		sb.append((char)c);
    	}
    	compileFile(sb.toString());
    }
    public void compileFile(String contents) throws IOException {
    	Grammar pegRules = AutoGrammar.fileparser;
    	Matcher pegMatcher = pegRules.matcher("file",contents);
        if(!pegMatcher.matches()) {
            throw new ParseException("Syntax error near line "+pegMatcher.near()+" : "+
                    pegMatcher.text.substring(0,pegMatcher.maxTextPos)+">|<"+
                    pegMatcher.text.substring(pegMatcher.maxTextPos));
        }
        for(int i=0;i<pegMatcher.groupCount();i++) {
        	Group rule = pegMatcher.group(i);
    		Pattern ptmp = new ReParse().compile(rule.group(1), false, this);
    		lastCompiled = rule.group(0).substring();
    		patterns.put(lastCompiled,ptmp);
    		if(lastCompiled.equals("access")) {
    			rule.dumpMatches();
    		}
        }
    }

	static boolean comp(Object o1, Object o2) {
		if(o1 == o2) return true;
		if(o1 == null || o2 == null)
			return false;
		return o1.equals(o2);
	}

	public void diag(DebugOutput out) {
		DebugVisitor dv = new DebugVisitor(out);
		for(Map.Entry<String, Pattern> entry : patterns.entrySet()) {
			out.println(entry.getKey()+" => "+entry.getValue().decompile());
			dv.out.indent+=2;
			entry.getValue().visit(dv);
			dv.out.indent-=2;
		}
	}
	
	boolean checked = false;
	
	private void check() {
		CheckVisitor checker = new CheckVisitor();
		checker.retry = false;
		for (Map.Entry<String, Pattern> p : patterns.entrySet()) {
			if (checker.patterns.containsKey(p.getKey()))
				continue;
			checker.checking = p.getKey();
			p.getValue().visit(checker);
		}
		if(checker.retry) {
			checker.defaults = checker.patterns;
			checker.patterns = new HashMap<String, Boolean>();
			for (Map.Entry<String, Pattern> p : patterns.entrySet()) {
				if (checker.patterns.containsKey(p.getKey()))
					continue;
				checker.checking = p.getKey();
				p.getValue().visit(checker);
			}
		}
		checked = true;
	}
	
	public List<String> extras(String pat) {
		List<String> extraPatterns = new ArrayList<String>();
		Map<String,Boolean> visited = new HashMap<String,Boolean>();
		visited.put(pat,Boolean.FALSE);
		Visitor extraFinder = new ExtraVisitor(visited);
		while(true) {
			boolean done = true;
			Set<String> set = new HashSet<String>();
			for(String p : visited.keySet()) {
				if(visited.get(p) == Boolean.FALSE)
					set.add(p);
			}
			for(String p : set) {
				//System.out.println("visit "+p+" "+set);
				visited.put(p, Boolean.TRUE);
				patterns.get(p).visit(extraFinder);
				done = false;
			}
			if(done)
				break;
		}
		for(String p : patterns.keySet()) {
			if(!visited.containsKey(p))
				extraPatterns.add(p);
		}
		return extraPatterns;
	}
	
	public Matcher matcher(String text) {
		return matcher(lastCompiled, text);
	}

	public Matcher matcher(String patternName, String text) {
		if(!checked)
			check();
		Matcher m = new Matcher();
		m.text = text;
		m.pattern = patterns.get(patternName);
		if(m.pattern == null)
			throw new ParseException("No such pattern: '"+patternName+"'");
		m.patternName = patternName;
		return m;
	}

	public static String readContents(File file) throws IOException {
		if(!file.exists()) throw new IOException("File not found "+file);
		int fileSize = (int)file.length();
		if(fileSize == 0) return "";
		byte[] buf = new byte[fileSize];
		FileInputStream fr = new FileInputStream(file);
		int bytesRead = fr.read(buf,0,buf.length);
		fr.close();
		if(bytesRead <= 0)
			throw new IOException("Could not read entire file: "+file);
		return new String(buf,"utf8");
	}

	public Matcher matchFile(String fileName,String rule) throws IOException {
		String contents = readContents(new File(fileName));
		Matcher m = matcher(rule,contents);
		m.matches();
		return m;
	}

	public void addOps(String name, String finalExprPeg, String whitePeg, String[][] ops) {
		String prev_name = name;
		for(int i=0;i<ops.length;i++) {
			String[] opsi = ops[i];
			String op_name = opsi[0];
			String op_pat = opsi[1];
			String pat_name = op_name+"_expr";
			String pat_op_name = op_name+"_op";
			String pat_expr = "{@"+pat_name+"}("+whitePeg+"{"+pat_op_name+"}"+whitePeg+"{@"+pat_name+"})*";
			compile(prev_name,pat_expr);
			compile(pat_op_name,op_pat);
			prev_name = pat_name;
		}
		compile(prev_name,finalExprPeg);
	}
	
	public String asPEG() {
		StringBuilder sb = new StringBuilder();
		for(String p : patterns.keySet()) {
			sb.append(p);
			sb.append(" = ");
			sb.append(patterns.get(p).decompile());
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Convenience method to get a group object from a pattern name and some text
	 * @param pattern
	 * @param text
	 * @return
	 */
	public Group match(String pattern, String text) {
		Matcher m = matcher(pattern,text);
		if(m.matches())
			return m.group();
		else
			throw new MatchException(m.near().toString());
	}

	public Set<String> getPatternNames() {
		return patterns.keySet();
	}

	public Pattern getPattern(String pat) {
		return patterns.get(pat);
	}
}
