/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysds.test.functions.mlcontext;

import static org.apache.sysds.api.mlcontext.ScriptFactory.dmlFromFile;

import org.apache.log4j.Logger;
import org.apache.sysds.api.mlcontext.MLResults;
import org.apache.sysds.api.mlcontext.Script;
import org.apache.sysds.runtime.matrix.data.MatrixBlock;
import org.apache.sysds.runtime.matrix.data.MatrixValue;
import org.apache.sysds.test.TestUtils;
import org.junit.Test;

import java.util.HashMap;

public class MLContextMLogregTest extends MLContextTestBase {
	protected static Logger log = Logger.getLogger(MLContextMLogregTest.class);

	protected final static String TEST_SCRIPT_MLogreg = "scripts/algorithms/MultiLogReg.dml";
	private final static double sparsity1 = 0.7; // dense
	private final static double sparsity2 = 0.1; // sparse

	private final static double eps = 1e-5;

	private final static int rows = 2468;
	private final static int cols = 227;
	private final static double alpha = 0.85;

	private final static double epsilon = 1e-9;
	private final static double maxiter = 10;

	@Test
	public void testMLogregSparse() {
		runMLogregTestMLC(true);
	}

	@Test
	public void testMLogregDense() {
		runMLogregTestMLC(false);
	}


	private void runMLogregTestMLC(boolean sparse) {


		//generate actual datasets
		double[][] X = getRandomMatrix(rows, cols, 0, 1, sparse?sparsity2:sparsity1, 2384);
		double[][] Y = TestUtils.round(getRandomMatrix(rows, 1, 0.51, 5+0.49, 1.0, 9283));

		baseDirectory = "target/testTemp/functions/mlcontext/";

		fullRScriptName = "src/test/scripts/functions/codegenalg/Algorithm_MLogreg.R";

		writeInputMatrixWithMTD("X", X, true);
		writeInputMatrixWithMTD("Y",Y, true);

		rCmd = getRCmd(inputDir(), String.valueOf(2),String.valueOf(epsilon),String.valueOf(maxiter), expectedDir());
		runRScript(true);

		MatrixBlock outmat = new MatrixBlock();

		Script mlr = dmlFromFile(TEST_SCRIPT_MLogreg);
		mlr.in("X", X).in("Y", Y).in("$icpt", 2).in("$tol", epsilon).in("$moi", maxiter).in("$reg", 0.001).out("B_out");
		outmat = ml.execute(mlr).getMatrix("B_out").toMatrixBlock();


		//compare matrices
		HashMap<MatrixValue.CellIndex, Double> rfile = readRMatrixFromFS("w");
		TestUtils.compareMatrices(rfile, outmat, eps);
	}
}
