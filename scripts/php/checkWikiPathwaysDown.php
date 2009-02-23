<?php

$mailsent = false;

while(true) {
	$fp = fsockopen("www.wikipathways.org", 80, $errno, $errstr, 5);
	if (!$fp) {
		if(!$mailsent) {
			mail('thomaskelder@gmail.com', 'WikiPathways is down!!', "$errstr ($errno)\n");
			$mailsent = true;
		}
	} else {
		echo "Success!\n";
		$mailsent = false;
		fclose($fp);
	}
	sleep(2);
}

?>
