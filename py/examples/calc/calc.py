# A simple calculator based on Piraha

import sys
from piraha import Grammar, compileFile, Matcher

g = Grammar()

# Compile a grammar from a file
compileFile(g,"calc.peg")

# Process the parse tree
def calc(gr):

    # Get the name of the rule that produced this parse tree element
    n = gr.getPatternName()

    # Process a single numeric value
    if n == "val":
        return float(gr.substring())

    # Process an operation
    elif n in ["add", "mul"]:
        v = calc(gr.group(0))
        # This could be 3 + 4 - 2 + 6 ...
        for i in range(1,gr.groupCount(),2):
            op = gr.group(i).substring()
            v2 = calc(gr.group(i+1))
            if op == "+":
                v += v2
            elif op == '-':
                v -= v2
            elif op == '*':
                v *= v2
            elif op == '/':
                v /= v2
        return v

    elif n in ["expr", "term", "paren"]:
        # Process the root element or a term in an expression
        # or a parenthetical expression
        return calc(gr.group(0))

    else:
        raise Exception(">"+n+"<")

# Create a matcher
if __name__ == "__main__":
    if len(sys.argv) == 1:
        input = "3*2 + (2+2)*1"
        print("Using sample input:",input)
    else:
        input = sys.argv[1]
    m = Matcher(g,g.default_rule, input)
    if m.matches():
        print("Success! Dump parse tree...")
        print(m.gr.dump())
        print("answer:",calc(m.gr))
    else:
        # Show a helpful error message
        m.showError()
