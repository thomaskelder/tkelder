#!/bin/bash

nsheets=$(xls2txt -l "$1" | wc -l)
echo Found $nsheets sheets
let "nsheets = $nsheets-1"

for s in `seq 0 $nsheets`
do
	name=$1_$s.txt
	echo Exporting sheet $s to $name
    xls2txt -n $s -f "$1" > "$name"
done

