package edu.lsu.cct.piraha;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import edu.lsu.cct.util.Here;


public class ReParse {
	private static final int INDENT_INCR = 2;
	
	/** The grammar used by this reparser. */
	Grammar g = AutoGrammar.reparser;
	
	/**
	 * This method constructs the Grammar for Piraha in Piraha.
	 * It has two forms. The first is for a compilation within a
	 * java program, the second (file=true) is for reading in a
	 * peg file.
	 * 
	 * @param file
	 */
	public void init(boolean file) {
		g = new Grammar();
		if(file)
			g.compile("literal","\\\\u{hex}|\\\\[^b1-9]|[^\\\\|()\\[\\]*+{}?$^\\u000a\\u000d.]");
		else
			g.compile("literal","\\\\u{hex}|\\\\[^b]|[^\\\\|()\\[\\]*+{}?$^.]");
		g.compile("hex","[a-fA-F0-9]{4}");
		g.compile("neg","\\^");
		g.compile("dot","\\.");
		g.compile("backref","\\\\[1-9]");
		g.compile("echar","-");
		g.compile("cchar","\\\\u{hex}|\\\\[^]|[^\\\\\\]-]");
		g.compile("range","{cchar}-{cchar}");
		g.compile("charclass","\\[{neg}?({range}|{echar})?({range}|{cchar})*{echar}?\\]");
		g.compile("pelem", "(({named}|{dot}|{backref}|{literal}|{charclass}|{group})({quant}|)|({start}|{end}|{boundary}))");
		if(file) {
			g.compile("pattern","({group_top}|)");
			g.compile("s","([ \t\r\n]|#.*)+");
			g.compile("w","[ \t]*");
			g.compile("s0","[ \t]+");
			g.compile("rule","{name}{-w}={-w}{pattern}");
			g.compile("file","^{-s}?{rule}({-s}?{rule})*{-s}?$");
			g.compile("group_top","{pelems_top}{pelems_next}*({s}?{nothing}\\||)");
			g.compile("group_inside","{pelems}(\\|{pelems})*({s0}?{nothing}\\||){s}?");
			g.compile("pelems","({s}?{pelem})+{s}?");
			g.compile("pelems_top","{pelem}({s0}?{pelem})*");
			g.compile("pelems_next","{s}?\\|{s}?{pelem}({s0}?{pelem})*");
		} else {
			g.compile("pattern","^({group_inside}|)$");
			g.compile("group_inside","{pelems}(\\|{pelems})*({nothing}\\||)");
			g.compile("pelems","{pelem}({pelem})*");
		}
		g.compile("ign_on","\\?i:");
		g.compile("ign_off","\\?-i:");
		g.compile("lookahead","\\?=");
		g.compile("neglookahead","\\?!");
		g.compile("pipe", "");
		g.compile("nothing", "");
		g.compile("group","\\(({ign_on}|{ign_off}|{lookahead}|{neglookahead}|)({group_inside}|)\\)");
		g.compile("num", "[0-9]+");
		g.compile("quantmax",",{num}?");
		g.compile("quant", "\\+|\\*|\\?|\\{{num}{quantmax}?\\}");
		g.compile("name","-?[a-zA-Z:_][a-zA-Z0-9:_]*");
		g.compile("named", "\\{{name}\\}");
		g.compile("start","\\^");
		g.compile("end","\\$");
		g.compile("boundary","\\\\b");
	}
	
	
	void dumpGrammar(PrintWriter out) {
		for(String pat : g.getPatternNames()) {
			out.print("g.patterns.put(\""+pat+"\",");
			Pattern p = g.getPattern(pat);
			dumpPattern(p,out);
			out.println(");");
		}
	}
	
	int indentLevel = 0;
	/**
	 * Helper class for creating the AutoGrammar file. Ensures
	 * proper indentation and improves readability.
	 * 
	 * @author sbrandt
	 *
	 */
	class IndentWriter extends Writer {
		
		Writer w;
		
		IndentWriter(Writer w) { this.w = w; }

		@Override
		public void close() throws IOException {
			w.close();
		}

		@Override
		public void flush() throws IOException {
			w.flush();
		}

