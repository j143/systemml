package com.ibm.bi.dml.hops;

import com.ibm.bi.dml.hops.OptimizerUtils.OptimizationType;
import com.ibm.bi.dml.lops.Aggregate;
import com.ibm.bi.dml.lops.Group;
import com.ibm.bi.dml.lops.Lops;
import com.ibm.bi.dml.lops.PartialAggregate;
import com.ibm.bi.dml.lops.Transform;
import com.ibm.bi.dml.lops.LopProperties.ExecType;
import com.ibm.bi.dml.parser.Expression.DataType;
import com.ibm.bi.dml.parser.Expression.ValueType;
import com.ibm.bi.dml.sql.sqllops.SQLLopProperties;
import com.ibm.bi.dml.sql.sqllops.SQLLops;
import com.ibm.bi.dml.sql.sqllops.SQLLopProperties.AGGREGATIONTYPE;
import com.ibm.bi.dml.sql.sqllops.SQLLopProperties.JOINTYPE;
import com.ibm.bi.dml.sql.sqllops.SQLLops.GENERATES;
import com.ibm.bi.dml.utils.HopsException;
import com.ibm.bi.dml.utils.LopsException;


/* Reorg (cell) operation: aij
 * 		Properties: 
 * 			Symbol: ', diag
 * 			1 Operand
 * 	
 * 		Semantic: change indices (in mapper or reducer)
 */

public class ReorgOp extends Hops {

	ReorgOp op;

	public ReorgOp(String l, DataType dt, ValueType vt, ReorgOp o, Hops inp) {
		super(Kind.ReorgOp, l, dt, vt);
		op = o;
		getInput().add(0, inp);
		inp.getParent().add(this);
	}

	public ReorgOp(String l, DataType dt, ValueType vt, ReorgOp o, Hops inp1, Hops inp2) {
		super(Kind.ReorgOp, l, dt, vt);
		
		op = o;
		getInput().add(0, inp1);
		getInput().add(1, inp2);
		inp1.getParent().add(this);
		inp2.getParent().add(this);
	}
	
	public void printMe() throws HopsException {
		if (LOG.isDebugEnabled()){
			if (get_visited() != VISIT_STATUS.DONE) {
				super.printMe();
				LOG.debug("  Operation: " + op);
				for (Hops h : getInput()) {
					h.printMe();
				}
			}
			set_visited(VISIT_STATUS.DONE);
		}
	}

	@Override
	public String getOpString() {
		String s = new String("");
		s += "r(" + HopsTransf2String.get(op) + ")";
		return s;
	}

