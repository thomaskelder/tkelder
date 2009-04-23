# Function that combines different matrices based on row names
combineByRow = function(listOfArrays) {
	# Create a union of the rownames for all matrices
	rowNames = c()
	colNames = c()
	for(m in listOfArrays) {
		rowNames = union(rowNames, rownames(m))
		colNames = c(colNames, colnames(m))
	}
	message("Total rows: ", length(rowNames))
	message("Total cols: ", length(colNames))
	
	combined = array(NA, 
		dim = c(length(rowNames), length(colNames)), 
		dimnames = list(rowNames, colNames)
	)
	for(m in listOfArrays) {
		for(col in colnames(m)) {
			message("Processing ", col)
			combined[rownames(m), col] = m[,col]
		}
	}
	combined
}
