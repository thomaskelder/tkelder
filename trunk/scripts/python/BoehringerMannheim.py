## Count the number of enzymes on the boehringer mannheim metabolic pathways wall chart
## which is hosted at expasy.org

import urllib, re

ecNumbers = set([])
ecNames = set([])

baseUrl = 'http://expasy.org'
largeMapUrl = baseUrl + '/cgi-bin/show_thumbnails.pl'

sock = urllib.urlopen(largeMapUrl)
largeMapHtml = sock.read()
sock.close()

p = re.compile('/cgi-bin/show_image\?[A-Z][1-9]+')

mapUrls = p.findall(largeMapHtml)
for u in mapUrls:
	print(baseUrl + u)
	sock = urllib.urlopen(baseUrl + u)
	html = sock.read()
	sock.close()
	
	p = re.compile('/enzyme/([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)" alt="(.+)"')
	for m in p.finditer(html):
		ecNumbers.add(m.group(1))
		ecNames.add(m.group(2))

print("Number of unique ec numbers: " + str(len(ecNumbers)))
print("Number of unique enzyme names: " + str(len(ecNames)))
