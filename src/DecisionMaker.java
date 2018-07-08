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
	
	static int[][] u_one_gainedinfo = {
		{0,0,0}, {0,1,0}, {0,0,1}, {0,1,1}
	};
	static int[][][] u_two_gainedinfo = {
		// u_stage_one = 0 {u_stage_two = 0, 1, 2, 3}
		{{0,0,0},{0,1,0},{0,0,1},{0,1,1}},
		// u_stage_one = 1 {u_stage_two = 0, 1, 2, 3}
		{{0,0,0},{0,0,0},{0,0,1},{0,0,1}},
		// u_stage_one = 2 {u_stage_two = 0, 1, 2, 3}
		{{0,0,0},{0,1,0},{0,0,0},{0,1,0}},
		// u_stage_one = 3 {u_stage_two = 0, 1, 2, 3}
		{{0,0,0},{0,0,0},{0,0,0},{0,0,0}}
	};
	
	/* probabilities */
	static double[][][] randd_prob = DMInputs.getRandD();
	static int[] htgr_cost = new int[DMInputs.getHTGRCapitalCost().length];
	static double[] htgr_prob = DMInputs.getHTGRCapCostProb();
	static int[] sfr_cost = new int[DMInputs.getSFRCapitalCost().length];
	static double[] sfr_prob = DMInputs.getSFRCapCostProb();
	
	static double[][][][][][][][][] Dat;
	
	static int[][][][][][][] u_three_pi;
	static int[][][][][][] u_two_pi;
	static int[][][][][] g_two_pi;
	static int[][][][] u_one_pi;
	static int[][][] g_one_pi;
	
	//static int
	static int[][][][][] u_two_hedge;
	
//	/* For perfect information strategies */

