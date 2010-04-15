/**
 * Parses TF -> target gene relations from
 * the Pazar XML files and export them to a tabular text file
 */
class PazarParser {
	static findParent(elm, parentNames) {
		def p = elm.parent()	
		if(p == null || parentNames.contains(p.name())) p
		else findParent(p, parentNames)
	}
	
	static parsePazarXml(file) {
		println "Start xmlslurper parsing..."
		def xml = new XmlSlurper(false, false).parse(file)
		println "...end xmlslurper parsing"
	
		//Only index a subset of elements that we actually need
		def elementsToHash = [
			"gene_source", "marker", "funct_tf", 
			"reg_seq", "tf", "interaction"] as Set
	
		def byPazarId = [:]
		def analyses = [:]

		println "Building element hash..."
		//Find the necessary elements
		xml.depthFirst().each {
			def pid = it.'@pazar_id'.text()
			if(pid && elementsToHash.contains(it.name())) {
				byPazarId[pid] = it
			}
			if("analysis".equals(it.name())) analyses[pid] = it
		}

		println "Iterating analyses"
		def results = []
		//Iterate analyses to find binding TFs and sites
		analyses.each {
			def analysisId = it.value.'@pazar_id'.text()
			
			it.value.input_output. each {
				def input = it.input.'@inputs'.text().split(' ')
				def output = it.output.'@outputs'.text().split(' ')
	
				def functTf = null
				def targets = []
				input.each {
					def elm = byPazarId[it]
				
					switch(elm?.name()) {
						case "funct_tf":
							functTf = elm
							break;	
						case "reg_seq":
							targets << elm
							break;
						default:
							println "Found unsupported element in inputs: ${it}"
					}
				}
	
				//Make sure the inputs map to TF	
				//Input should reference a func_tf element
				if(!functTf || targets.size() == 0) return
			
				//Find the ensembl ids of the target gene(s)
				def targetGenes = [] as Set
				targets.each {
					def geneSource = findParent(it, ["gene_source", "marker"])
					assert ["gene_source", "marker"].contains(geneSource.name()) , "Expecting gene_source|marker, got: ${geneSource.name()}"
					def accn = geneSource.'@db_accn'.text()
					def descr = geneSource.'@description'?.text() ?: ''
					targetGenes.add([ 'accession': accn, 'description':descr ])
				}

				//Find the TF ensembl ids
				def tfGenes = [] as Set
				functTf.tf_unit.'@tf_id'.each {
					def tf = byPazarId[it.text()]
					def geneSource = findParent(tf, ["gene_source", "marker"])
					assert ["gene_source", "marker"].contains(geneSource.name()) , "Expecting gene_source|marker, got: ${geneSource.name()}"
					def accn = geneSource.'@db_accn'.text()
					def descr = geneSource.'@description'?.text() ?: ''
					tfGenes.add([ 'accession': accn, 'description':descr ])
				}
	
				def experiments = output.collect { pid ->
					byPazarId[pid]
				}
				if(!experiments) experiments = []
	
				results << [analysis: analysisId, functTf: functTf.'@pazar_id'.text(), functTfName: functTf.'@funct_tf_name'.text(),
					tfs: tfGenes, targets: targetGenes, experiments: experiments]
			}
		}
		return results
	}
	
	static asText(results) {
		def cols = [
			"FunctTF", "TF gene", "Target gene", "TF gene name", "Target gene name", "Analysis", "Experiment", "Value"
		]
		def txt = cols.join("\t") + "\n"
		
		results.each { r ->
			r['experiments'].each { exp ->
				r['tfs'].each { tf ->
					r['targets'].each { tgt ->
						def value = exp?.'@qualitative'?.text() ? exp.'@qualitative'?.text() : exp?.'@quantitative'?.text()
						def scale = exp?.'@scale'?.text() ? exp.'@scale'.text() : ''
						def vals = [
							r['functTfName'],  tf.accession, tgt.accession, tf.description, tgt.description, r['analysis'], exp?.'@pazar_id'?.text(), "${value} ${scale}"
						]
						txt += vals.join("\t") + "\n"
					}
				}
			}
		}
		txt
	}
}
