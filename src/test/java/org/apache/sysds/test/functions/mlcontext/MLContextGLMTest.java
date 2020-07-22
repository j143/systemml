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

import org.apache.log4j.Logger;
import org.apache.sysds.api.mlcontext.Script;
import org.apache.sysds.runtime.matrix.data.MatrixBlock;
import org.apache.sysds.runtime.matrix.data.MatrixValue;
import org.apache.sysds.test.TestUtils;
import org.junit.Test;

import java.util.HashMap;

import static org.apache.sysds.api.mlcontext.ScriptFactory.dmlFromFile;

public class MLContextGLMTest extends MLContextTestBase {
	protected static Logger log = Logger.getLogger(MLContextGLMTest.class);

	protected final static String TEST_SCRIPT = "scripts/algorithms/GLM.dml";
	private final static double sparsity1 = 0.7; // dense
	private final static double sparsity2 = 0.1; // sparse

	public enum GLMType {
		POISSON_LOG,
	}

	private final static double eps = 1e-3;

	private final static int rows = 2468;
	private final static int cols = 507;

	@Test
	public void testGLMSparse() {
		runGLMTestMLC(GLMType.POISSON_LOG, true);
	}

	private void runGLMTestMLC(GLMType type, boolean sparse) {

		double[][] X = getRandomMatrix(rows, cols, 0, 1, sparse ? sparsity2 : sparsity1, 7);
		double[][] Y = getRandomMatrix(rows, 1, 0, 10, 1.0, 3);

		// Hack Alert
		// overwrite baseDirectory to the place where test data is stored.
		baseDirectory = "target/testTemp/functions/mlcontext/";

		fullRScriptName = "src/test/scripts/functions/codegenalg/Algorithm_GLM.R";

		writeInputMatrixWithMTD("X", X, true);
		writeInputMatrixWithMTD("y", Y, true);

		rCmd = getRCmd(inputDir(), "0", "0.000001", "0", "0.001", expectedDir());
		runRScript(true);

		MatrixBlock outmat = new MatrixBlock();

		switch (type) {
			case POISSON_LOG:
			Script lrcg = dmlFromFile(TEST_SCRIPT);
			lrcg.in("X", X).in("y", Y).in("$icpt", "0").in("$tol", "0.000001").in("$maxi", "0").in("$reg", "0.000001")
					.out("beta_out");
			outmat = ml.execute(lrcg).getMatrix("beta_out").toMatrixBlock();

			break;

		}

		//compare matrices
//		HashMap<MatrixValue.CellIndex, Double> rfile = readRMatrixFromFS("w");
//		TestUtils.compareMatrices(rfile, outmat, eps);
	}
}
