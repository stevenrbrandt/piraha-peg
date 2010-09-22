package edu.lsu.cct.piraha.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.lsu.cct.piraha.Grammar;
import edu.lsu.cct.piraha.Group;
import edu.lsu.cct.piraha.Matcher;

public class Cpp {
	Map<String,Group> defines = new HashMap<String,Group>();
	List<String> includes = new ArrayList<String>();
	Stack<IfElseState> output = new Stack<IfElseState>();
	Set<String> mfiles = new HashSet<String>();
	PrintWriter out;
	
	public void setOutput(String file) throws IOException {
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		out = new PrintWriter(bw);
	}
	
	/**
	 * This makes it easy to catch all defines being made for debugging purposes. 
	 */
	private void setDef(String s,Group g) {
		defines.put(s, g);
	}
	Grammar g = new Grammar();
	Cpp() {
		g.compile("scomment","//.*");
		g.compile("dcomment","//.*|/\\*((?!\\*/)[^])*\\*/");
		g.compile("quote","\"(\\\\[^]|[^\\\\\"])*\"");
		g.compile("squote","'(\\\\[^]|[^\\\\'])*'");
		g.compile("w","[ \t\r\b]|\\\\\n");
		g.compile("num", "-?[0-9]+L?");
		g.compile("sp","{-scomment}|{-dcomment}|{-w}");
		g.compile("args","({-w}*{word}({-w}*,{-w}*{word})*|){-w}*");
		g.compile("def","define{-w}+{word}({-w}*\\({args}\\)|)");
		g.compile("file","[^<> \t\r\n\b]+");
		g.compile("inc","include{-w}*({quote}|<{file}>)");
		g.compile("ifdef","ifdef{-w}+{word}");
		g.compile("ifndef","ifndef{-w}+{word}");
		g.compile("if","if{-w}+");
		g.compile("warning","warning{-w}+");
		g.compile("error","error({-w}+|\\b)");
		g.compile("elif","elif{-w}+");
		g.compile("else","else");
		g.compile("undef","undef{-w}+{word}");
		g.compile("pragma","pragma");
		g.compile("endif","endif");
		g.compile("join","({target}|{num}|{argvalues}){-sp}*##{-sp}*({target}|{num}|{argvalues})");
		g.compile("stringify","#{-sp}*{word}");
		g.compile("macro","{-w}*#{-w}*({def}|{inc}|{ifdef}|{ifndef}|{else}|{undef}|{endif}|{if}|{elif}|{error}|{warning}|{pragma})({defined}|{join}|{stringify}|{-elem})*");
		g.compile("other","[^,\\(#\"\\)\n \t\r\ba-zA-Z0-9_\\\\]+");
		g.compile("argval","({defined}|{join}|{stringify}|{-elem}|\n)*");
		g.compile("argvalues","\\({argval}(,{argval})*\\)");
		g.compile("target","{word}({-sp}*{argvalues}|)");
		g.compile("elem", "({scomment}|{dcomment}|{quote}|{argvalues}|{target}|{squote}|{w}+|{num}|{other})");
		g.compile("word","(?!\\bdefined\\b)(?i:[a-z_][a-z0-9_]*)");
		g.compile("comma",",");
		g.compile("line","({macro}|({-elem}|{comma})*)(\n|$)");
		g.compile("defined","\\bdefined{-w}*(\\({-w}*{word}{-w}*\\)|\\b{word})");
		g.compile("rematch","^({join}|{stringify}|{-elem})*$");
		g.compile("ifmatch","^({defined}|{-elem})*");
		
		IfParse.buildGrammar(g);
		//System.out.println("extras="+g.extras("line"));
		output.push(IfElseState.TRUE);
		out = new PrintWriter(System.out);
	}

	File findFile(String file) throws IOException {
		File f = new File(file);
		if(f.exists())
			return f;
		for(String incDir : includes) {
			File d = new File(incDir);
			f = new File(d,file);
			if(f.exists())
				return f;
		}
		throw new IOException("could not find file "+file+" included from "+currentFile);
	}
	
	public void addInclude(String inc) {
		includes.add(inc);
	}
	
	boolean modified;
	
