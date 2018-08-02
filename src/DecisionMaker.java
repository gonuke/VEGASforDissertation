import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/*
 * New deterministic equivalent solver
 * for @birdybird's dissertation
 */



public class DecisionMaker {

	static boolean fuck=false;
	
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
	static double[][] disp_prob = DMInputs.getDisposalCostProbabilities();
	static int[] htgr_cost = new int[DMInputs.getHTGRCapitalCost().length];
	static double[] htgr_prob = DMInputs.getHTGRCapCostProb();
	static int[] sfr_cost = new int[DMInputs.getSFRCapitalCost().length];
	static double[] sfr_prob = DMInputs.getSFRCapCostProb();
	
	static double[][][][][][][][][] Dat;
	static double[][][][][][][][][] NormDat;
	
	static int[][][][][][][] u_three_pi;
	static int[][][][][][] u_two_pi;
	static int[][][][][] g_two_pi;
	static int[][][][] u_one_pi;
	static int[][][] g_one_pi;
	
	//static int
	static int[][][][][][] u_two_hedge;
	static int[][][][][] g_two_hedge;
	static int[][] u_one_hedge;
	static int g_one_hedge;
	
	
	public DecisionMaker() {
		
	}
	
	public static void main(String args[]) {
		DecisionMaker decide = new DecisionMaker();
		decide.dimensionArrays();
		/* get the data, then normalize it */
		decide.loadData();
		decide.normalizeData();
		/* get the perfect info strategies based on that info */
		decide.getPerfectInformationStrategies();
		/* get the hedging strategies */
		decide.getHedgingStrategies();
		/* print the hedging strategy results */
		decide.printHedgingStrategies();
		/* print the perfect information strategies */
		decide.printPerfectInformationStrategies();
	}	
	