		@Override
		public void write(char[] arg0, int arg1, int arg2) throws IOException {
			for(int i=arg1;i<arg2;i++)
				out(arg0[i]);
		}
		
		
		void out(int c) throws IOException {
			w.write(c);
			if(c == '\n') {
				for(int i=0;i<indentLevel;i++)
					w.write(' ');
			}
		}
		
	}
	
	/**
	 * Dump a pattern to a string. Used to generate the AutoGrammar. 
	 * @param p
	 * @param out
	 * @throws Error
	 */
	private void dumpPattern(Pattern p,PrintWriter out) throws Error {
		if(p instanceof Literal) {
			Literal lit = (Literal)p;
			out.print("new Literal('"+Group.escChar(lit.ch)+"')");
		} else if(p instanceof Seq) {
			Seq s = (Seq)p;
			indentLevel += INDENT_INCR;
			if(!s.ignCase && !s.igcShow)
				out.println("new Seq(");
			else
				out.println("new Seq("+s.ignCase+","+s.igcShow+",");
			for(int i=0;i<s.patternList.size();i++) {
				if(i>0) out.println(",");
				dumpPattern(s.patternList.get(i),out);
			}
			indentLevel -= INDENT_INCR;
			out.print(")");
		} else if(p instanceof Multi) {
			Multi m = (Multi)p;
			out.print("new Multi(");
			dumpPattern(m.pattern,out);
			out.print(","+m.min+","+m.max+")");
		} else if(p instanceof Bracket) {
			Bracket b = (Bracket)p;
			indentLevel += INDENT_INCR;
			out.println("new Bracket("+b.neg+")");
			for(int i=0;i<b.ranges.size();i++) {
				Range r = b.ranges.get(i);
				out.println(".addRange('"+Group.escChar(r.lo)+"','"+Group.escChar(r.hi)+"')");
			}
			indentLevel -= INDENT_INCR;
		} else if(p instanceof Or) {
			Or or = (Or)p;
			indentLevel += INDENT_INCR;
			if(!or.ignCase && !or.igcShow)
				out.println("new Or(");
			else
				out.println("new Or("+or.ignCase+","+or.igcShow+",");
			for(int i=0;i<or.patterns.size();i++) {
				if(i>0) out.println(',');
				dumpPattern(or.patterns.get(i),out);
			}
			indentLevel -= INDENT_INCR;
			out.print(")");
		} else if(p instanceof Lookup) {
			Lookup lk = (Lookup)p;
			out.print("new Lookup(\""+(lk.capture ? "" : "-")+lk.lookup+"\",g)");
		} else if(p instanceof Start) {
			out.print("new Start()");
		} else if(p instanceof End) {
			out.print("new End()");
		} else if(p instanceof Nothing) {
			out.print("new Nothing()");
		} else if(p instanceof NegLookAhead) {
			NegLookAhead neg = (NegLookAhead)p;
			out.print("new NegLookAhead(");
			dumpPattern(neg.pattern, out);
			out.print(")");
		} else if(p instanceof Dot) {
			out.print("new Dot()");
		} else {
			throw new Error(p.getClass().getName());
		}
	}
	
	/** A quick test of the grammar defined in this file. */
	static ReParse createAndTest() {
		ReParse r = new ReParse();
		r.init(false);
		test(r,"(?=a)");
		test(r,"[0-3a-d]");
		test(r,"(d|)");
		test(r,"(({a}|{b})|{c})");
		test(r,"(({named}|{literal}|{charclass}|{group})({quant}|)|({start}|{end}|{boundary}))");
		test(r,"<{d}>|x","(<{d}>|x)");
		test(r,"a*");
		test(r,"b+");
		test(r,"c{3}");
		test(r,"d{1,4}");
		test(r,"e{9,}");
		test(r,"hello");
		test(r,"hell{4}o\\n\\t\\r\\u012f\\\\");//|w+o?od*e{3,}n{1,2})");
		test(r,"(?i:abc)");
		test(r,"(?-i:abc)");
		test(r,"(?!abc)");
		test(r,"^a");
		test(r,"a$");
		test(r,"a\\b ");
		test(r,"[a-z]");
		test(r,"[z-]","[-z]");
		test(r,"[-a]");
		test(r,"[a-df]");
		test(r,"[\\u0123-\\u0129]");
		test(r,"(\\[[^\\]]*)+");
		return r;
	}
	static void test(ReParse r,String s) {
		test(r,s,null);
	}
	static void test(ReParse r,String s,String s2) {
		Pattern p = r.compile(s,new Grammar());
		System.out.print("decomp="+p.decompile());
		if(p.decompile().equals(s2))
			return;
		if(!s.equals(p.decompile())) {
			DebugVisitor dv = new DebugVisitor(new DebugOutput());
			dv.startVisit(p);
			throw new Error(s+" != "+p.decompile());
		}
	}
	
	
	public static void main(String[] args) throws IOException {
		ReParse p = createAndTest();
		p.generateFile(args[0]);
	}
	
