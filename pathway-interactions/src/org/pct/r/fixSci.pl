## Removes scientific notation from numbers in input text file ##
while (<>) {
	$r = "([0-9\.]+[eE]{1}[+-]?[0-9]+)";
	
	if(/$r/) {
		$d = sprintf("%.12f", $1);
		$_ =~ s/$r/$d/;
		print $_;
	} else {
		print $_;
	}
}
