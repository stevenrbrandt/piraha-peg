# A simple calculator based on Piraha

import sys
from piraha import Grammar, compileFile, Matcher

g = Grammar()

# Compile a grammar from a file
compileFile(g,"calc.peg")

values = {}

# Process the parse tree
def calc(gr):
    global values

    # Get the name of the rule that produced this parse tree element
    n = gr.getPatternName()

    # Process the read of a variable
    if n in ["name"]:
        return values[gr.substring()]

    # Process a single numeric value
    elif n == "val":
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

    elif n in ["expr"]:
        v = None
        for i in range(gr.groupCount()):
            v = calc(gr.group(i))
        return v

    elif n in ["term", "paren"]:
        # Process a term in an expression
        # or a parenthetical expression
        return calc(gr.group(0))

    elif n in ["assign"]:
        varname = gr.group(0).substring()
        value = calc(gr.group(1))
        values[varname] = value

    else:
        raise Exception(">"+n+"<")

# Create a matcher
if __name__ == "__main__":
    if len(sys.argv) == 1:
        input_file = "calc.in"
        print("Using sample input file:",input_file)
    else:
        input_file = sys.argv[1]
    with open(input_file, "r") as fd:
        input = fd.read()
    m = Matcher(g,g.default_rule, input)
    if m.matches():
        print("Success! Dump parse tree...")
        print(m.gr.dump())
        print("answer:",calc(m.gr))
    else:
        # Show a helpful error message
        m.showError()
