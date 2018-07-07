import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/*
 * New deterministic equivalent solver
 * for @birdybird's dissertation
 */



public class DecisionMaker {

	/* decision criteria weighting */
	static double[] u_weight = DMInputs.getUWeighting();
	static double[] g_weight = DMInputs.getGWeighting();
	
	/* U's strategies */
	static int[] u_one = {0,1,2,3};
	static int[] u_two = {0,1,2,3};
	static int[][][] u_three = {
		{{0},{0,1},{0,2},{0,1,2,3}},
		{{0,1},{0,1},{0,1,2,3},{0,1,2,3}},
		{{0,2},{0,1,2,3},{0,2},{0,1,2,3}},
		{{0,1,2,3},{0,1,2,3},{0,1,2,3},{0,1,2,3}}
	};
	
	/* G's strategies */
	
	static int[] rep_cost = new int[DMInputs.getReprocessingCost().length];
	static int[] disp_cost = new int[DMInputs.getDisposalCostOutcomes().length];
	static int[] g_one = new int[DMInputs.getDisposalCostProbabilities().length];
	static int[] g_two = new int[DMInputs.getCapitalSubsidy().length];
	
	/* Nature plays */
	static int[][] CapitalCostCombos = {
		{0,0,0},
		{0,0,1},
		{0,0,2},
		{0,1,0},
		{0,1,1},
		{0,1,2},
		{0,2,0},
		{0,2,1},
		{0,2,2}
	};
	/* probabilities */
	static double[][][] randd_prob = DMInputs.getRandD();
	static int[] htgr_cost = new int[DMInputs.getHTGRCapitalCost().length];
	static double[] htgr_prob = DMInputs.getHTGRCapCostProb();
	static int[] sfr_cost = new int[DMInputs.getSFRCapitalCost().length];
	static double[] sfr_prob = DMInputs.getSFRCapCostProb();
	
	static double[][][][][][][][][] Dat;
	
	static int[][][][][][][] u_thr_pi;
	static int[][][][][][] u_two_pi;
	static int[][][][][] g_two_pi;
	static int[][][][] u_one_pi;
	static int[][][] g_one_pi;
	
			
	
	
//	static double[][][][][][][][][] leaf_values;
//	
//	/* For perfect information strategies */
//	/* u_three_perfectinfo[u_one][u_two][g_one][g_two][dispcost_outcome][htgr_capcost][sfr_capcost] */
//	static int[][][][][][][] u_three_perfectinfo;
//	static int[][][][][][] u_two_perfectinfo;
//	static int[][][][][] g_two_perfectinfo;
//	static int[][][][] u_one_perfectinfo;
//	static int[][][] g_one_perfectinfo;
//	
//	/* For hedging strategies */
//	/* iff htgr or sfr outcome == 3, this means that the capital cost hasn't been realized */
//	static int[][][][][][] u_two_hedge;
//	static int[][][][][] g_two_hedge;
//	static int[][] u_one_hedge;
//	static int g_one_hedge;
	
	
	
//	static int[][] u_one_gainedinfo = {
//		{0,0,0}, {0,1,0}, {0,0,1}, {0,1,1}
//	};
//	static int[][][] u_two_gainedinfo = {
//		// u_stage_one = 0 {u_stage_two = 0, 1, 2, 3}
//		{{0,0,0},{0,1,0},{0,0,1},{0,1,1}},
//		// u_stage_one = 1 {u_stage_two = 0, 1, 2, 3}
//		{{0,0,0},{0,0,0},{0,0,1},{0,0,1}},
//		// u_stage_one = 2 {u_stage_two = 0, 1, 2, 3}
//		{{0,0,0},{0,1,0},{0,0,0},{0,1,0}},
//		// u_stage_one = 3 {u_stage_two = 0, 1, 2, 3}
//		{{0,0,0},{0,0,0},{0,0,0},{0,0,0}}
//	};
//	



//	static double[][][][][][][][][] leaf_values;
//	
//	/* For perfect information strategies */
//	/* u_three_perfectinfo[u_one][u_two][g_one][g_two][dispcost_outcome][htgr_capcost][sfr_capcost] */
//	static int[][][][][][][] u_three_perfectinfo;
//	static int[][][][][][] u_two_perfectinfo;
//	static int[][][][][] g_two_perfectinfo;
//	static int[][][][] u_one_perfectinfo;
//	static int[][][] g_one_perfectinfo;
//	
//	/* For hedging strategies */
//	/* iff htgr or sfr outcome == 3, this means that the capital cost hasn't been realized */
//	static int[][][][][][] u_two_hedge;
//	static int[][][][][] g_two_hedge;
//	static int[][] u_one_hedge;
//	static int g_one_hedge;
	
	
	
	public DecisionMaker() {
		
	}
	
	public static void main(String args[]) {
		NewDecisionMaker decide = new NewDecisionMaker();
		//decide.dimensionArrays();
		/* get the data, then normalize it */
		decide.loadData();
		decide.normalizeData();
		/* get the perfect info strategies based on that info */
		decide.getPerfectInformationStrategies();
		/* get the hedging strategies */
		decide.getHedgingStrategies();
		/* print the hedging strategy results */
		decide.printHedgingStrategies();
	}	
	