	@Override
	public Lops constructLops()
			throws HopsException, LopsException {

		if (get_lops() == null) {
			if (op == ReorgOp.DIAG_M2V) {
				// TODO: this code must be revisited once the support 
				// for matrix indexing is implemented. 

				try {
					// Handle M2V case separately
					// partialAgg (diagM2V) - group - agg (+)

					PartialAggregate transform1 = new PartialAggregate(
							getInput().get(0).constructLops(),
							Aggregate.OperationTypes.DiagM2V,
							HopsDirection2Lops.get(Direction.Col),
							get_dataType(), get_valueType());

					
					transform1.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
					
					// copy the dimensions from the HOP (which would be a column
					// vector, in this case)
					transform1.getOutputParameters().setDimensions(get_dim1(),
							get_dim2(), get_rows_in_block(),
							get_cols_in_block(), getNnz());

					Group group1 = new Group(
							transform1, Group.OperationTypes.Sort,
							get_dataType(), get_valueType());
					group1.getOutputParameters().setDimensions(get_dim1(),
							get_dim2(), get_rows_in_block(),
							get_cols_in_block(), getNnz());
					
					group1.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());

					Aggregate agg1 = new Aggregate(
							group1, HopsAgg2Lops.get(AggOp.SUM),
							get_dataType(), get_valueType(), ExecType.MR);
					agg1.getOutputParameters().setDimensions(get_dim1(),
							get_dim2(), get_rows_in_block(),
							get_cols_in_block(), getNnz());

					agg1.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
					
					// kahanSum setup is not used for Diag operations. They are
					// treated as special case in the run time
					agg1.setupCorrectionLocation(transform1
							.getCorrectionLocaion());

					set_lops(agg1);
				} catch (LopsException e) {
					throw new HopsException(this.printErrorLocation() + "In ReorgOp Hop, error constructing Lops " , e);
				}
			}/* else if(op == ReorgOp.APPEND){
				ExecType et = optFindExecType();
				
				UnaryCP offset = new UnaryCP(getInput().get(0).constructLops(),
						 UnaryCP.OperationTypes.NCOL,
						 DataType.SCALAR,
						 ValueType.DOUBLE);
                offset.getOutputParameters().setDimensions(0, 0, 0, 0, -1);
                offset.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
				Append append = new Append(getInput().get(0).constructLops(), 
										   getInput().get(1).constructLops(), 
										   offset,
										   get_dataType(),
										   get_valueType(), et);
				append.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
				
				append.getOutputParameters().setDimensions(get_dim1(), 
														   get_dim2(), 
														   get_rows_in_block(), 
														   get_cols_in_block(), 
														   getNnz());
				
				ReBlock reblock = null;
				try {
					reblock = new ReBlock(
							append, get_rows_in_block(),
							get_cols_in_block(), get_dataType(), get_valueType());
				} catch (Exception e) {
					throw new HopsException(this.printErrorLocation() + "error in constructing Lops for ReorgOp " , e);
				}
				reblock.getOutputParameters().setDimensions(get_dim1(), get_dim2(), 
						get_rows_in_block(), get_cols_in_block(), getNnz());
		
				reblock.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
				
				set_lops(reblock);
				
			}*/ else {
				ExecType et = optFindExecType();
				Transform transform1 = new Transform(
						getInput().get(0).constructLops(), HopsTransf2Lops
								.get(op), get_dataType(), get_valueType(), et);
				transform1.getOutputParameters().setDimensions(get_dim1(),
						get_dim2(), get_rows_in_block(), get_cols_in_block(), getNnz());
				
				transform1.setAllPositions(this.getBeginLine(), this.getBeginColumn(), this.getEndLine(), this.getEndColumn());
				
				set_lops(transform1);
			}
		}
		return get_lops();
	}

	@Override
	public SQLLops constructSQLLOPs() throws HopsException {
		if (this.get_sqllops() == null) {
			if (this.getInput().size() != 1)
				throw new HopsException(this.printErrorLocation() + "An unary hop must have only one input \n");

			// Check whether this is going to be an Insert or With
			GENERATES gen = determineGeneratesFlag();

			Hops input = this.getInput().get(0);

			SQLLops sqllop = new SQLLops(this.get_name(),
										gen,
										input.constructSQLLOPs(),
										this.get_valueType(),
										this.get_dataType());

			String sql = null;
			if (this.op == ReorgOp.TRANSPOSE) {
				sql = String.format(SQLLops.TRANSPOSEOP, input.get_sqllops().get_tableName());
			} else if (op == ReorgOp.DIAG_M2V) {
				sql = String.format(SQLLops.DIAG_M2VOP, input.get_sqllops().get_tableName());
			} else if (op == ReorgOp.DIAG_V2M) {
				sql = String.format(SQLLops.DIAG_V2M, input.get_sqllops().get_tableName());
			}
			
			sqllop.set_properties(getProperties(input));
			sqllop.set_sql(sql);

			this.set_sqllops(sqllop);
		}
		return this.get_sqllops();
	}
	private SQLLopProperties getProperties(Hops input)
	{
		SQLLopProperties prop = new SQLLopProperties();
		prop.setJoinType(JOINTYPE.NONE);
		prop.setAggType(AGGREGATIONTYPE.NONE);
		prop.setOpString(HopsTransf2String.get(op) + "(" + input.get_sqllops().get_tableName() + ")");
		return prop;
	}
	
	@Override
	public boolean allowsAllExecTypes()
	{
		return true;
	}
	
	// TRANSPOSE, DIAG_V2M, DIAG_M2V, APPEND
	@Override
	public double computeMemEstimate() {
		Hops input = getInput().get(0);
		
		switch(op) {
		case TRANSPOSE:
			// input is a [k1,k2] matrix and output is a [k2,k1] matrix
			// although nnz and dims do not change, we need to compute mem based on our sparse row representation
			if( dimsKnown() ){
				double spt = (input.getNnz()>0)? ((double)input.getNnz())/input.get_dim1()/input.get_dim2() : 1.0;
				_outputMemEstimate = OptimizerUtils.estimateSizeExactSparsity(input.get_dim2(), input.get_dim1(), spt);	
			}
			else
				_outputMemEstimate = OptimizerUtils.DEFAULT_SIZE;
			
			break;
			
		case DIAG_V2M:
			// input is a [1,k] or [k,1] matrix, and output is [kxk] matrix
			// In the worst case, #nnz in output = k => sparsity = 1/k
			if( dimsKnown() ){
				long k = (input.get_dim1() > 1 ? input.get_dim1() : input.get_dim2());   
				_outputMemEstimate = OptimizerUtils.estimateSizeExactSparsity(k, k, (double)1/k); 
			}
			else 
				_outputMemEstimate = OptimizerUtils.DEFAULT_SIZE;

			break;
			
		case DIAG_M2V:
			// input is [k,k] matrix and output is [k,1] matrix
			// #nnz in the output is likely to be k (a dense matrix)
			if( dimsKnown() )
				_outputMemEstimate = OptimizerUtils.estimateSizeExactSparsity(input.get_dim1(), 1, 1.0); 
			else
				_outputMemEstimate = OptimizerUtils.DEFAULT_SIZE;
			
			break;
		
	/*	case APPEND:
			// always get a worst-case estimate for append!
			_outputMemEstimate = OptimizerUtils.DEFAULT_SIZE;
			
			break;*/
			
		}
		
		_memEstimate = getInputOutputSize();
		
		return _memEstimate;
	}
	

	
	@Override
	protected ExecType optFindExecType() throws HopsException {
		
		checkAndSetForcedPlatform();
	
		if( _etypeForced != null ) 			
			_etype = _etypeForced;
		else 
		{
			//mark for recompile (forever)
			if( OptimizerUtils.ALLOW_DYN_RECOMPILATION && !dimsKnown() )
				setRequiresRecompile();
			
			if ( OptimizerUtils.getOptType() == OptimizationType.MEMORY_BASED ) {
				_etype = findExecTypeByMemEstimate();
			}
			// Choose CP, if the input dimensions are below threshold or if the input is a vector
			else if ( getInput().get(0).areDimsBelowThreshold() || getInput().get(0).isVector() )
				_etype = ExecType.CP;
			else 
				_etype = ExecType.MR;
		}
		return _etype;
	}
	
	@Override
	public void refreshSizeInformation()
	{
		Hops input1 = getInput().get(0);
		
		switch(op) 
		{
			case TRANSPOSE:
				set_dim1(input1.get_dim2());
				set_dim2(input1.get_dim1());
				setNnz(input1.getNnz());
				break;
				
			case DIAG_V2M:
				// input is a [1,k] or [k,1] matrix, and output is [kxk] matrix
				int maxDim = (int) Math.max(input1.get_dim1(), input1.get_dim2());
				set_dim1(maxDim);
				set_dim2(maxDim);
				break;
				
			case DIAG_M2V:
				// input is [k,k] matrix and output is [k,1] matrix
				set_dim1(input1.get_dim1());
				set_dim2(1);								
				break;
		/*	
			case APPEND:
				Hops input2 = getInput().get(1);
				set_dim1( input1.get_dim1() );
				set_dim2( input1.get_dim2() + input2.get_dim2() );
				setNnz( input1.getNnz() + input2.getNnz() );
				break;
		*/		
		}	
	}
}