	void generateFile(String autoFile) throws IOException {
		init(false);
		
		StringWriter sw = new StringWriter();
		indentLevel = 0;
		PrintWriter pw = new PrintWriter(new IndentWriter(sw));
		pw.println("/** Autogenerated code, created by ReParse. Do not edit. */");
		pw.println();
		pw.println("package edu.lsu.cct.piraha;");
		pw.println();
		indentLevel += INDENT_INCR;
		pw.println("public class AutoGrammar {");
		pw.println();
		
		init(false);
		pw.println("public final static Grammar reparser = reparserGenerator();");
		pw.println();
		indentLevel += INDENT_INCR;
		pw.println("private static Grammar reparserGenerator() {");
		pw.println("Grammar g = new Grammar();");
		dumpGrammar(pw);
		indentLevel -= INDENT_INCR;
		pw.println("return g;");
		pw.println("}");
		pw.println();
		
		init(true);
		pw.println("public final static Grammar fileparser = fileparserGenerator();");
		pw.println();
		indentLevel += INDENT_INCR;
		pw.println("private static Grammar fileparserGenerator() {");
		pw.println("Grammar g = new Grammar();");
		dumpGrammar(pw);
		indentLevel -= INDENT_INCR;
		pw.println("return g;");
		indentLevel -= INDENT_INCR;
		pw.println("}");

		pw.println("}");
		pw.close();
		String contents = sw.toString();
		
		File f = new File(autoFile);
		boolean write = true;
		if(f.exists()) {
			StringBuilder sb = new StringBuilder((int)f.length());
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			while(true) {
				int c= br.read();
				if(c < 0)
					break;
				sb.append((char)c);
			}
			br.close();
			if(sb.toString().equals(contents))
				write = false;
		}
		if(write) {
			FileWriter fw = new FileWriter(f);
			fw.write(contents);
			fw.close();
		}
	}
	
	/** Compiles a string into a pattern. */
	public Pattern compile(String s,Grammar gram) {
		Matcher m = g.matcher("pattern",s);
		if(m.matches()) {
			m.dumpMatches();
			return compile(m.group(),false,gram);
		} else {
			throw new ParseException(m.near().toString());
		}
	}
	
