<?php
/* Abort if called from a web server */
if ( isset( $_SERVER ) && array_key_exists( 'REQUEST_METHOD', $_SERVER ) ) {
	print "This script must be run from the command line\n";
	exit();
}

$mydir = getcwd();
chdir("../"); //Ugly, but we need to change to the MediaWiki install dir to include these files, otherwise we'll get an error
require_once('wpi.php');
chdir($mydir);
require_once('phplot/phplot.php');

set_time_limit(0);


#### Generate some statistics for WikiPathways ####
$interval = "month";
$date = "20070401010000";

$counts = countNewPathways($date, $interval, array("Curation:Tutorial", "Curation:FeaturedPathway"), false);
plotCounts($counts, "new_pathways_$interval.png", array("All pathways", "Deleted pathways", "Tagged as tutorial pathway", "Tagged as featured pathway"), 
	"New pathways created per $interval");

$counts = countEdits($date, $interval);
plotCounts(array('Pathways' => $counts['NoTutorial'], 'Tutorial / test pathways' => array_subtract($counts['Users'], $counts['NoTutorial'])), 
	"edit_count_users_$interval.png", '', "User edit counts per $interval (excluding bots)", "stackedbars");
plotCounts(array_slice($counts, 1, 2, true), 
	"edit_count_$interval.png", '', "Edit counts per $interval", "stackedbars");

$counts = countUsers($date, $interval);
plotCounts($counts, "active_users_$interval.png", '', "Active user counts per $interval"); 

$interval = "hour";
$date = "20090203200000";
$end = "20090205000000";
$counts = countWebserviceByAction($date, $interval, $end);
plotCounts($counts, "webservice_$interval.png", '', "Web service calls per $interval", "stackedbars");
$counts = countWebserviceByIp($date, $interval, $end, 10);
plotCounts($counts, "webservice_ip_$interval.png", '', "Web service calls per $interval", "stackedbars");
$counts = countWebserviceIpTotals($date, $end, 20);
plotPie($counts, "webservice_ips.png", "Web service calls for $date - $end");

function countWebserviceIpTotals($start, $end, $cutoff = 0) {
	$dbr =& wfGetDB(DB_SLAVE);

	if(!$end) $end = wfTimestamp(TS_MW);
	
	$counts = array();
	$res = $dbr->query(
		"SELECT ip FROM webservice_log " .
		"WHERE request_timestamp > '$start' AND request_timestamp <= '$end'"
	);
	while($row = $dbr->fetchRow($res)) {
		$ip = $row['ip'];
			$counts[$ip] += 1;
	}
	$dbr->freeResult($res);
	
	$newCounts = array();
	
	if($cutoff) {
		foreach(array_keys($counts) as $k) {
			if($counts[$k] <= $cutoff) {
				$newCounts['Other'] += 1;
			} else {
				$newCounts[$k] = $counts[$k];
			}
		}
	} else {
		$newCounts = $counts;
	}
	return $newCounts;
}

function countWebserviceByIp($start, $interval, $end = '', $cutoff = 0) {
	$dbr =& wfGetDB(DB_SLAVE);

	$nextTime = $start;
	if(!$end) $end = wfTimestamp(TS_MW);
	
	$counts = array();
	
	while($currTime <= $end) {
		$currTime = $nextTime;
		$nextTime = incrementTime($currTime, $interval);
		
		$counts[''][$currTime] = 0;
		
		$res = $dbr->query(
			"SELECT ip FROM webservice_log " .
			"WHERE request_timestamp > '$currTime' AND request_timestamp <= '$nextTime'"
		);

		while($row = $dbr->fetchRow($res)) {
			$ip = $row['ip'];
			$counts[$ip][$currTime] += 1;
		}
		$dbr->freeResult($res);
	}
	
	$newCounts = array();
	
	if($cutoff) {
		foreach(array_keys($counts) as $ip) {
			foreach(array_keys($counts[$ip]) as $t) {
				if($counts[$ip][$t] <= $cutoff) {
					$newCounts['Other'][$t] += 1;
				} else {
					$newCounts[$ip][$t] = $counts[$ip][$t];
				}
			}
		}
	} else {
		$newCounts = $counts;
	}
	
	return $newCounts;
}

