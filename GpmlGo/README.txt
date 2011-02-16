For a given set of GPML pathways and a Gene Ontology, this script will calculate the percentage of genes that maps between each pathway and GO term.

=== Required data ===
You will need several data files in order to run the script:
1. Biomart mappings
This file will provide the mappings from Ensembl genes to GO terms. You can query this information from Biomart and save as TSV file. As attributes, select "Ensembl gene ID", GO ID and GO Evidence code.

Here is an example of the settings for the human mappings for Biological Process:
http://www.ensembl.org/biomart/martview/04ea6e1437256744a382c96a5b21195f/04ea6e1437256744a382c96a5b21195f?VIRTUALSCHEMANAME=default&ATTRIBUTES=hsapiens_gene_ensembl.default.feature_page.ensembl_gene_id|hsapiens_gene_ensembl.default.feature_page.go_biological_process_id|hsapiens_gene_ensembl.default.feature_page.go_biological_process_linkage_type&FILTERS=&VISIBLEPANEL=resultspan

2. Ontology
This file defines the GO ontology and can be downloaded here:
http://www.geneontology.org/ontology/obo_format_1_2/gene_ontology.1_2.obo

3. GPML pathways
Can be downloaded from WikiPathways (http://www.wikipathways.org).

=== Build ===
To build the jar file, run:

ant dist

=== Run ===
To run the script after building, run:

cd dist/
java -jar gpml-go.jar

This will show the available command line options. If you want to perform the mapping, run:

java -jar gpml-go.jar -action map -go /path/to/gene_ontology.1_2.obo -pathways /path/to/gpmlfiles -annot /path/to/biomartfile -org "Mus musculus" -out /path/to/outputfile.txt -idm idmapper-pgdb:/path/to/synonymdb.pgdb