//	
//	/* For hedging strategies */
//	/* iff htgr or sfr outcome == 3, this means that the capital cost hasn't been realized */
//	static int[][][][][][] u_two_hedge;
//	static int[][][][][] g_two_hedge;
//	static int[][] u_one_hedge;
//	static int g_one_hedge;
	


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
		decide.dimensionArrays();
		/* get the data, then normalize it */
		decide.loadData();
		decide.normalizeData();
		/* get the perfect info strategies based on that info */
		//decide.getPerfectInformationStrategies();
		/* get the hedging strategies */
		//decide.getHedgingStrategies();
		/* print the hedging strategy results */
		//decide.printHedgingStrategies();
	}	
	
	public void dimensionArrays() {
		
		Dat = new double [u_one.length][u_one.length][u_one.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length][3];
		/* u_one, u_two and u_three have the same max length */
		
		u_three_pi = new int[u_one.length][u_two.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		u_two_pi = new int[u_one.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		g_two_pi = new int[u_one.length][g_one.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		u_one_pi = new int[g_one.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		g_one_pi = new int[disp_cost.length][htgr_cost.length][sfr_cost.length];
		
		u_two_hedge = new int[g_one.length][disp_cost.length][u_one.length][htgr_cost.length][sfr_cost.length];
		
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
		
		double[] min_val = {1.e-6,1.e-6,1.e-6};
		double[] max_val = {0,0,0};
		int val, count=0;
		
		for (u_o=0; u_o<u_one.length; u_o++) {
			for (u_tw=0; u_tw<u_two.length; u_tw++) {
				for (u_th=0; u_th<u_three[u_o][u_tw].length; u_th++) {
					for (g_o=0; g_o<g_one.length; g_o++) {
						for (g_tw=0; g_tw<g_two.length; g_tw++) {
							for (disp=0; disp<disp_cost.length; disp++) {
								for (htgr=0; htgr<htgr_cost.length; htgr++) {
									for (sfr=0; sfr<sfr_cost.length; sfr++) {
										for (val=0; val<min_val.length; val++) {
											if(Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][val] <= min_val[val]) min_val[val] = Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][val];
											if(Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][val] >= max_val[val]) max_val[val] = Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][val];
										}
										count++;
									}
								}
							}
						}

					}
				}
			}
		}
		
		double[] norm_val = {0,0,0};
		double[] diff_val = {0,0,0};
		
		for (int k=0; k<diff_val.length; k++) diff_val[k] = max_val[k] - min_val[k];
		
		for (u_o=0; u_o<u_one.length; u_o++) {
			for (u_tw=0; u_tw<u_two.length; u_tw++) {
				for (u_th=0; u_th<u_three[u_o][u_tw].length; u_th++) {
					for (g_o=0; g_o<g_one.length; g_o++) {
						for (g_tw=0; g_tw<g_two.length; g_tw++) {
							for (disp=0; disp<disp_cost.length; disp++) {
								for (htgr=0; htgr<htgr_cost.length; htgr++) {
									for (sfr=0; sfr<sfr_cost.length; sfr++) {
										
										/* 0 = Total COE; 1 = Total Decay Heat; 2 = Average Nuclear Security Measure */
										norm_val[0] = 1 - (Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][0]-min_val[0])/diff_val[0];
										norm_val[1] = 1 - (Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][1]-min_val[1])/diff_val[1];
										norm_val[2] = (Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][2]-min_val[2])/diff_val[2];
										Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr] = norm_val;
										System.out.print(norm_val[0] + " " + norm_val[1] + " " + norm_val[2] + "\n");
										
									}
								}
							}
						}

					}
				}
			}
		}

	}	
	
	public void getPerfectInformationStrategies() {
		
		int g_o, g_tw;
		int u_o, u_tw, u_th;
		int disp, htgr, sfr;
		
		int i;
		double val = 0.;
		
		/* Perfect Information Strategy for U's stage three play (given G's stage one and two plays; U's stage one and two plays; and all Nature's plays) */
		for (u_o=0; u_o<u_one.length; u_o++) {
			for (u_tw=0; u_tw<u_two.length; u_tw++) {
				for (g_o=0; g_o<g_one.length; g_o++) {
					for (g_tw=0; g_tw<g_two.length; g_tw++) {
						for (disp=0; disp<disp_cost.length; disp++) {
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									double[] temp_double = new double[u_three[u_o][u_tw].length];
									for (u_th=0; u_th<u_three[u_o][u_tw].length; u_th++) {
										val = getVal(u_weight, Dat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr]);
										temp_double[u_th] = val;
									}
									u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr] = u_three[u_o][u_tw][getIndexOfMax(temp_double)];
								}
							}
						}
					}
				}
			}
		}
		
		/* Perfect Information Strategy for U's stage two play (given G's stage one and two plays; U's stage one play; and all Nature's plays) */
		for (u_o=0; u_o<u_one.length; u_o++) {
			for (g_o=0; g_o<g_one.length; g_o++) {
				for (g_tw=0; g_tw<g_two.length; g_tw++) {
					for (disp=0; disp<disp_cost.length; disp++) {
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								double[] temp_double = new double[u_two.length];
								for (i=0; i<temp_double.length; i++) temp_double[i] = 0.;
								val = 0.;
								for (u_tw=0; u_tw<u_two.length; u_tw++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									val = getVal(u_weight, Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[u_tw] = val;
								}
								u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr] = u_two[getIndexOfMax(temp_double)];
							}
						}
					}
				}
			}
		}
		
		/* Perfect Information Strategy for G's stage two play (given G's stage one play; U's stage one play; and all Nature's plays) */
		for (u_o=0; u_o<u_one.length; u_o++) {
			for (g_o=0; g_o<g_one.length; g_o++) {
				for (disp=0; disp<disp_cost.length; disp++) {
					for (htgr=0; htgr<htgr_cost.length; htgr++) {
						for (sfr=0; sfr<sfr_cost.length; sfr++) {
							double[] temp_double = new double[g_two.length];
							val = 0.;
							for (g_tw=0; g_tw<g_two.length; g_tw++) {
								u_tw = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr];
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								val = getVal(g_weight, Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
								temp_double[g_tw] = val;
							}
							g_two_pi[u_o][g_o][disp][htgr][sfr] = g_two[getIndexOfMax(temp_double)];
						}
					}
				}
			}
		}
		
		/* Perfect Information Strategy for U's stage one play (given G's stage one play; and all Nature's plays) */
		for (g_o=0; g_o<g_one.length; g_o++) {
			for (disp=0; disp<disp_cost.length; disp++) {
				for (htgr=0; htgr<htgr_cost.length; htgr++) {
					for (sfr=0; sfr<sfr_cost.length; sfr++) {
						double[] temp_double = new double[u_one.length];
						val = 0.;
						for (u_o=0; u_o<u_one.length; u_o++) {
							g_tw = g_two_pi[u_o][g_o][disp][htgr][sfr];
							u_tw = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr];
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
							val = getVal(u_weight, Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
							temp_double[u_o] = val;
						}
						u_one_pi[g_o][disp][htgr][sfr] = u_one[getIndexOfMax(temp_double)];
					}
				}
			}
		}
		
		/* Perfect Information Strategy for G's stage one play (given all Nature's plays) */
		for (disp=0; disp<disp_cost.length; disp++) {
			for (htgr=0; htgr<htgr_cost.length; htgr++) {
				for (sfr=0; sfr<sfr_cost.length; sfr++) {
					double[] temp_double = new double[g_one.length];
					val = 0.;
					for (g_o=0; g_o<g_one.length; g_o++) {
						u_o = u_one_pi[g_o][disp][htgr][sfr];
						g_tw = g_two_pi[g_o][u_o][disp][htgr][sfr];
						u_tw = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr];
						u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
						val = getVal(g_weight, Dat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
						temp_double[g_o] = val;
					}
					g_one_pi[disp][htgr][sfr] = g_one[getIndexOfMax(temp_double)];
				}
			}
		}
		
	}

	public void getHedgingStrategies() {
		
		int g_o, g_tw;
		int u_o, u_tw, u_th;
		int disp, htgr, sfr;
		
		int[][][] rx_cost_one = { 
				{{3},{3}},
				{{0,1,2},{3}},
				{{3},{0,1,2}},
				{{0,1,2},{0,1,2}}
		};
		
		int[][][][] rx_cost_two = {
			
			/* u_two decision 0	*/ { {{3},{3}}, {{0,1,2},{3}}, {{3}, {0,1,2}} },
			/* u_two decision 1	*/ { { {0,1,2},{3} }, { {0,1,2},{} }, {}, {} },
			/* u_two decision 2	*/ { },
			/* u_two decision 3 */	{ }
				
		};
		
		int[][] one_info = {
			{0,0,0}, {0,1,0}, {0,0,1}, {0,1,1}
		};
		int[][][] two_info = {
			// u_stage_one = 0 {u_stage_two = 0, 1, 2, 3}
			{{0,0,0},{0,1,0},{0,0,1},{0,1,1}},
			// u_stage_one = 1 {u_stage_two = 0, 1, 2, 3}
			{{0,0,0},{0,0,0},{0,0,1},{0,0,1}},
			// u_stage_one = 2 {u_stage_two = 0, 1, 2, 3}
			{{0,0,0},{0,1,0},{0,0,0},{0,1,0}},
			// u_stage_one = 3 {u_stage_two = 0, 1, 2, 3}
			{{0,0,0},{0,0,0},{0,0,0},{0,0,0}}
		};
		
		/* Get U's stage two hedging strategy (knowing G's stage one and two plays; U's stage one play; Nature's moves so far) */
		for (g_o=0; g_o<g_one.length; g_o++) {
			for (u_o=0; u_o<u_one.length; u_o++) {
				for (g_tw=0; g_tw<g_two.length; g_tw++) {
					for (disp=0; disp<disp_cost.length; disp++) {
						double[] temp = new double[u_two.length];
						for (u_tw=0; u_tw<u_two.length; u_tw++) {
							
							if (two_info[u_o])
							
							
							
						}
					}
				}
			}
		}
		
		
//		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
//			for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
//				for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
//					for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) {
//						if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==0) {
//							u_one_htgr_outcome = 3;
//							u_one_sfr_outcome = 3;
//							double[] temp_double = new double[u_two_strategy.length];
//							double chance = 0;
//							double obj_function = 0;
//							for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
//								for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_htgr_capcost; u_two_htgr_outcome++) {
//									for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
//										u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome];
//										chance = htgr_capcost_probability[u_two_htgr_outcome]*sfr_capcost_probability[u_two_sfr_outcome];
//										obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome]);
//										temp_double[u_stage_two] += chance*obj_function;
//									}
//								}
//							}
//							u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = u_two_strategy[getIndexOfMax(temp_double)];
//						} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==0) {
//							u_one_sfr_outcome = 3;
//							for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
//								double[] temp_double = new double[u_two_strategy.length];
//								double chance = 0;
//								double obj_function = 0;
//								for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
//									for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
//										u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome];
//										chance = sfr_capcost_probability[u_two_sfr_outcome];
//										obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome]);
//										temp_double[u_stage_two] += chance*obj_function;
//									}
//								}
//								u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = u_two_strategy[getIndexOfMax(temp_double)];
//							}
//						} else if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==1) {
//							u_one_htgr_outcome = 3;
//							for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
//								double[] temp_double = new double[u_two_strategy.length];
//								double chance = 0;
//								double obj_function = 0;
//								for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
//									for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_sfr_capcost; u_two_htgr_outcome++) {
//										u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome];
//										chance = htgr_capcost_probability[u_two_htgr_outcome];
//										obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome]);
//										temp_double[u_stage_two] += chance*obj_function;
//									}
//								}
//								u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = u_two_strategy[getIndexOfMax(temp_double)];
//							}
//						} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==1) {
//							for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
//								for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
//									u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = u_two_perfectinfo[u_stage_one][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome];
//								}
//							}
//						}
//					}
//				}
//			}
//		}
		
		
	}
	
	public double getVal(double[] weight, double[] vals) {
		double val = 0.;
		for (int i=0; i<vals.length; i++) val+=weight[i]*vals[i];
		return(val);
	}
	
	public int getIndexOfMax(double[] vals) {
		int best=0;
		double best_val=0;
		for (int i=0; i<vals.length; i++) {
			if (vals[i]>best_val) best_val = vals[i]; best = i;
		}
		return(best);
	}
	
} // DecisionMaker class