function countWebserviceByAction($start, $interval, $end = '') {
	$dbr =& wfGetDB(DB_SLAVE);
	
	$operations = array(
		"listOrganisms",
		"listPathways", 
		"getPathway",
		"getPathwayInfo",
		"getPathwayHistory",
		"getRecentChanges",
		"login",
		"getPathwayAs",
		"updatePathway",
		"createPathway",
		"findPathwaysByText",
		"findPathwaysByXref",
		"removeCurationTag",
		"saveCurationTag",
		"getCurationTags",
		"getCurationTagHistory",
		"getColoredPathway",
		"findInteractions",
		"getXrefList",
		"findPathwaysByLiterature",
		"wsdl",
	);
	
	$nextTime = $start;
	if(!$end) $end = wfTimestamp(TS_MW);
	
	$counts = array();
	
	while($currTime <= $end) {
		$currTime = $nextTime;
		$nextTime = incrementTime($currTime, $interval);
		
		foreach($operations as $o) $counts[$operation][$currTime] = 0;
		
		$res = $dbr->query(
			"SELECT operation FROM webservice_log " .
			"WHERE request_timestamp > '$currTime' AND request_timestamp <= '$nextTime'"
		);

		while($row = $dbr->fetchRow($res)) {
			$operation = $row['operation'];
			foreach($operations as $o) {
				if(preg_match("/($o)/", $operation, $match)) {
					$operation = $match[1];
					break;
				}
			}
			$counts[$operation][$currTime] += 1;
		}
		$dbr->freeResult($res);
	}
	return $counts;
}

function countUsers($timestamp, $interval) {
	$dbr =& wfGetDB(DB_SLAVE);
	$ns = NS_PATHWAY;
	
	$nextTime = $timestamp;
	
	$counts = array();
	$tutorial = MetaTag::getPagesForTag("Curation:Tutorial");
	$users = array();
	
	while($currTime <= wfTimestamp( TS_MW )) {
		$currTimcutoffe = $nextTime;
		$nextTime = incrementTime($currTime, $interval);
		
		$res = $dbr->query("SELECT user_id FROM user WHERE user_registration > '$currTime'");
		
		while($row = $dbr->fetchRow($res)) {
			$uid = $row['user_id'];
			$user = $users[$uid];
			if(!$user) {
				$user = User::newFromId($uid);
				$users[$uid] = $user;
			}
			
			$isTotal = 0;
			$isTutorial = 0;

			if(!$user->isBot() && !$user->isAnon()) {
				$res2 = $dbr->query(
					"SELECT rev_id, rev_page FROM revision LEFT JOIN page ON page.page_id = revision.rev_page " .
					"WHERE page.page_namespace = $ns AND " .
					"revision.rev_timestamp > '$currTime' AND revision.rev_timestamp <= '$nextTime' AND " .
					"revision.rev_user = $uid"
				);
				while($row2 = $dbr->fetchRow($res2)) {
					$isTotal = 1;
					if(!in_array($row2['rev_page'], $tutorial)) {
						$isTutorial = 1;
						break;
					}
				}
				$dbr->freeResult($res2);
			}
			
			$counts["Active users"][$currTime] += $isTotal;
			$counts["Active users - excluding tutorial pathways"][$currTime] += $isTutorial;
		}
		$dbr->freeResult($res);
	}
	return $counts;
}

function countEdits($timestamp, $interval) {
	$dbr =& wfGetDB(DB_SLAVE);
	$ns = NS_PATHWAY;

	$nextTime = $timestamp;
	
	$counts = array();
	
	$tutorial = MetaTag::getPagesForTag("Curation:Tutorial");
	
	while($currTime <= wfTimestamp( TS_MW )) {
		$currTime = $nextTime;
		$nextTime = incrementTime($currTime, $interval);
		$res = $dbr->query(
			"SELECT rev_id, rev_page, rev_user FROM revision LEFT JOIN page ON page.page_id = revision.rev_page " .
			"WHERE page.page_namespace = $ns AND " .
			"revision.rev_timestamp > '$currTime' AND revision.rev_timestamp <= '$nextTime'"
		);
		$countTotal = 0;
		$countUser = 0;
		$countBot = 0;
		$countTut = 0;
		while($row = $dbr->fetchRow($res)) {
			$countTotal += 1;
			$user = User::newFromId($row['rev_user']);
			if($user->isBot() || $user->isAnon()) {
				$countBot += 1;
			} else {
				$countUser += 1;
				if(!in_array($row['rev_page'], $tutorial)) {
					$countTut += 1;
				}
			}
		}
		$dbr->freeResult($res);
		
		$counts['Total'][$currTime] = $countTotal;
		$counts['Users'][$currTime] = $countUser;
		$counts['Bots'][$currTime] = $countBot;
		$counts['NoTutorial'][$currTime] = $countTut;
	}
	return $counts;
}