	File currentFile;
	public void doFile(String file) throws IOException {
		File save = currentFile;
		try {
			doFile_(file);
		} finally {
			currentFile = save;
		}
	}
	public void finish() {
		out.close();
		if(mflag)
			System.out.println();
	}
	public void doFile_(String file) throws IOException {
		File f = findFile(file);
		if(mflag && !mfiles.contains(f.getAbsolutePath())) {
			if(mfiles.size()==0) {
				System.out.print(f);
				System.out.print(" : ");
			} else {
				System.out.println(" \\");
				System.out.print(' ');
				System.out.print(f.getAbsolutePath());
			}
			mfiles.add(f.getAbsolutePath());
		}
		currentFile = f;
		String contents = Grammar.readContents(f);
		Matcher m = g.matcher("line",contents);
		int pos = 0;
		while(m.match(pos)) {
			if(pos == m.getEnd())
				throw new Error("fail "+m.near());
			//System.out.println("pos="+pos+"/"+m.getEnd()+"/"+contents.length());
			pos = m.getEnd();
			//System.out.println("===");
			//m.dumpMatches();
			//System.out.println("==="+m.getPatternName());
			if(m.groupCount() > 0 && "macro".equals(m.group(0).getPatternName())) {
				processMacros(m.group(0));
			} else if(output.peek()==IfElseState.TRUE) {
				StringBuffer sb = new StringBuffer();
				modified = false;
				buildString(sb,m);
				while(modified) {
					Matcher m2 = g.matcher("rematch", sb.toString());
					if(m2.matches()) {
						//System.out.println("inter:"+sb);
						sb = new StringBuffer();
						modified = false;
						buildString(sb,m2);
					} else {
						throw new Error(m2.near().toString());
					}
				}
				out.println(sb);
			}
			if(pos == contents.length())
				break;
		}
		if(pos < contents.length())
			throw new Error("parse error in file "+f+" line "+m.near());
	}
	
	private void buildString(StringBuffer sb,Group g) {
		String m = g.getPatternName();
		if("dcomment".equals(m)||"scomment".equals(m)||"w".equals(m)) {
			sb.append(' ');
		} else if("target".equals(m)) {
			//System.out.println("target: "+g.substring());
			if(g.groupCount()==1) {
				String key = g.group(0).substring();
				if(defines.containsKey(key)) {
					Group gg = defines.get(key);
					if(gg.groupCount()>0 && gg.group(0).groupCount()>1 && gg.group(0).group(1).getPatternName().equals("args")) {
						sb.append(key);
					} else {
						modified = true;
						if(gg.groupCount()==0||"argval".equals(gg.getPatternName())) {
							buildString(sb,gg);
						} else {
							for(int i=1;i<gg.groupCount();i++)
								buildString(sb,gg.group(i));
						}
						//System.out.println("key:"+key+"="+sb.substring(n));
					}
				} else {
					sb.append(key);
				}
			} else {
				Map<String,Group> defSaves = new HashMap<String,Group>();
				String key = g.group(0).substring();
				if(defines.containsKey(key)) {
					modified = true;
					Group gg = defines.get(key);
					boolean arityMatch = false;
					Group args = null;
					if(gg.groupCount() > 0 && gg.group(0).groupCount()>1) {
						args = gg.group(0).group(1);
						if(args.groupCount() == g.group(1).groupCount())
							arityMatch = true;
					} else {
						args = gg;
					}
					if(!arityMatch) {
						gg.dumpMatches();
						System.out.println(gg.substring());
						throw new Error("Arity mismatch for function "+key+" def="+args.groupCount()+" != use="+g.group(1).groupCount()+" "+g.near());
					}
					for(int i=0;i<args.groupCount();i++) {
						String var = args.group(i).substring();
						if(defines.containsKey(var)) {
							defSaves.put(var,defines.get(var));
						}
						setDef(var, g.group(1).group(i));
					}
					for(int i=1;i<gg.groupCount();i++)
						buildString(sb, gg.group(i));
					for(int i=0;i<args.groupCount();i++) {
						String var = args.group(i).substring();
						if(defSaves.containsKey(var)) {
							setDef(var,defSaves.get(var));
						} else {
							defines.remove(var);
						}
					}
				} else {
					//g.dumpMatches();
					sb.append(key);
					sb.append("(");
					for(int i=0;i<g.group(1).groupCount();i++) {
						if(i>0) sb.append(",");
						buildString(sb,g.group(1).group(i));
					}
					sb.append(")");
					for(int i=2;i<g.groupCount();i++)
						buildString(sb,g.group(i));
				}
			}
		} else if("argvalues".equals(m)) {
			sb.append('(');
			for(int i=0;i<g.groupCount();i++) {
				if(i>0) sb.append(',');
				buildString(sb,g.group(i));
			}
			sb.append(')');
		} else if("line".equals(m)||"argval".equals(m)||"rematch".equals(m)||"ifmatch".equals(m)) {
			for(int i=0;i<g.groupCount();i++)
				buildString(sb,g.group(i));
		} else if("other".equals(m)||"num".equals(m)||"quote".equals(m)||"defined".equals(m)) {
			sb.append(g.substring());
		} else if("join".equals(m)) {
			Group g1 = g.group(0);
			Group g2 = g.group(g.groupCount()-1);
			String k1 = g1.substring();
			String k2 = g2.substring();
			boolean gg1 = false, gg2 = false;
			if(defines.containsKey(k1)) {
				gg1 = true;
			}
			if(defines.containsKey(k2)) {
				gg2 = true;
			}
			if(gg1 || gg2) {
				if(gg1)
					buildString(sb,g1);
				else
					sb.append(k1);
				sb.append("##");
				if(gg2)
					buildString(sb,g2);
				else
					sb.append(k2);
			} else {
				sb.append(k1);
				sb.append(k2);
				modified = true;
			}
			//modified = true;
		} else if("stringify".equals(m)) {
			Group g2 = g.group(g.groupCount()-1);
			String k2 = g2.substring();
			if(defines.containsKey(k2))
				k2 = defines.get(k2).substring();
			sb.append('"');
			sb.append(k2);
			sb.append('"');
		} else if("comma".equals(m)) {
			sb.append(',');
		} else {
			throw new Error(m);
		}
	}
	
