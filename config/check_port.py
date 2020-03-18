import os

os.system("netstat -anp | grep 8212 | grep LISTEN | awk 'FNR == 1 {print $7; exit}'")