/**
 * Lists new pathways per interval since the given time.
 * Excludes deleted pathways, and new pathways that are result of redirects.
 * @param $interval Interval is either 'day', 'week', 'month', or 'year'
 */
function countNewPathways($timestamp, $interval, $perTag = array(), $excludeDeleted = true) {
	$dbr =& wfGetDB(DB_SLAVE);
	
	$res = $dbr->select(
		"page", array("page_id"), 
		array("page_namespace"=> NS_PATHWAY)
	);
	
	// Get all first pathway revisions
	
	$pathways = array(); //Key: pathway id, Val: pathway object
	$firstRevision = array(); //Key: pathway id, Val: first revision timestamp
	
	$tagFilters = array();
	foreach($perTag as $t) {
		$tagFilters[$t] = MetaTag::getPagesForTag($t);
	}
	
	while($row = $dbr->fetchRow($res)) {
		$title = Title::newFromId((int)$row["page_id"]);
		$pathway = pathwayFromPage($title);
		if($pathway) {
			if($excludeDeleted && $pathway->isDeleted()) {
				continue;
			}
			$pwId = $pathway->getIdentifier();
			$pathways[$pwId] = $pathway;
			$first = $firstRevision[$pwId];
			if($first) {
				$first = min($first, getFirstRevision($title)->getTimestamp());
			} else {
				$first = getFirstRevision($title)->getTimestamp();
			}
			$firstRevision[$pwId] = $first;
		}
	}
	$dbr->freeResult($res);
	
	// Count pathways per interval
	$counts = array(); //Key: 'All' or tag name, Val: array with date=>count
	
	$currTime = $timestamp;
	$nextTime = incrementTime($currTime, $interval);
	
	asort($firstRevision);
	foreach(array_keys($firstRevision) as $pwId) {
		$pathway = new Pathway($pwId);
		
		$pwTime = $firstRevision[$pwId];
		while($pwTime >= $nextTime) {
			$currTime = $nextTime;
			$nextTime = incrementTime($currTime, $interval);
			$counts['All'][$currTime] = 0;
			if(!$excludeDeleted) {
				$counts['Deleted'][$currTime] = 0;
			}
			foreach($perTag as $t) {
				$counts[$t][$currTime] = 0;
			}
		}
		$counts['All'][$currTime] = $counts['All'][$currTime] + 1;
		if(!$excludeDeleted && $pathway->isDeleted()) {
			$counts['Deleted'][$currTime] = $counts['Deleted'][$currTime] + 1;
		}
		foreach($perTag as $t) {
			if(in_array($pathway->getTitleObject()->getArticleID(), $tagFilters[$t])) {
				$counts[$t][$currTime] = $counts[$t][$currTime] + 1;
			}
		}
	}
	return $counts;
}

function plotPie($counts, $file, $title) {
	$plot = new PHPlot(1024, 800);
	$plot->setImageBorderType('plain');
	
	$data = array();
	foreach(array_keys($counts) as $t) {
		$data[] = array($t, $counts[$t]);
	}
	
	$plot->SetDataColors(array(
		"YellowGreen", "brown", "ivory", "orchid",
		"salmon", "yellow", "SlateBlue", "blue", "grey",
		"orange", "red", "SkyBlue", "green", "navy", "purple",
		"wheat", "PeachPuff", "beige", "maroon", "plum",
		"azure1", "magenta", "pink", "tan", "DarkGreen",
		"aquamarine1", "cyan", "peru"
	));
	$plot->SetPlotType('pie');
	$plot->SetDataType('text-data-single');
	$plot->SetDataValues($data);

	# Set enough different colors;
	$plot->SetDataColors(array('red', 'green', 'blue', 'yellow', 'cyan',
		                    'magenta', 'brown', 'lavender', 'pink',
		                    'gray', 'orange'));

	# Main plot title:
	$plot->SetTitle($title);

	# Build a legend from our data array.
	# Each call to SetLegend makes one line as "label: value".
	foreach ($data as $row)
	  $plot->SetLegend(implode(': ', $row));

	echo("Plotting to " . realpath($file) . "\n");
	$plot->SetIsInline(true);
	$plot->SetOutputfile($file);
	$plot->DrawGraph();
}

