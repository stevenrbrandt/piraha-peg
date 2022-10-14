from colored import colored
import os

_here = os.path.realpath(os.getcwd())

def here(*args):
    import inspect
    stack = inspect.stack()
    frame = stack[1]
    fname = os.path.realpath(frame.filename)
    if fname.startswith(_here):
        fname = fname[len(_here)+1:]
    print(colored("HERE:","cyan"),fname+":"+colored(frame.lineno,"yellow"), *args, flush=True)
    frame = None
    stack = None

if __name__ == "__main__":
    here(_here)