	/** Compiles a parse tree to a pattern. */
	Pattern compile(Group g,boolean ignCase,Grammar gram) {
		String pn = g.getPatternName();
		if("literal".equals(pn)) {
			char c = getChar(g);
			if(ignCase)
				return new ILiteral(c);
			else
				return new Literal(c);
		} else if("pattern".equals(pn)) {
			if(g.groupCount()==0)
				return new Nothing();
			return compile(g.group(0),ignCase,gram);
		} else if("pelem".equals(pn)) {
			if(g.groupCount()==2) {
				Multi m = mkMulti(g.group(1));
				m.pattern = compile(g.group(0),ignCase,gram);
				return m;
			}
			return compile(g.group(0),ignCase,gram);
		} else if("pelems".equals(pn)||"pelems_top".equals(pn)||"pelems_next".equals(pn)) {
			List<Pattern> li = new ArrayList<Pattern>();
			for(int i=0;i<g.groupCount();i++) {
				li.add(compile(g.group(i),ignCase,gram));
			}
			if(li.size()==1)
				return li.get(0);
			return new Seq(li,false,false);
		} else if("group_inside".equals(pn)||"group_top".equals(pn)) {
			if(g.groupCount()==1)
				return compile(g.group(0),ignCase,gram);
			List<Pattern> li = new ArrayList<Pattern>();
			for(int i=0;i<g.groupCount();i++) {
				li.add(compile(g.group(i),ignCase,gram));
			}
			Or or = new Or(false,false);
			or.patterns = li;
			return or;
		} else if("group".equals(pn)) {
			Or or = new Or();
			boolean ignC = ignCase;
			Group inside = null;
			if(g.groupCount()==2) {
				ignC = or.igcShow = true;
				String ps = g.group(0).getPatternName();
				if(ps.equals("ign_on")) {
					ignC = or.ignCase = true;
				} else if(ps.equals("ign_off")) {
					ignC = or.ignCase = false;
				} else if(ps.equals("neglookahead")) {
					return new NegLookAhead(compile(g.group(1),ignCase,gram));
				} else if(ps.equals("lookahead")) {
					return new LookAhead(compile(g.group(1),ignCase,gram));
				}
				inside = g.group(1);
			} else {
				inside = g.group(0);
			}
			for(int i=0;i<inside.groupCount();i++) {
				or.patterns.add(compile(inside.group(i),ignC,gram));
			}
			if(or.igcShow == false && or.patterns.size()==1)
				return or.patterns.get(0);
			return or;
		} else if("start".equals(pn)) {
			return new Start();
		} else if("end".equals(pn)) {
			return new End();
		} else if("boundary".equals(pn)) {
			return new Boundary();
		} else if("charclass".equals(pn)) {
			Bracket br = new Bracket();
			int i=0;
			if(g.groupCount()>0 && g.group(0).getPatternName().equals("neg")) {
				i++;
				br.neg = true;
			}
			for(;i<g.groupCount();i++) {
				String gn = g.group(i).getPatternName();
				if("range".equals(gn)) {
					char c0 = getChar(g.group(i).group(0));
					char c1 = getChar(g.group(i).group(1));
					br.addRange(c0, c1, ignCase);
				} else {
					char c = getChar(g.group(i));
					br.addRange(c,c, ignCase);
				}
			}
			return br;
		} else if("named".equals(pn)) {
			String lookup = g.group(0).substring();
			if("brk".equals(lookup))
				return new Break();
			return new Lookup(lookup, gram);
		} else if("nothing".equals(pn)) {
			return new Nothing();
		} else if("s".equals(pn)||"s0".equals(pn)) {
			return new Lookup("-skipper", gram);
		} else if("dot".equals(pn)) {
			return new Dot();
		} else if("backref".equals(pn)) {
			return new BackRef(g.substring().charAt(1)-'0', ignCase);
		} else {
			throw new Error("unknown pattern "+pn);
		}
	}
	private char getChar(Group gr) {
		if(gr.groupCount()==1)
			return (char)Integer.parseInt(gr.group(0).substring(),16);
		String gs = gr.substring();
		if(gs.length()==2) {
			char c = gs.charAt(1);
			if(c == 'n')
				return '\n';
			else if(c == 'r')
				return '\r';
			else if(c == 't')
				return '\t';
			else if(c == 'b')
				return '\b';
			else
				return c;
		} else {
			return gs.charAt(0);
		}
	}
	private Multi mkMulti(Group g) {
		if(g.groupCount()==0) {
			String s = g.substring();
			if("*".equals(s)) {
				return new Multi(0,Integer.MAX_VALUE);
			} else if("+".equals(s)) {
				return new Multi(1,Integer.MAX_VALUE);
			} else if("?".equals(s)) {
				return new Multi(0,1);
			} else {
				throw new Error(s);
			}
		} else if(g.groupCount()==1) {
			int mn = Integer.parseInt(g.group(0).substring());
			return new Multi(mn,mn);
		} else if(g.groupCount()==2) {
			int mn = Integer.parseInt(g.group(0).substring());
			if(g.group(1).groupCount()>0) {
				int mx = Integer.parseInt(g.group(1).group(0).substring());
				return new Multi(mn,mx);
			} else {
				return new Multi(mn,Integer.MAX_VALUE);
			}
		}
		g.dumpMatches();
		throw new Error();
	}
}