function plotCounts($counts, $file, $labels = '', $title, $type = 'bars') {
	$plot = new PHPlot(1024, 800);
	$plot->setImageBorderType('plain');
	
	$data = array();
	foreach(array_keys($counts) as $label) {
		foreach(array_keys($counts[$label]) as $t) {
			if(!$data[$t]) {
				$data[$t] = array(wfTimestamp(TS_UNIX, $t));
			}
			$data[$t][] = $counts[$label][$t];
		}
	}
	$data = array_values($data); //Remove keys
	
	if(!$labels) $labels = array_keys($counts);
	
	$plot->SetDataColors(array(
		"YellowGreen", "brown", "ivory", "orchid",
		"salmon", "yellow", "SlateBlue", "blue", "grey",
		"orange", "red", "SkyBlue", "green", "navy", "purple",
		"wheat", "PeachPuff", "beige", "maroon", "plum",
		"azure1", "magenta", "pink", "tan", "DarkGreen",
		"aquamarine1", "cyan", "peru"
	));
	$plot->SetXTimeFormat("%Y-%m-%d, %H");
	$plot->SetXLabelType('time');
	$plot->SetLegend($labels);
	$plot->SetPlotType($type);
	$plot->SetDataType('text-data');
	$plot->SetDataValues($data);
	$plot->SetTitle($title);
	$plot->SetXTickLabelPos('none');
	$plot->SetXTickPos('none');
	$plot->SetXLabelAngle(90);
	$plot->SetYDataLabelPos('plotin');

	echo("Plotting to " . realpath($file) . "\n");
	$plot->SetIsInline(true);
	$plot->SetOutputfile($file);
	$plot->DrawGraph();  
}

function incrementTime($timestamp, $increment) {
	$nextTime = getDate(wfTimestamp(TS_UNIX, $timestamp));
	switch($increment) {
		case "hour":
			$nextTime["hours"] += 1;
			break;
		case "day":
			$nextTime["mday"] += 1;
			break;
		case "week":
			$nextTime["mday"] += 7;
			break;
		case "month":
			$nextTime["mon"] += 1;
			break;
		case "year":
			$nextTime["year"] += 1;
			break;			
	}
	$nextTime = mktime(
		$nextTime["hours"], $nextTime["minutes"], $nextTime["seconds"],
		$nextTime["mon"], $nextTime["mday"], $nextTime["year"]
	);
	$nextTime = wfTimestamp(TS_MW, $nextTime);
	return $nextTime;
}

/**
 * Finds the pathway for a given title, including all redirects
 */
function pathwayFromPage($title) {
	if(!$title) {
		return null;
	}
	# Check if the page is a redirect
	if($title->isRedirect()) {
		#If so, return the redirect
		$rev = Revision::newFromID($title->getLatestRevID());
		$text = $rev->getText();
		return pathwayFromPage(Title::newFromRedirect($text));
	}
		
	# Check if the page is a pathway
	try {
		$pathway = Pathway::newFromTitle($title);
		return $pathway;
	} catch(Exception $e) {
		return null;
	}
}

/**
 * Get first revision for a title
 */
function getFirstRevision($title) {
	$revs = Revision::fetchAllRevisions($title);
	$revs->seek($revs->numRows() - 1);
	$row = $revs->fetchRow();
	return Revision::newFromId($row['rev_id']);
}

function array_subtract($a1, $a2) {
	$aRes = $a1;
	foreach (array_slice(func_get_args(), 1) as $aRay) {
		foreach (array_intersect_key($aRay, $aRes) as $key => $val) $aRes[$key] -= $val;
		foreach (array_diff_key($aRay, $aRes) as $key => $val) $aRes[$key] = -$val; 
	}
	return $aRes;
}
?>
