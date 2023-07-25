 Until the loinc entries are correctly filled (currently missing `display` entry), this can be fixed by regex replace
```regexp
^(\d+)\t(http:\/\/loinc.org)\t(.*)$
```
with
```regexp
$1\t$2\t$3\t\t$3
```