	private void processMacros(Group g) throws IOException {
		String m = g.group(0).getPatternName();
		//System.out.println("macro "+g.substring());
		if("def".equals(m)) {
			if (output.peek()==IfElseState.TRUE) {
				if (g.groupCount() == 0)
					throw new Error(g.near().toString());
				String var = g.group(0).group(0).substring();
				Group rem = remap(g.group());
				setDef(var, rem);
			}
		} else if("inc".equals(m)) {
			if (output.peek()==IfElseState.TRUE) {
				//g.dumpMatches();
				String n = g.group(0).group(0).substring();
				//System.out.println(g.group(0).getPatternName());
				if ("file".equals(g.group(0).group(0).getPatternName())) {
					doFile(n);
				} else {
					doFile(n.substring(1, n.length() - 2));
				}
			}
		} else if("undef".equals(m)) {
			if(output.peek()==IfElseState.TRUE) {
				defines.remove(g.group(0).group(0).substring());
			}
		} else if("ifdef".equals(m)) {
			if(output.peek()==IfElseState.TRUE) {
				boolean def = defines.containsKey(g.group(0).group(0).substring());
				output.push(def ? IfElseState.TRUE : IfElseState.FALSE);
			} else {
				output.push(IfElseState.FALSE);
			}
		} else if("ifndef".equals(m)) {
			if(output.peek()==IfElseState.TRUE) {
				boolean def = !defines.containsKey(g.group(0).group(0).substring());
				output.push(def ? IfElseState.TRUE : IfElseState.FALSE);
			} else {
				output.push(IfElseState.FALSE);
			}
		} else if("else".equals(m)) {
			IfElseState top = output.pop();
			if(output.peek()!=IfElseState.TRUE) {
				output.push(top);
				return;
			}
			if(top==IfElseState.TRUE || top==IfElseState.FALSE_BUT_HAS_BEEN_TRUE) {
				output.push(IfElseState.FALSE_BUT_HAS_BEEN_TRUE);
			} else {
				output.push(IfElseState.TRUE);
			}
		} else if("warning".equals(m)) {
			if(output.peek()==IfElseState.TRUE) {
				System.err.println(g.substring());
			}
		} else if("elif".equals(m)) {
			IfElseState top = output.pop();
			if(output.peek() != IfElseState.TRUE) {
				output.push(top);
				return;
			}
			if(top == IfElseState.TRUE || top==IfElseState.FALSE_BUT_HAS_BEEN_TRUE) {
				output.push(IfElseState.FALSE_BUT_HAS_BEEN_TRUE);
			} else {
				processIf(g);
			}
		} else if("error".equals(m)) {
			if(output.peek()==IfElseState.TRUE) {
				throw new RuntimeException("Error directive processessed: in file "+currentFile+" near line "+g.near());
			}
		} else if("if".equals(m)) {
			if(output.peek()==IfElseState.TRUE) {
				processIf(g);
			} else {
				output.push(IfElseState.FALSE);
			}
		} else if("endif".equals(m)) {
			output.pop();
		} else if("pragma".equals(m)) {
			// ignore
		} else {
			throw new Error(m+" => error near line "+g.near());
		}
	}

