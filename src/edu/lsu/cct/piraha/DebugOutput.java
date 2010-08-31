package edu.lsu.cct.piraha;

import java.io.PrintWriter;

public class DebugOutput {
	public int indent;
	PrintWriter pw;
	boolean newline = true;
	public DebugOutput() {
		pw = new PrintWriter(System.out);
	}
	public DebugOutput(PrintWriter pw) {
		this.pw = pw;
	}
	public static String outc(char c) {
		if(c == '\n')
			return "\\n";
		else if(c == '\r')
			return "\\r";
		else if(c == '\t')
			return "\\t";
		else if(c == '\b')
			return "\\b";
		else if(c < 10 || c >= 128) {
			String hex = Integer.toHexString(c);
			while(hex.length() < 4)
				hex = "0"+hex;
			return "\\u"+hex;
		} else {
			return Character.toString(c);
		}
	}
	public void print(Object o) {
		if(o != null) {
			if(newline) {
				for(int i=0;i<indent;i++)
					pw.print(' ');
				newline=false;
			}
			String s = o.toString();
			for(int i=0;i<s.length();i++) {
				char c = s.charAt(i);
				/*
				if(c == '\n')
					pw.print("\\n");
				else if(c == '\r')
					pw.print("\\r");
				else if(c == '\t')
					pw.print("\\t");
				else if(c == '\b')
					pw.print("\\b");
				else if(c < 10 || c >= 128) {
					String hex = Integer.toHexString(c);
					while(hex.length() < 4)
						hex = "0"+hex;
					pw.printf("\\u"+hex);
				} else
					pw.print(c);
					*/
				pw.print(outc(c));
			}
		}
		pw.flush();
	}
	public void println(Object o) {
		print(o);
		println();
	}
	public void println() {
		pw.println();
		pw.flush();
		newline = true;
	}
	public final static DebugOutput out = new DebugOutput();
}
