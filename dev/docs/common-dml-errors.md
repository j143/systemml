#### Errors commonly encountered while working with DML

##### 1. Read input file does not exist on FS (local mode):

`org.apache.sysds.parser.DataExpression.validateExpression(DataExpression.java:867)`

```
// Check for file existence (before metadata parsing for meaningful error messages)
if( shouldReadMTD //skip check for jmlc/mlcontext
	&& !HDFSTool.existsFileOnHDFS(inputFileName)) 
	{
		String fsext = InfrastructureAnalyzer.isLocalMode() ? "FS (local mode)" : "HDFS";
		raiseValidateError("Read input file does not exist on "+fsext+": " + 
			inputFileName, conditional);
	}
```

