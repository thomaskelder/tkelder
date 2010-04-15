/**
 * Script to parse TF -> target gene relations from
 * the Pazar XML files and export them to a tabular text file
 *
 * Takes one argument, which points to the directory containing the Pazar
 * XML files, or a single Pazar XML file.
 */

//Read all files in the pazar directory
def inFile = new File(args[0])
def files = [inFile]
if(inFile.isDirectory()) {
	files = inFile.listFiles(
		 [accept:{file-> file ==~ /.*?\.xml/ }] as FileFilter
	)	
}

def summary = "------------- SUMMARY --------------\n"
files.each {
	println "Reading file ${it}"
	def results = PazarParser.parsePazarXml(it)
	
	def txtFile = new File(it.getAbsolutePath() + ".txt")
	txtFile.write(PazarParser.asText(results))
	
	def tfs = [] as Set
	def tgs = [] as Set
	def ftfs = [] as Set
	results.each {
		tfs.addAll(it['tfs'])
		tgs.addAll(it['targets'])
		ftfs.add(it['functTf'])
	}
	
	summary += """
--------------------
${it}
Functional TFs: ${ftfs.size()}
Unique TF genes: ${tfs.size()}
Unique target genes: ${tgs.size()}
analyses: ${results.size()}
"""
}

println summary
  