	public void loadData() {

		String inputFile = "DecisionMakingResults.txt";
		String file_path = System.getProperty("user.dir") + File.separatorChar + inputFile;
		File data = new File(file_path);
		BufferedReader buf;
		String current_line = " anything ";
		StringTokenizer st;
		int i,j,k;
		double[] dummy_double = {0.0,0};
		
		int[] ints = {0,0,0,0,0,0,0};
		/* 
		 * 0 = u_one; 1 = u_two; 2 = u_thr; 3 = rep_cost; 4 = g_two; 5 = disp_cost; 6 = htgr_cost; 7 = sfr_cost
		 */
		
		try {
		
			int numberOfLines = readLines(inputFile);
			buf = new BufferedReader(new FileReader(data));
		
			current_line = buf.readLine();
			
			for (j=0; j<numberOfLines-1; j++) {

				current_line = buf.readLine();

				st = new StringTokenizer(current_line);
				
				for (i=0; i<ints.length; i++) ints[i] = Integer.valueOf( st.nextToken() ).intValue();

				for (i=0; i<dummy_double.length; i++) {
					Dat[ints[0]][ints[1]][ints[2]][ints[3]][ints[4]][ints[5]][ints[6]][ints[7]][i] = Double.valueOf( st.nextToken() ).doubleValue();
				}

			}

		}
		catch(IOException IOE) {
			System.err.println("Decision Making Error 02: Error reading results from VEGAS "+ inputFile);
			System.err.println(IOE.toString());
		}

	}
	
	public int readLines(String inputFile) throws IOException {
		
		String file_path = System.getProperty("user.dir") + File.separatorChar + inputFile;
		File data = new File(file_path);
		BufferedReader buf;
		int numberOfLines = 0;

		try {
			buf = new BufferedReader(new FileReader(data));
			while (( buf.readLine()) != null) numberOfLines++;
			buf.close();
		} catch(IOException IOE) {
			System.err.println("Decision Making Error 01: Error counting lines in "+ inputFile);
			System.err.println(IOE.toString());
		}
		
		return numberOfLines;
		
	}	
	
	public void normalizeData() {
	
		int g_o, g_tw;
		int u_o, u_tw, u_th;
		int rep, disp, htgr, sfr;
		
		double[] min_val = {0,0,0};
		double[] max_val = {0,0,0};
		int val, count=0;
		
		for (u_o=0; u_o<u_one.length; u_o++) {
			for (u_tw=0; u_tw<u_two.length; u_tw++) {
				for (u_th=0; u_th<u_three.length; u_th++) {
					for (g_tw=0; g_tw<g_two.length; g_tw++) {
						for (rep=0; rep<rep_cost.length; rep++) {
							for (disp=0; disp<disp_cost.length; disp++) {
								for (htgr=0; htgr<htgr_cost.length; htgr++) {
									for (sfr=0; sfr<sfr_cost.length; sfr++) {
										
									}
								}
							}
						}

					}
				}
			}
		}

	}
	
//	public void normalizeData() {
//		int g_stage_one, g_stage_two;
//		int u_stage_one, u_stage_two, u_stage_three;
//		int outcome_one, htgr_outcome, sfr_outcome;
//		
//		double[] min_value={0,0,0,0};
//		double[] max_value={0,0,0,0};
//		int value, count=0;
//		
//		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
//			for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
//				for (u_stage_three=0; u_stage_three<u_three_strategy[u_stage_one][u_stage_two].length; u_stage_three++) {
//					for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
//						for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
//							for (outcome_one=0; outcome_one<n_dispcost_outcome; outcome_one++) {
//								for (htgr_outcome=0; htgr_outcome<n_htgr_capcost; htgr_outcome++) {
//									for (sfr_outcome=0; sfr_outcome<n_sfr_capcost; sfr_outcome++) {
//										for (value=0; value<min_value.length; value++) {
//											if (count==0) min_value[value] = leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value];
//											if (count==0) max_value[value] = leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value];
//											if (leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value] <= min_value[value]) {
//												min_value[value] = leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value];
//											}
//											if (leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value] >= max_value[value]) {
//												max_value[value] = leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value];
//											}
//										}
//										count++;
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		
//		double[] normalized_value = {0,0,0,0};
//		
//		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
//			for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
//				for (u_stage_three=0; u_stage_three<u_three_strategy[u_stage_one][u_stage_two].length; u_stage_three++) {
//					for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
//						for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
//							for (outcome_one=0; outcome_one<n_dispcost_outcome; outcome_one++) {
//								for (htgr_outcome=0; htgr_outcome<n_htgr_capcost; htgr_outcome++) {
//									for (sfr_outcome=0; sfr_outcome<n_sfr_capcost; sfr_outcome++) {
//										for (value=0; value<min_value.length; value++) {
//											if (value==2) { // proliferation metric .. right now is dose rate .. want high dose rate!
//												if (min_value[value] == max_value[value]) {
//													
//												} else {
//													normalized_value[value] = (leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value]-min_value[value])/(max_value[value]-min_value[value]);
//													leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value] = normalized_value[value];
//												}
//											} else {
//												if (min_value[value] == max_value[value]) {	// all other metrics .. want min values
//													
//												} else {
//													normalized_value[value] = (leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value]-min_value[value])/(max_value[value]-min_value[value]);
//													leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value] = ((double) 1.-normalized_value[value]);
//												}
//											}
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//	}
	
	
}