	public void dimensionArrays() {
		
		int i=0;
		for (i=0; i<rep_cost.length; i++) rep_cost[i] = i;
		for (i=0; i<disp_cost.length; i++) disp_cost[i] = i;
		for (i=0; i<g_one.length; i++) g_one[i] = i;
		for (i=0; i<g_two.length; i++) g_two[i] = i;
		
		Dat = new double [u_one.length][u_one.length][u_one.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length][3];
		/* u_one, u_two and u_three have the same max length */
		NormDat = new double [u_one.length][u_one.length][u_one.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length][3];
		
		u_three_pi = new int[u_one.length][u_two.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		u_two_pi = new int[u_one.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		g_two_pi = new int[u_one.length][g_one.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		u_one_pi = new int[g_one.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		g_one_pi = new int[disp_cost.length][htgr_cost.length][sfr_cost.length];
		
		/* if the htgr or sfr cost is (rx_cost.length+1) then it means the previous stage didn't yield information about the cost */
		u_two_hedge = new int[g_one.length][disp_cost.length][u_one.length][htgr_cost.length+1][sfr_cost.length+1][g_two.length];
		g_two_hedge = new int[g_one.length][disp_cost.length][u_one.length][htgr_cost.length+1][sfr_cost.length+1];
		u_one_hedge = new int[g_one.length][disp_cost.length];
		
	}
	
	public void loadData() {

		String inputFile = "DecisionMakingResults.txt";
		String file_path = System.getProperty("user.dir") + File.separatorChar + inputFile;
		File data = new File(file_path);
		BufferedReader buf;
		String current_line = " anything ";
		StringTokenizer st;
		int i,j,k;
		double[] dummy_double = {0,0,0};
		
		int[] ints = {0,0,0,0,0,0,0,0};
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
				for (i=0; i<dummy_double.length; i++) Dat[ints[0]][ints[1]][ints[2]][ints[3]][ints[4]][ints[5]][ints[6]][ints[7]][i] = Double.valueOf( st.nextToken() ).doubleValue();
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
		int disp, htgr, sfr;
		
		double[] min_val = {1.e20,1.e20,1.e20};
		double[] max_val = {0,0,0};
		int val;

		try {

			String user_dir = System.getProperty("user.dir");
			File output_target = new File(user_dir+File.separatorChar+"NormalizedData.txt");

			if(output_target.exists()) output_target.delete();
			FileWriter output_filewriter = new FileWriter(output_target);
			PrintWriter output_writer = new PrintWriter(output_filewriter);

			output_writer.print("u_first u_second u_last g_one g_two disp_cost htgr_cost sfr_cost coe heat_load ns_measure");
			output_writer.print("\n");

			for (u_o=0; u_o<u_one.length; u_o++) {
				for (u_tw=0; u_tw<u_two.length; u_tw++) {
					for (u_th=0; u_th<u_three[u_o][u_tw].length; u_th++) {
						for (g_o=0; g_o<g_one.length; g_o++) {
							for (g_tw=0; g_tw<g_two.length; g_tw++) {
								for (disp=0; disp<disp_cost.length; disp++) {
									for (htgr=0; htgr<htgr_cost.length; htgr++) {
										for (sfr=0; sfr<sfr_cost.length; sfr++) {
											for (val=0; val<min_val.length; val++) {
												if(Dat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr][val] <= min_val[val]) min_val[val] = Dat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr][val];
												if(Dat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr][val] >= max_val[val]) max_val[val] = Dat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr][val];
											}
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

			for (u_o=0; u_o<u_one.length; u_o++) {
				for (u_tw=0; u_tw<u_two.length; u_tw++) {
					for (u_th=0; u_th<u_three[u_o][u_tw].length; u_th++) {
						for (g_o=0; g_o<g_one.length; g_o++) {
							for (g_tw=0; g_tw<g_two.length; g_tw++) {
								for (disp=0; disp<disp_cost.length; disp++) {
									for (htgr=0; htgr<htgr_cost.length; htgr++) {
										for (sfr=0; sfr<sfr_cost.length; sfr++) {

											for (int k=0; k<diff_val.length; k++) diff_val[k] = max_val[k] - min_val[k];

											/* 0 = Total COE; 1 = Total Decay Heat; 2 = Average Nuclear Security Measure */
											norm_val[0] = 1 - (Dat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr][0]-min_val[0])/diff_val[0];
											norm_val[1] = 1 - (Dat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr][1]-min_val[1])/diff_val[1];
											norm_val[2] = (Dat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr][2]-min_val[2])/diff_val[2];
											
											for (int k=0; k<norm_val.length; k++) NormDat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr][k] = norm_val[k];

											output_writer.print(u_o + " " + u_tw + " " + u_three[u_o][u_tw][u_th] + " ");
											output_writer.print(g_o + " " + g_tw + " ");
											output_writer.print(disp + " " + htgr + " " + sfr + " ");
											output_writer.print(norm_val[0] + " " + norm_val[1] + " " + norm_val[2] + "\n");

										}
									}
								}
							}

						}
					}
				}
			}
		} catch (IOException e) {
			System.out.print("Error writing normalized data values");
		}
		System.out.print("Finished printing normalized data" + "\n");
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
										val = getVal(u_weight, NormDat[u_o][u_tw][u_three[u_o][u_tw][u_th]][g_o][g_tw][disp][htgr][sfr]);
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
									val = getVal(u_weight, NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
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
							for (i=0; i<temp_double.length; i++) temp_double[i] = 0.;
							val = 0.;
							for (g_tw=0; g_tw<g_two.length; g_tw++) {
								u_tw = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr];
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								val = getVal(g_weight, NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
								temp_double[g_tw] = val;
							}
							g_two_pi[u_o][g_o][disp][htgr][sfr] = g_two[getIndexOfMax(temp_double)];
							g_tw = g_two_pi[u_o][g_o][disp][htgr][sfr];
							u_tw = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr];
							//if (u_tw==2 && g_tw!=2) g_two_pi[u_o][g_o][disp][htgr][sfr] = 2;
							//if (u_tw==3 && g_tw!=3) g_two_pi[u_o][g_o][disp][htgr][sfr] = 3;
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
							val = getVal(u_weight, NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
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
						g_tw = g_two_pi[u_o][g_o][disp][htgr][sfr];
						u_tw = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr];
						u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
						val = getVal(g_weight, NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
						temp_double[g_o] = val;
					}
					g_one_pi[disp][htgr][sfr] = g_one[getIndexOfMax(temp_double)];
				}
			}
		}
		
	}

	public void getHedgingStrategies() {
		
		int g_o, g_tw = 0;
		int u_o, u_tw = 0, u_th;
		int disp, htgr, sfr;

		double chance=0.;
		double val=0.;
		
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
			for (disp=0; disp<disp_cost.length; disp++) {
				for (u_o=0; u_o<u_one.length; u_o++) {
					for (g_tw=0; g_tw<g_two.length; g_tw++) {
						
						if (one_info[u_o][1]==0 && one_info[u_o][2]==0) { // no information; hedge against all costs
							double[] temp_double = new double[u_two.length];
							for (u_tw=0; u_tw<u_two.length; u_tw++) {
								for (htgr=0; htgr<htgr_cost.length; htgr++) {
									for (sfr=0; sfr<sfr_cost.length; sfr++) {
										u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
										chance = htgr_prob[htgr]*sfr_prob[sfr];
										val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
										temp_double[u_tw] += chance*val;
									}
								}
							}
							u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw] = u_two[getIndexOfMax(temp_double)];
						}
						
						if (one_info[u_o][1]==1 && one_info[u_o][2]==0) { 
							for (htgr=0; htgr<htgr_cost.length; htgr++) { // known htgr cost; hedge against unknown sfr cost
								double[] temp_double = new double[u_two.length];
								for (u_tw=0; u_tw<u_two.length; u_tw++) {
									for (sfr=0; sfr<sfr_cost.length; sfr++) {
										u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
										chance = sfr_prob[sfr];
										val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
										temp_double[u_tw] += chance*val;
									}
								}
								u_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length][g_tw] = u_two[getIndexOfMax(temp_double)];
							}
						}
						
						if (one_info[u_o][1]==0 && one_info[u_o][2]==1) {
							for (sfr=0; sfr<sfr_cost.length; sfr++) { // known sfr cost; hedge against unknown htgr cost
								double[] temp_double = new double[u_two.length];
								for (u_tw=0; u_tw<u_two.length; u_tw++) {
									for (htgr=0; htgr<htgr_cost.length; htgr++) {
										u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
										chance = htgr_prob[htgr];
										val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
										temp_double[u_tw] += chance*val;
									}
								}
								u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr][g_tw] = u_two[getIndexOfMax(temp_double)];
							}
						}
						
						if (one_info[u_o][1]==1 && one_info[u_o][2]==1) {
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_two_hedge[g_o][disp][u_o][htgr][sfr][g_tw] = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr]; // no hedge if information already known
								}
							}
						}
							
					}
				}
			}
		}

		/* Get G's stage two hedging strategy (knowing G's stage one play; U's stage one play; Nature's moves so far) */
		for (g_o=0; g_o<g_one.length; g_o++) {
			for (disp=0; disp<disp_cost.length; disp++) {
				for (u_o=0; u_o<u_one.length; u_o++) {

					if (one_info[u_o][1]==0 && one_info[u_o][2]==0) {
						double[] temp_double = new double[g_two.length];
						for (g_tw=0; g_tw<g_two.length; g_tw++) {
							u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr]*sfr_prob[sfr];
									val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[g_tw] += chance*val;
								}
							}
						}
						g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = g_two[getIndexOfMax(temp_double)];
						g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length];
						u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
						//if (u_tw==2 && g_tw!=2) g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = u_tw;
						//if (u_tw==3 && g_tw!=3) g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = u_tw;
					}

					if (one_info[u_o][1]==1 && one_info[u_o][2]==0) {
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							double[] temp_double = new double[g_two.length];
							for (g_tw=0; g_tw<g_two.length; g_tw++) {
								u_tw = u_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length][g_tw];
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = sfr_prob[sfr];
									val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[g_tw] += chance*val;
								}
							}
							g_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length] = g_two[getIndexOfMax(temp_double)];
							g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length];
							u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
							//if (u_tw==2 && g_tw!=2) g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = u_tw;
							//if (u_tw==3 && g_tw!=3) g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = u_tw;
						}
					}
					
					if (one_info[u_o][1]==0 && one_info[u_o][2]==1) {
						for (sfr=0; sfr<sfr_cost.length; sfr++) {
							double[] temp_double = new double[g_two.length];
							for (g_tw=0; g_tw<g_two.length; g_tw++) {
								u_tw = u_two_hedge[g_o][disp][u_o][sfr][htgr_cost.length][g_tw];
								for (htgr=0; htgr<htgr_cost.length; htgr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr];
									val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[g_tw] += chance*val;
								}
							}
							g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr] = g_two[getIndexOfMax(temp_double)]; 
							g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length];
							u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
							//if (u_tw==2 && g_tw!=2) g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = u_tw;
							//if (u_tw==3 && g_tw!=3) g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = u_tw;
						}
					}
					
					if (one_info[u_o][1]==1 && one_info[u_o][2]==1) { /* all uncertainties resolved; g two hedge is just the perfect information strategy then */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								g_tw = g_two_pi[u_o][g_o][disp][htgr][sfr];
								g_two_hedge[g_o][disp][u_o][htgr][sfr] = g_tw;
								u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
								//if (u_tw==2 && g_tw!=2) g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = u_tw;
								//if (u_tw==3 && g_tw!=3) g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = u_tw;
							}
						}
					}
					
				}
			}
		}

		/* Get U's stage one hedging strategy (knowing G's stage one play; Nature's moves so far) */
		for (g_o=0; g_o<g_one.length; g_o++) {
			for (disp=0; disp<disp_cost.length; disp++) {

				double[] temp_double = new double[u_one.length];
				for (u_o=0; u_o<u_one.length; u_o++) {

					if (one_info[u_o][1]==0 && one_info[u_o][2]==0) { /* LWR; two_info can take values of {0,0,0}, {0,1,0}, {0,0,1}, {0,1,1} */
						g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length];
						u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
						if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][0];
							temp_double[u_o] += getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]);
						}
						if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][0];
								chance = htgr_prob[htgr];
								val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0]);
								temp_double[u_o] += chance*val;
							}
						}
						if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][sfr];
								chance = sfr_prob[sfr];
								val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr]);
								temp_double[u_o] += chance*val;
							}
						}
						if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==1) { /* two_info {0,1,1} */
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr]*sfr_prob[sfr];
									val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[u_o] += chance*val;
								}
							}
						}
					}

					if (one_info[u_o][1]==1 && one_info[u_o][2]==0) { /* HTGR; two_info can only take values of {0,0,0} and {0,0,1} */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							g_tw = g_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length];
							u_tw = u_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length][g_tw];
							if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][0];
								chance = htgr_prob[htgr];
								val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0]);
								temp_double[u_o] += chance*val;
							}
							if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr]*sfr_prob[sfr];
									val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[u_o] += chance*val;
								}
							}
						}
					}

					if (one_info[u_o][1]==0 && one_info[u_o][2]==1) { /* SFR recyling LWR fuel; two_info can only take values of {0,0,0} and {0,1,0} */
						for (sfr=0; sfr<sfr_cost.length; sfr++) {
							g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr];
							u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr][g_tw]; 
							if (two_info[u_o][u_tw][1] == 0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][sfr];
								chance = sfr_prob[sfr];
								val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr]);
								temp_double[u_o] += chance*val;
							}
							if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
								for (htgr=0; htgr<htgr_cost.length; htgr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr]*sfr_prob[sfr];
									val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[u_o] += chance*val;
								}
							}
						}
					}

					if (one_info[u_o][1]==1 && one_info[u_o][2]==1) { /* SFR recycling HTGR fuel; two_info can only take values of {0,0,0} */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								g_tw = g_two_hedge[g_o][disp][u_o][htgr][sfr];
								u_tw = u_two_hedge[g_o][disp][u_o][htgr][sfr][g_tw];
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								chance = htgr_prob[htgr]*sfr_prob[sfr];
								val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
								temp_double[u_o] += chance*val;
							}
						}
					}

				} /* indexed over all u_one */
				u_one_hedge[g_o][disp] = u_one[getIndexOfMax(temp_double)];
			}
		}
		
		/* Get G's stage one hedging strategy */
		double[] temp_double = new double[g_one.length];
		for (g_o=0; g_o<g_one.length; g_o++) {
			
			for (disp=0; disp<disp_cost.length; disp++) {
				u_o = u_one_hedge[g_o][disp];

				if (one_info[u_o][1]==0 && one_info[u_o][2]==0) { /* LWR; two_info can take values of {0,0,0}, {0,1,0}, {0,0,1}, {0,1,1} */
					g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length];
					u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
					if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */
						u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][0];
						chance = disp_prob[g_o][disp];
						val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]);
						temp_double[g_o] += chance*val;
					}
					if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][0];
							chance = disp_prob[g_o][disp]*htgr_prob[htgr];
							val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0]);
							temp_double[g_o] += chance*val;
						}
					}
					if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
						for (sfr=0; sfr<sfr_cost.length; sfr++) {
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][sfr];
							chance = disp_prob[g_o][disp]*sfr_prob[sfr];
							val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr]);
							temp_double[g_o] += chance*val;
						}
					}
					if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==1) { /* two_info {0,1,1} */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								chance = disp_prob[g_o][disp]*htgr_prob[htgr]*sfr_prob[sfr];
								val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
								temp_double[g_o] += chance*val;
							}
						}
					}
				}

				if (one_info[u_o][1]==1 && one_info[u_o][2]==0) { /* HTGR; two_info can only take values of {0,0,0} and {0,0,1} */
					for (htgr=0; htgr<htgr_cost.length; htgr++) {
						g_tw = g_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length];
						u_tw = u_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length][g_tw];
						if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][0];
							chance = disp_prob[g_o][disp]*htgr_prob[htgr];
							val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0]);
							temp_double[g_o] += chance*val;
						}
						if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								chance = disp_prob[g_o][disp]*htgr_prob[htgr]*sfr_prob[sfr];
								val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
								temp_double[g_o] += chance*val;
							}
						}
					}
				}

				if (one_info[u_o][1]==0 && one_info[u_o][2]==1) { /* SFR recyling LWR fuel; two_info can only take values of {0,0,0} and {0,1,0} */
					for (sfr=0; sfr<sfr_cost.length; sfr++) {
						g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr];
						u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr][g_tw]; 
						if (two_info[u_o][u_tw][1] == 0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][sfr];
							chance = disp_prob[g_o][disp]*sfr_prob[sfr];
							val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr]);
							temp_double[g_o] += chance*val;
						}
						if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								chance = disp_prob[g_o][disp]*htgr_prob[htgr]*sfr_prob[sfr];
								val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
								temp_double[g_o] += chance*val;
							}
						}
					}
				}

				if (one_info[u_o][1]==1 && one_info[u_o][2]==1) { /* SFR recycling HTGR fuel; two_info can only take values of {0,0,0} */
					for (htgr=0; htgr<htgr_cost.length; htgr++) {
						for (sfr=0; sfr<sfr_cost.length; sfr++) {
							g_tw = g_two_hedge[g_o][disp][u_o][htgr][sfr];
							u_tw = u_two_hedge[g_o][disp][u_o][htgr][sfr][g_tw];
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
							chance = disp_prob[g_o][disp]*htgr_prob[htgr]*sfr_prob[sfr];
							val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
							temp_double[g_o] += chance*val;
						}
					}
				}
			
			}
			g_one_hedge = g_one[getIndexOfMax(temp_double)];

		}

	}
	
	public void printHedgingStrategies() {
		
		int[] hedge = new int[10];
		/* 0: g_one; 1: disp; 2: u_one; 3: htgr_one; 4: sfr_one; 5: g_two; 6: u_two; 7: htgr_two; 8: sfr_two; 9: u_three */
		int g_o;
		int disp;
		int u_o, u_tw, u_th, htgr, sfr;
		int g_tw;
		
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
		
		try {

			String user_dir = System.getProperty("user.dir");
			File output_target = new File(user_dir+File.separatorChar+"HedgingStrategyResults.txt");

			if(output_target.exists()) output_target.delete();
			FileWriter output_filewriter = new FileWriter(output_target);
			PrintWriter output_writer = new PrintWriter(output_filewriter);
			
			output_writer.print("g_one_rep disp_cost u_first htgr_one sfr_one g_two_sub u_second htgr_two sfr_two u_last");
			output_writer.print("\n");
			
			g_o = g_one_hedge; 
			hedge[0] = g_one_hedge;
			
			for (disp=0; disp<disp_cost.length; disp++) {
				hedge[1] = disp;
				u_o = u_one_hedge[g_o][disp]; 
				hedge[2] = u_o;
				
				
				if (one_info[u_o][1]==0 && one_info[u_o][2]==0) { /* LWR; two_info can take values of {0,0,0}, {0,1,0}, {0,0,1}, {0,1,1} */
					
					hedge[3] = htgr_cost.length; 
					hedge[4] = sfr_cost.length;
					g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length]; 
					hedge[5] = g_tw;
					u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw]; 
					hedge[6] = u_tw;
					//if (u_tw==2 && g_tw!=u_tw) hedge[5] = u_tw;
					//if (u_tw==3 && g_tw!=u_tw) hedge[5] = u_tw;
					if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */
						hedge[7] = htgr_cost.length; 
						hedge[8] = sfr_cost.length;
						u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][0]; 
						hedge[9] = u_th;
					}
					for (int k=0; k<9; k++) output_writer.print(hedge[k] + " ");
					output_writer.print(hedge[9] + "\n");
					
					if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							hedge[7] = htgr; 
							hedge[8] = sfr_cost.length;
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][0]; 
							hedge[9] = u_th;
							for (int k=0; k<9; k++) output_writer.print(hedge[k] + " ");
							output_writer.print(hedge[9] + "\n");
						}
					}
					
					if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
						for (sfr=0; sfr<sfr_cost.length; sfr++) {
							hedge[7] = htgr_cost.length; 
							hedge[8] = sfr;
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][sfr]; 
							hedge[9] = u_th;
							for (int k=0; k<9; k++) output_writer.print(hedge[k] + " ");
							output_writer.print(hedge[9] + "\n");
						}
					}
					if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==1) { /* two_info {0,1,1} */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								hedge[7] = htgr; 
								hedge[8] = sfr;
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr]; 
								hedge[9] = u_th;
								for (int k=0; k<9; k++) output_writer.print(hedge[k] + " ");
								output_writer.print(hedge[9] + "\n");
							}
						}
					}
				}

				if (one_info[u_o][1]==1 && one_info[u_o][2]==0) { /* HTGR; two_info can only take values of {0,0,0} and {0,0,1} */
					for (htgr=0; htgr<htgr_cost.length; htgr++) {
						hedge[3] = htgr; 
						hedge[4] = sfr_cost.length;
						g_tw = g_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length]; 
						hedge[5] = g_tw;
						u_tw = u_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length][g_tw]; 
						hedge[6] = u_tw;
						//if (u_tw==2 && g_tw!=u_tw) hedge[5] = u_tw;
						//if (u_tw==3 && g_tw!=u_tw) hedge[5] = u_tw;
						if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */
							hedge[7] = htgr; 
							hedge[8] = sfr_cost.length;
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][0]; 
							hedge[9] = u_th;
							for (int k=0; k<9; k++) output_writer.print(hedge[k] + " ");
							output_writer.print(hedge[9] + "\n");
						}
						if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								hedge[7] = htgr; 
								hedge[8] = sfr;
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr]; 
								hedge[9] = u_th;
								for (int k=0; k<9; k++) output_writer.print(hedge[k] + " ");
								output_writer.print(hedge[9] + "\n");
							}
						}
					}
				}

				if (one_info[u_o][1]==0 && one_info[u_o][2]==1) { /* SFR recyling LWR fuel; two_info can only take values of {0,0,0} and {0,1,0} */
					for (sfr=0; sfr<sfr_cost.length; sfr++) {
						hedge[3] = htgr_cost.length; 
						hedge[4] = sfr;
						g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr]; 
						hedge[5] = g_tw;
						u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr][g_tw]; 
						hedge[6] = u_tw;
						//if (u_tw==2 && g_tw!=u_tw) hedge[5] = u_tw;
						//if (u_tw==3 && g_tw!=u_tw) hedge[5] = u_tw;
						if (two_info[u_o][u_tw][1] == 0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */
							hedge[7] = htgr_cost.length; 
							hedge[8] = sfr;
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][sfr]; 
							hedge[9] = u_th;
							for (int k=0; k<9; k++) output_writer.print(hedge[k] + " ");
							output_writer.print(hedge[9] + "\n");
						}
						if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								hedge[7] = htgr; 
								hedge[8] = sfr;
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr]; 
								hedge[9] = u_th;
								for (int k=0; k<9; k++) output_writer.print(hedge[k] + " ");
								output_writer.print(hedge[9] + "\n");
							}
						}
					}
				}

				if (one_info[u_o][1]==1 && one_info[u_o][2]==1) { /* SFR recycling HTGR fuel; two_info can only take values of {0,0,0} */
					for (htgr=0; htgr<htgr_cost.length; htgr++) {
						for (sfr=0; sfr<sfr_cost.length; sfr++) {
							hedge[3] = htgr; 
							hedge[4] = sfr;
							g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr]; 
							hedge[5] = g_tw;
							u_tw = u_two_hedge[g_o][disp][u_o][htgr][sfr][g_tw];
							hedge[6] = u_tw;
							//if (u_tw==2 && g_tw!=u_tw) hedge[5] = u_tw;
							//if (u_tw==3 && g_tw!=u_tw) hedge[5] = u_tw;
							hedge[7] = htgr; 
							hedge[8] = sfr;
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr]; 
							hedge[9] = u_th;
							for (int k=0; k<9; k++) output_writer.print(hedge[k] + " ");
							output_writer.print(hedge[9] + "\n");
						}
					}
				}
			}
			output_writer.close();

		} catch (IOException e) {
			System.out.print("Error writing Hedging Strategy results");
		}
		System.out.print("Finished printing hedging strategy results" + "\n");
	}
	
	public void printPerfectInformationStrategies() {
		
		int[] strategy = new int[8];
		/* 0: g_one; 1: disp; 2: u_one; 3: htgr_one; 4: sfr_one; 5: g_two; 6: u_two; 7: htgr_two; 8: sfr_two; 9: u_three */
		int g_o;
		int disp;
		int u_o, u_tw, u_th, htgr, sfr;
		int g_tw;
		boolean recycle=false;
		boolean htgr_only=false;
		
		try {

			String user_dir = System.getProperty("user.dir");
			File output_target = new File(user_dir+File.separatorChar+"PerfectInformationStrategies.txt");

			if(output_target.exists()) output_target.delete();
			FileWriter output_filewriter = new FileWriter(output_target);
			PrintWriter output_writer = new PrintWriter(output_filewriter);
			
			output_writer.print("disp_cost htgr_cost sfr_cost u_first u_second u_last g_one g_two_sub");
			output_writer.print("\n");

			for (disp=0; disp<disp_cost.length; disp++) {
				strategy[0] = disp;
				for (htgr=0; htgr<htgr_cost.length; htgr++) {
					strategy[1] = htgr;
					for (sfr=0; sfr<sfr_cost.length; sfr++) {
						strategy[2] = sfr;
						g_o = g_one_pi[disp][htgr][sfr];
						u_o = u_one_pi[g_o][disp][htgr][sfr]; 
						g_tw = g_two_pi[u_o][g_o][disp][htgr][sfr];
						u_tw = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr]; 
						u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];	
						strategy[3] = u_o; strategy[4] = u_tw; strategy[5] = u_th; strategy[6] = g_o; strategy[7] = g_tw;
						for (int k=0; k<strategy.length-1; k++) output_writer.print(strategy[k] + " ");
						output_writer.print(strategy[7] + "\n");
						
					}
				}
			}
			output_writer.close();

		} catch (IOException e) {
			System.out.print("Error writing perfect information strategies");
		}
		System.out.print("Finished printing perfect information strategies" + "\n");
	}
	
	public double getVal(double[] weight, double[] vals) {
		double val = 0.;
		for (int i=0; i<vals.length; i++) val+=weight[i]*vals[i];
		return(val);
	}

	
	public int getIndexOfMax(double[] vals) {
		int best=0;
		double best_val=vals[0];
		boolean equal=false;
		for (int i=0; i<vals.length; i++) {
			if (vals[i]>best_val) {
				best_val = vals[i]; 
				best = i; 
			}
		}
		return(best);
	}
	
} // DecisionMaker class
