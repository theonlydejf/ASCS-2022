import subprocess

def m(_base):
    _out = ""
    arr = ["A", "B", "C", "D", "E", "F"]
    for j in range(len(arr)):
        for i in range(2, 26):
            _out += _base + arr[j] + str(i) + "\t"
        _out += "\n"
    subprocess.run("pbcopy", text=True, input=_out)
    print("done")

m("='Judging room 3'!") 