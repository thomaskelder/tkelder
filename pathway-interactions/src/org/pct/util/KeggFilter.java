package org.pct.util;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter KEGG pathway categories (based on file name pattern used by KEGG - GPML converter).
 * @author thomas
 *
 */
public class KeggFilter {
	private static Set<File> findPathways(Collection<File> pathwayFiles, String[] toFind) {
		Set<File> disease = new HashSet<File>();
		for(File f : pathwayFiles) {
			for(String d : toFind) {
				if(f.getName().contains(d)) disease.add(f);
			}
		}
		return disease;
	}
	
	public static Set<File> findDiseasePathways(Collection<File> pathwayFiles) {
		return findPathways(pathwayFiles, diseasePathways);
	}
	
	public static Set<File> findOverviewPathways(Collection<File> pathwayFiles) {
		return findPathways(pathwayFiles, overviewPathways);
	}
	
	private static final String[] overviewPathways = new String[] {
		"Overview of biosynthetic pathways",
		"Biosynthesis of plant secondary metabolites",
		"Biosynthesis of phenylpropanoids",
		"Biosynthesis of terpenoids and steroids",
		"Biosynthesis of alkaloids derived from",
		"Biosynthesis of plant hormones",
		"Metabolic pathways",
		"Biosynthesis of secondary metabolites",
	};
	
	private static final String[] diseasePathways = new String[] {
			"Pathways in cancer",
			"Colorectal cancer",
			"Pancreatic cancer",
			"Glioma",
			"Thyroid cancer",
			"Acute myeloid leukemia",
			"Chronic myeloid leukemia",
			"Basal cell carcinoma",
			"Melanoma",
			"Renal cell carcinoma",
			"Bladder cancer",
			"Prostate cancer",
			"Endometrial cancer",
			"Small cell lung cancer",
			"Non-small cell lung cancer",
			"ICD-10 disease classification",
			"Asthma",
			"Systemic lupus erythematosus",
			"Autoimmune thyroid disease",
			"Allograft rejection",
			"Graft-versus-host disease",
			"Primary immunodeficiency",	
			"Alzheimer's disease",
			"Parkinson's disease",
			"Amyotrophic lateral sclerosis (ALS)",
			"Huntington's disease",
			"Prion diseases",	
			"Hypertrophic cardiomyopathy (HCM)",
			"Arrhythmogenic right ventricular cardiomyopathy",
			"Dilated cardiomyopathy (DCM)",
			"Viral myocarditis",
			"Type I diabetes mellitus",
			"Type II diabetes mellitus",
			"Maturity onset diabetes of the young",	
			"Vibrio cholerae infection",
			"Vibrio cholerae pathogenic cycle",
			"Epithelial cell signaling in Helicobacter",
			"Pathogenic Escherichia coli infection",
			"Shigellosis",
			"Bacterial invasion of epithelial cells",
			"Malaria",
			"Leishmaniasis",
			"Chagas disease",
	};
}