	private void processIf(Group g) {
		StringBuffer sb = new StringBuffer();
		sb.append(g.getText().substring(g.group(0).getEnd(), g.getEnd()));
		do {
			modified = false;
			Matcher m2 = this.g.matcher("ifmatch", sb.toString());
			if(m2.matches()) {
				sb = new StringBuffer();
				buildString(sb,m2);
			}
		} while(modified);
		boolean b = IfParse.parse(this,sb.toString());
		output.push(b ? IfElseState.TRUE : IfElseState.FALSE);
	}
	
	/**
	 * Keeps track of whether the -M flag was set.
	 */
	private boolean mflag = false;

	int varNum = 1;
	/**
	 * This renames all the arguments in use by a macro.
	 * foo(A) A*A becomes foo($12) $12*$12, and so on. This
	 * prevents namespace clashes.
	 * @param g
	 * @return
	 */
	private Group remap(Group g) {
		if(g.groupCount() > 1 && g.group(0).groupCount() > 1 && 
				g.group(0).group(1).getPatternName().equals("args")) {
			Map<String,String> map = new HashMap<String,String>();
			Group args = g.group(0).group(1);
			for(int i=0;i<args.groupCount();i++) {
				String val = args.group(i).substring();
				map.put(val,"$"+(varNum++));
			}
			return remap(g,map);
		} else
			return g;
	}

	private Group remap(Group g, Map<String, String> map) {
		if(g.groupCount()==0) {
			if(g.getPatternName().equals("word")) {
				String key = g.substring();
				if(map.containsKey(key)) {
					String val = map.get(key);
					return Group.make("word",val);
				} else {
					return g;
				}
			} else {
				return g;
			}
		} else {
			LinkedList<Group> subs = new LinkedList<Group>();
			for(int i=0;i<g.groupCount();i++) {
				subs.add(remap(g.group(i),map));
			}
			return new Group(g.getPatternName(), g.getBegin(), g.getEnd(), subs, g.getText());
		}
	}

	public static void main(String[] args) throws Exception {
		Cpp cpp = null;
		try {
			cpp = new Cpp();
			
			cpp.addInclude("/usr/include");

			List<String> files = new ArrayList<String>();
			for(int i=0;i<args.length;i++) {
				if(args[i].startsWith("-I")) {
					cpp.addInclude(args[i].substring(2));
				} else if(args[i].startsWith("-D")) {
					String arg = args[i].substring(2);
					int n = arg.indexOf("=");
					if(n < 0)
						cpp.setDef(arg,"1");
					else
						cpp.setDef(arg.substring(0,n),arg.substring(n+1));
				} else if(args[i].equals("--x86_64redhat4.4.1")) {
					cpp.addInclude("/usr/include/gnu");
					cpp.addInclude("/usr/lib/gcc/x86_64-redhat-linux/4.4.1/include");
					cpp.addInclude("/usr/lib/gcc/x86_64-redhat-linux/4.4.1/../../../../include/c++/4.4.1");
					cpp.addInclude("/usr/lib/gcc/x86_64-redhat-linux/4.4.1/../../../../include/c++/4.4.1/x86_64-redhat-linux");
					cpp.setDef("__x86_64__","1");
				} else if(args[i].equals("-M")) {
				} else if(args[i].startsWith("-o")) {
					cpp.setOutput(args[i].substring(2));
				} else {
					files.add(args[i]);
				}
			}
			
			for(String file : files)
				cpp.doFile(file);
			
		} finally {
			cpp.finish();
		}
	}
	public void setDef(String key,String val) {
		setDef(key,Group.make("word",val));
	}
	
	/**
	 * Set the -M flag.
	 */
	public void setMFlag() {
		setOutput(new PrintWriter(new StringWriter()));
		mflag  = true;
	}
	public void setOutput(PrintWriter out) {
		this.out = out;
	}
}
