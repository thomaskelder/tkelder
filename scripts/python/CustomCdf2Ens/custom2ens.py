#Converts a custom cdf annotated expresssion file to ensembl genes

#Input expression file should be a tab delimited file where one column
#contains the custom probeset ids and the other columns contain the data.

#For each row (probeset), the ensembl gene will be looked up and the row
#will be copied to a new file, with an additional column, containing the
#Ensembl gene.

from optparse import OptionParser

parser = OptionParser()
parser.add_option("-c", "--cdf2ens", dest="mapFile",
		help="File containing cdf to ensembl mappings (see http://brainarray.mbni.med.umich.edu/Brainarray/Database/CustomCDF/11.0.1/ensg.asp)")
parser.add_option("-e", "--expr",
		dest="exprFile", help="File containing expression values")
parser.add_option("-i", "--idcol",
		dest="idCol", default="0", help="The column in EXPRFILE which contains the probeset ids")
parser.add_option("-o", "--out",
		dest="outFile", help="File to write the mapped data to")
		
(options, args) = parser.parse_args()

print 'Using mappings from ', options.mapFile
print 'Using expression data from ', options.exprFile
print 'Output to ', options.outFile

idCol = int(options.idCol)
mapFile = open(options.mapFile, 'r')
exprFile = open(options.exprFile, 'r')
outFile = open(options.outFile, 'w')

#Read the mappings
i = 0
mappings = {}
for line in mapFile:
	if i == 0:
		i = i + 1
		continue; #Skip header
	
	cols = line.strip().split("\t")
	#Remove '_at' from ensembl id
	eid = cols[0].rstrip('_at')
	pid = cols[6]
	eset = set()
	if not pid in mappings:
		mappings[pid] = eset
	else:
		eset = mappings[pid]
	eset.add(eid)

mapFile.close()

i = 0
for line in exprFile:
	if i == 0:
		i = i + 1
		outFile.write(line)
		continue; #Just copy header
	
	cols = line.strip().split("\t")
	pid = cols[idCol]
	if pid in mappings:
		for eid in mappings[pid]:
			cols.insert(0, eid)
			outFile.write('\t'.join(cols))
			outFile.write("\n")
	else:
		cols.insert(0, '')
		outFile.write('\t'.join(cols))
		outFile.write("\n")
		
outFile.close()
exprFile.close()
