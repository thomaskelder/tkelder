<?php

$client = new SoapClient("http://www.wikipathways.org/wpi/webservice/webservice.php?wsdl");
$pws = $client->listPathways();
$tags = $client->getCurationTagsByName(array('tagName' => 'Curation:FeaturedPathway'));

$tagged = array();
foreach($tags->tags as $tag) {
	$tagged[] = $tag->pathway->id;
}

foreach($pws->pathways as $p) {
	if(!in_array($p->id, $tagged)) {
		echo($p->url . "\n");
	}
}
?>
