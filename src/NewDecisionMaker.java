import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;


public class NewDecisionMaker {
	
	/* gained information from U's stage one and two plays
	 * [first reactor build decision #][reactor #]
	 * [first reactor build decision #][second reactor build decision #][reactor #]
	 */
	
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
	

	/* Player U's profile */

	/* U's objective function weights */
	static double[] u_weights = DMInputs.getUWeighting();
	/* U's try to build reactor scenarios */
	static int[] u_one_strategy = {0,1,2,3};
	static int[] u_two_strategy = {0,1,2,3};
	static int[][][] u_three_strategy = {
		{{0},{0,1},{0,2},{0,1,2,3}},
		{{0,1},{0,1},{0,1,2,3},{0,1,2,3}},
		{{0,2},{0,1,2,3},{0,2},{0,1,2,3}},
		{{0,1,2,3},{0,1,2,3},{0,1,2,3},{0,1,2,3}}
	};
	/* End */

	/* Player G's profile */
	/* G's objective function weights */
	static double[] g_weights = DMInputs.getGWeighting();
	/* G's R&D options */
	static double[] chosen_reprocessing_cost = DMInputs.getReprocessingCost();
	static int[] g_one_strategy;
	/* G's policy incentives: indexes over the possible capital subsidies offered  */
	static int[][] chosen_capital_subsidy = DMInputs.getCapitalSubsidy();
	static int[] g_two_strategy;
	/* End */

	/* Nature outcomes */
	/* 
	/* All the possible HTGR and SFR capital cost outcome combinations
	 * CapitalCostOutcomes[i] = index of NOAKCapitalCost outcome of NOAK LWR capital cost (known); */
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
	static double[] htgr_capcost_probability = DMInputs.getHTGRCapCostProb();
	static int n_htgr_capcost = DMInputs.getHTGRCapitalCost().length;
	static double[] sfr_capcost_probability = DMInputs.getSFRCapCostProb();
	static int n_sfr_capcost = DMInputs.getSFRCapitalCost().length;

	/* All disposal cost outcomes */
	//static double[][] DisposalCostOutcomes = DMInputs.getDisposalCostOutcomes();
	static int n_dispcost_outcome = DMInputs.getDisposalCostOutcomes().length;
	static double[][] dispcost_probability = DMInputs.getDisposalCostProbabilities();
	/* End */

	static double[][][][][][][][][] leaf_values;
	
	/* For perfect information strategies */
	/* u_three_perfectinfo[u_one][u_two][g_one][g_two][dispcost_outcome][htgr_capcost][sfr_capcost] */
	static int[][][][][][][] u_three_perfectinfo;
	static int[][][][][][] u_two_perfectinfo;
	static int[][][][][] g_two_perfectinfo;
	static int[][][][] u_one_perfectinfo;
	static int[][][] g_one_perfectinfo;
	
	/* For hedging strategies */
	/* iff htgr or sfr outcome == 3, this means that the capital cost hasn't been realized */
	static int[][][][][][] u_two_hedge;
	static int[][][][][] g_two_hedge;
	static int[][] u_one_hedge;
	static int g_one_hedge;
	
	

	public NewDecisionMaker() {
	}

	public static void main(String args[]) {
		NewDecisionMaker decide = new NewDecisionMaker();
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
	}

	public void dimensionArrays() {
		
		g_one_strategy = new int[chosen_reprocessing_cost.length];
		for (int i=0; i<chosen_reprocessing_cost.length; i++) g_one_strategy[i] = i;
		
		g_two_strategy = new int[chosen_capital_subsidy.length];
		for (int i=0; i<chosen_capital_subsidy.length; i++) g_two_strategy[i] = i;
		
		leaf_values = new double[u_one_strategy.length][u_one_strategy.length][u_one_strategy.length][g_one_strategy.length][g_two_strategy.length][n_dispcost_outcome][n_htgr_capcost][n_sfr_capcost][4];
		
		/* For perfect information strategies */
		/* u_three_perfectinfo[u_one][u_two][g_one][g_two][dispcost_outcome][htgr_capcost][sfr_capcost] */
		u_three_perfectinfo = new int[u_one_strategy.length][u_two_strategy.length][g_one_strategy.length][g_two_strategy.length][n_dispcost_outcome][n_htgr_capcost][n_sfr_capcost];
		u_two_perfectinfo = new int[u_one_strategy.length][g_one_strategy.length][g_two_strategy.length][n_dispcost_outcome][n_htgr_capcost][n_sfr_capcost];
		g_two_perfectinfo = new int[u_one_strategy.length][g_one_strategy.length][n_dispcost_outcome][n_htgr_capcost][n_sfr_capcost];
		u_one_perfectinfo = new int[g_one_strategy.length][n_dispcost_outcome][n_htgr_capcost][n_sfr_capcost];
		g_one_perfectinfo = new int[n_dispcost_outcome][n_htgr_capcost][n_sfr_capcost];

		/* For hedging strategies */
		/* iff htgr or sfr outcome == 3, this means that the capital cost hasn't been realized */
		u_two_hedge = new int[g_one_strategy.length][n_dispcost_outcome][u_one_strategy.length][n_htgr_capcost+1][n_sfr_capcost+1][g_two_strategy.length];
		g_two_hedge = new int[g_one_strategy.length][n_dispcost_outcome][u_one_strategy.length][n_htgr_capcost+1][n_sfr_capcost+1];
		u_one_hedge = new int[g_one_strategy.length][n_dispcost_outcome];
		g_one_hedge = 0;
		
	}
	
	public void loadData() {

		String inputFile = "DecisionMakingResults.txt";
		String file_path = System.getProperty("user.dir") + File.separatorChar + inputFile;
		File data = new File(file_path);
		BufferedReader buf;
		String current_line = " anything ";
		StringTokenizer st;
		int i,j;
		int[] leafInt = {0,0,0,0,0,0,0,0};
		double[] dummy_double = {0,0,0};

		try {

			int numberOfLines = readLines();
			buf = new BufferedReader(new FileReader(data));
			
			current_line = buf.readLine();
			
			for (j=0; j<numberOfLines-1; j++) {

				current_line = buf.readLine();

				st = new StringTokenizer(current_line);
				for(i=0; i<leafInt.length; i++) leafInt[i]=Integer.valueOf( st.nextToken() ).intValue();
				for(i=0; i<dummy_double.length; i++) {
					leaf_values[leafInt[0]][leafInt[1]][leafInt[2]][leafInt[3]][leafInt[4]][leafInt[5]][leafInt[6]][leafInt[7]][i] = Double.valueOf( st.nextToken() ).doubleValue();
				}
	
			}

		}
		catch(IOException IOE) {
			System.err.println("Decision Making Error 02: Error reading results from VEGAS "+ inputFile);
			System.err.println(IOE.toString());
		}

	}
	
	public int readLines() throws IOException {
		
		String inputFile = "DecisionMakingResults.txt";
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
		int g_stage_one, g_stage_two;
		int u_stage_one, u_stage_two, u_stage_three;
		int outcome_one, htgr_outcome, sfr_outcome;
		
		double[] min_value={0,0,0,0};
		double[] max_value={0,0,0,0};
		int value, count=0;
		
		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
			for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
				for (u_stage_three=0; u_stage_three<u_three_strategy[u_stage_one][u_stage_two].length; u_stage_three++) {
					for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
						for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
							for (outcome_one=0; outcome_one<n_dispcost_outcome; outcome_one++) {
								for (htgr_outcome=0; htgr_outcome<n_htgr_capcost; htgr_outcome++) {
									for (sfr_outcome=0; sfr_outcome<n_sfr_capcost; sfr_outcome++) {
										for (value=0; value<min_value.length; value++) {
											if (count==0) min_value[value] = leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value];
											if (count==0) max_value[value] = leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value];
											if (leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value] <= min_value[value]) {
												min_value[value] = leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value];
											}
											if (leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value] >= max_value[value]) {
												max_value[value] = leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value];
											}
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
		
		double[] normalized_value = {0,0,0,0};
		
		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
			for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
				for (u_stage_three=0; u_stage_three<u_three_strategy[u_stage_one][u_stage_two].length; u_stage_three++) {
					for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
						for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
							for (outcome_one=0; outcome_one<n_dispcost_outcome; outcome_one++) {
								for (htgr_outcome=0; htgr_outcome<n_htgr_capcost; htgr_outcome++) {
									for (sfr_outcome=0; sfr_outcome<n_sfr_capcost; sfr_outcome++) {
										for (value=0; value<min_value.length; value++) {
											if (value==2) { // proliferation metric .. right now is dose rate .. want high dose rate!
												if (min_value[value] == max_value[value]) {
													
												} else {
													normalized_value[value] = (leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value]-min_value[value])/(max_value[value]-min_value[value]);
													leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value] = normalized_value[value];
												}
											} else {
												if (min_value[value] == max_value[value]) {	// all other metrics .. want min values
													
												} else {
													normalized_value[value] = (leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value]-min_value[value])/(max_value[value]-min_value[value]);
													leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][outcome_one][htgr_outcome][sfr_outcome][value] = ((double) 1.-normalized_value[value]);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public double get_u_weight_leafvalue(double[] values) {
		double weighted_value=0.;
		for (int i=0; i<u_weights.length; i++) {
			weighted_value += u_weights[i]*values[i];
		}
		return(weighted_value);
	}
	public double get_g_weight_leafvalue(double[] values) {
		double weighted_value=0.;
		for (int i=0; i<g_weights.length; i++) {
			weighted_value += g_weights[i]*values[i];
		}
		return(weighted_value);
	}
		
	public void getPerfectInformationStrategies() {
		
		/* get all the perfect information strategies */
		int g_stage_one, u_stage_one, g_stage_two, u_stage_two, u_stage_three;
		int dispcost_outcome;
		int capital_cost;

		/* Perfect Information Strategy for U's stage three play given G's stage one and two play and U's stage one and two and three plays and all state of the world outcomes */
		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
			for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
				for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
					for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) 
						for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) { // N picks waste disposal cost{
							for (capital_cost=0; capital_cost<CapitalCostCombos.length; capital_cost++) { 
								double[] temp_double = new double[u_three_strategy[u_stage_one][u_stage_two].length];
								double obj_function = 0;
								for (u_stage_three=0; u_stage_three<u_three_strategy[u_stage_one][u_stage_two].length; u_stage_three++) {
									obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_three_strategy[u_stage_one][u_stage_two][u_stage_three]][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]]);
									temp_double[u_stage_three] = obj_function;
								}
								u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]] = u_three_strategy[u_stage_one][u_stage_two][getIndexOfMax(temp_double)];
							}
						}
				}
			}
		}

		/* Perfect Information Strategy for U's stage two play given G's stage one and two plays and U's stage one play knowing U's stage three resulting plays and all state of the world outcomes */
		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
			for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
				for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) 
					for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) { // N picks waste disposal cost{
						for (capital_cost=0; capital_cost<CapitalCostCombos.length; capital_cost++) { 
							double[] temp_double = new double[u_two_strategy.length];
							double obj_function = 0;
							for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
								obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]]);
								temp_double[u_stage_two] = obj_function;
							}
							u_two_perfectinfo[u_stage_one][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]] = u_two_strategy[getIndexOfMax(temp_double)];
						}
					}
			}
		}
		
		/* Perfect Information Strategy for G's stage two play given G's stage one play and U's stage one play knowing U's stage two and three resulting plays and all state of the world outcomes */
		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
			for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
				for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) { // N picks waste disposal cost{
					for (capital_cost=0; capital_cost<CapitalCostCombos.length; capital_cost++) { 
						double[] temp_double = new double[g_two_strategy.length];
						double obj_function = 0;
						for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
							u_stage_two = u_two_perfectinfo[u_stage_one][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
							u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
							obj_function = get_g_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]]);
							temp_double[g_stage_two] = obj_function;
						}
						g_two_perfectinfo[u_stage_one][g_stage_one][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]] = g_two_strategy[getIndexOfMax(temp_double)];
					}

				}
			}
		}
		
		/* Perfect Information Strategy for U's stage one play given G's stage one play knowing U's stage two and three and G's stage two resulting plays and all state of the world outcomes */
		for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
			for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) { // N picks waste disposal cost{
				for (capital_cost=0; capital_cost<CapitalCostCombos.length; capital_cost++) { 
					double[] temp_double = new double[u_one_strategy.length];
					double obj_function = 0;
					for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
						g_stage_two = g_two_perfectinfo[u_stage_one][g_stage_one][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
						u_stage_two = u_two_perfectinfo[u_stage_one][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
						u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
						obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]]);
						temp_double[u_stage_one] = obj_function;
					}
					u_one_perfectinfo[g_stage_one][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]] = u_one_strategy[getIndexOfMax(temp_double)];
				}
			}
		}
		
		/* Perfect Information Strategy for G's stage one play knowing G's stage two and U's stage one, two and three resulting plays and all state of the world outcomes */
			for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) { // N picks waste disposal cost{
				for (capital_cost=0; capital_cost<CapitalCostCombos.length; capital_cost++) { 
					double[] temp_double = new double[g_one_strategy.length];
					double obj_function = 0;
					for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
						u_stage_one = u_one_perfectinfo[g_stage_one][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
						g_stage_two = g_two_perfectinfo[u_stage_one][g_stage_one][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
						u_stage_two = u_two_perfectinfo[u_stage_one][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
						u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]];
						obj_function = get_g_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]]);
						temp_double[g_stage_one] = obj_function;
					}
					g_one_perfectinfo[dispcost_outcome][CapitalCostCombos[capital_cost][1]][CapitalCostCombos[capital_cost][2]] = g_one_strategy[getIndexOfMax(temp_double)];
				}
			}
			
	}
	
	public void getHedgingStrategies() {
		
		/* get all the hedging strategies */
		int g_stage_one, u_stage_one, g_stage_two, u_stage_two, u_stage_three;
		int dispcost_outcome;
		int u_one_htgr_outcome, u_one_sfr_outcome; // outcome of htgr and sfr capital costs after u's stage one decision
		int u_two_htgr_outcome, u_two_sfr_outcome; // outcome of htgr and sfr capital costs after u's stage two decision

		/* U's stage two hedging strategy 
		 * knowing G's stage one and two plays
		 * having made stage one play and knowing eventual stage three play
		 */

		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
			for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
				for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
					for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) {
						if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==0) {
							u_one_htgr_outcome = 3;
							u_one_sfr_outcome = 3;
							double[] temp_double = new double[u_two_strategy.length];
							double chance = 0;
							double obj_function = 0;
							for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
								for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_htgr_capcost; u_two_htgr_outcome++) {
									for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
										u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome];
										chance = htgr_capcost_probability[u_two_htgr_outcome]*sfr_capcost_probability[u_two_sfr_outcome];
										obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome]);
										temp_double[u_stage_two] += chance*obj_function;
									}
								}
							}
							//int the_max = getIndexOfMax(temp_double);
							u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = u_two_strategy[getIndexOfMax(temp_double)];
							//int the_hedge = u_two_strategy[getIndexOfMax(temp_double)];
							//u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = the_hedge;
							//System.out.print("the max is " + the_max + " and the hedge is " + the_hedge + " for g one " + g_stage_one + " disp cost " + dispcost_outcome + " u one " + u_stage_one + " htgr one and sfr one outcomes " + u_one_htgr_outcome + " and " + u_one_sfr_outcome + "\n");
						} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==0) {
							u_one_sfr_outcome = 3;
							for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
								double[] temp_double = new double[u_two_strategy.length];
								double chance = 0;
								double obj_function = 0;
								for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
									for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
										u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome];
										chance = sfr_capcost_probability[u_two_sfr_outcome];
										obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome]);
										temp_double[u_stage_two] += chance*obj_function;
									}
								}
								//int the_max = getIndexOfMax(temp_double);
								u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = u_two_strategy[getIndexOfMax(temp_double)];
								//int the_hedge = u_two_strategy[getIndexOfMax(temp_double)];
								//u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = the_hedge;
								//System.out.print("the max is " + the_max + " and the hedge is " + the_hedge + "\n");
								//System.out.print("the max is " + the_max + " and the hedge is " + the_hedge + " for g one " + g_stage_one + " disp cost " + dispcost_outcome + " u one " + u_stage_one + " htgr one and sfr one outcomes " + u_one_htgr_outcome + " and " + u_one_sfr_outcome + "\n");
							}
						} else if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==1) {
							u_one_htgr_outcome = 3;
							for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
								double[] temp_double = new double[u_two_strategy.length];
								double chance = 0;
								double obj_function = 0;
								for (u_stage_two=0; u_stage_two<u_two_strategy.length; u_stage_two++) {
									for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_sfr_capcost; u_two_htgr_outcome++) {
										u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome];
										chance = htgr_capcost_probability[u_two_htgr_outcome];
										obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome]);
										temp_double[u_stage_two] += chance*obj_function;
									}
								}
								//int the_max = getIndexOfMax(temp_double);
								u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = u_two_strategy[getIndexOfMax(temp_double)];
								//int the_hedge = u_two_strategy[getIndexOfMax(temp_double)];
								//u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = the_hedge;
								//System.out.print("the max is " + the_max + " and the hedge is " + the_hedge + "\n");
								//System.out.print("the max is " + the_max + " and the hedge is " + the_hedge + " for g one " + g_stage_one + " disp cost " + dispcost_outcome + " u one " + u_stage_one + " htgr one and sfr one outcomes " + u_one_htgr_outcome + " and " + u_one_sfr_outcome + "\n");
							}
						} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==1) {
							for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
								for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
									u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two] = u_two_perfectinfo[u_stage_one][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome];
									//int the_hedge = u_two_perfectinfo[u_stage_one][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome];
									//System.out.print("the hedge is " + the_hedge + " for g one " + g_stage_one + " disp cost " + dispcost_outcome + " u one " + u_stage_one + " htgr one and sfr one outcomes " + u_one_htgr_outcome + " and " + u_one_sfr_outcome + "\n");
									//System.out.print("the hedge is " + u_two_perfectinfo[u_stage_one][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome] + "\n");
								}
							}
						}
					}
				}
			}
		}
		
		/* G's stage two hedging strategy
		 * knowing U's stage one play
		 * having made stage one play 
		 * knowing the eventual plays for U's stage two and three
		 */

		for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
			for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
				for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) {
					if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==0) {
						u_one_htgr_outcome = 3;
						u_one_sfr_outcome = 3;
						double[] temp_double = new double[g_two_strategy.length];
						double chance = 0;
						double obj_function = 0;
						for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
							u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
							for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_htgr_capcost; u_two_htgr_outcome++) {
								for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
									u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome];
									chance = htgr_capcost_probability[u_two_htgr_outcome]*sfr_capcost_probability[u_two_sfr_outcome];
									obj_function = get_g_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome]);
									temp_double[g_stage_two] += chance*obj_function;
								}
							}
						}
						g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome] = g_two_strategy[getIndexOfMax(temp_double)];
					} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==0) {
						u_one_sfr_outcome = 3;
						for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
							double[] temp_double = new double[g_two_strategy.length];
							double chance = 0;
							double obj_function = 0;
							for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
								u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
								for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
									u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome];
									chance = sfr_capcost_probability[u_two_sfr_outcome];
									obj_function = get_g_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome]);
									temp_double[g_stage_two] += chance*obj_function;
								}
							}
							g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome] = g_two_strategy[getIndexOfMax(temp_double)];
						}
					} else if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==1) {
						u_one_htgr_outcome = 3;
						for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
							double[] temp_double = new double[g_two_strategy.length];
							double chance = 0;
							double obj_function = 0;
							for (g_stage_two=0; g_stage_two<g_two_strategy.length; g_stage_two++) {
								u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
								for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_htgr_capcost; u_two_htgr_outcome++) {
									u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome];
									chance = htgr_capcost_probability[u_two_htgr_outcome];
									obj_function = get_g_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_one_sfr_outcome][u_two_htgr_outcome]);
									temp_double[g_stage_two] += chance*obj_function;
								}
							}
							g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome] = g_two_strategy[getIndexOfMax(temp_double)];
						}
					} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==1) {
						for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
							for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
								g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome] = g_two_perfectinfo[u_stage_one][g_stage_one][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome];
							}
						}
					}
				}
			}
		}
		
		/* U's stage one hedging strategy
		 * knowing G's stage one play
		 * all the eventual plays 
		 */
		
		for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
			for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) {
				double[] temp_double = new double[u_one_strategy.length];
				double chance = 0;
				double obj_function = 0;
				for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
					if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==0) {
						u_one_htgr_outcome = 3;
						u_one_sfr_outcome = 3;
						g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
						u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
						for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_htgr_capcost; u_two_htgr_outcome++) {
							for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome];
								chance = htgr_capcost_probability[u_two_htgr_outcome]*sfr_capcost_probability[u_two_sfr_outcome];
								obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome]);
								temp_double[u_stage_one] += chance*obj_function;
							}
						}
					} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==0) {
						u_one_sfr_outcome = 3;
						for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
							g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
							u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
							for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome];
								chance = htgr_capcost_probability[u_one_htgr_outcome]*sfr_capcost_probability[u_two_sfr_outcome];
								obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome]);
								temp_double[u_stage_one] += chance*obj_function;
							}
						}
					} else if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==1) {
						u_one_htgr_outcome = 3;
						for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
							g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
							u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
							for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_sfr_capcost; u_two_htgr_outcome++) {
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome];
								chance = htgr_capcost_probability[u_two_htgr_outcome]*sfr_capcost_probability[u_one_sfr_outcome];
								obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome]);
								temp_double[u_stage_one] += chance*obj_function;
							}
						}
					} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==1) {
						for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
							for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
								g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
								u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome];
								chance = htgr_capcost_probability[u_one_htgr_outcome]*sfr_capcost_probability[u_one_sfr_outcome];
								obj_function = get_u_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome]);
								temp_double[u_stage_one] += chance*obj_function;
							}
						}
					}
				}
				u_one_hedge[g_stage_one][dispcost_outcome] = u_one_strategy[getIndexOfMax(temp_double)];
			}
		}
		
		/* G's stage one hedging strategy
		 * knowing eventual plays for given outcomes
		 */
		
		double[] temp_double = new double[g_one_strategy.length];
		double chance = 0;
		double obj_function = 0;
		for (g_stage_one=0; g_stage_one<g_one_strategy.length; g_stage_one++) {
			for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) {
				for (u_stage_one=0; u_stage_one<u_one_strategy.length; u_stage_one++) {
					if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==0) {
						u_one_htgr_outcome = 3;
						u_one_sfr_outcome = 3;
						g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
						u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
						for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_htgr_capcost; u_two_htgr_outcome++) {
							for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome];
								chance = dispcost_probability[g_stage_one][dispcost_outcome]*htgr_capcost_probability[u_two_htgr_outcome]*sfr_capcost_probability[u_two_sfr_outcome];
								obj_function = get_g_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome]);
								temp_double[g_stage_one] += chance*obj_function;
							}
						}
					} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==0) {
						u_one_sfr_outcome = 3;
						for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
							g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
							u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
							for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome];
								chance = dispcost_probability[g_stage_one][dispcost_outcome]*htgr_capcost_probability[u_one_htgr_outcome]*sfr_capcost_probability[u_two_sfr_outcome];
								obj_function = get_g_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome]);
								temp_double[g_stage_one] += chance*obj_function;
							}
						}
					} else if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==1) {
						u_one_htgr_outcome = 3;
						for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
							g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
							u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
							for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_sfr_capcost; u_two_htgr_outcome++) {
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome];
								chance = dispcost_probability[g_stage_one][dispcost_outcome]*htgr_capcost_probability[u_two_htgr_outcome]*sfr_capcost_probability[u_one_sfr_outcome];
								obj_function = get_g_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome]);
								temp_double[g_stage_one] += chance*obj_function;
							}
						}
					} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==1) {
						for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
							for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
								g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
								u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome];
								chance = htgr_capcost_probability[u_one_htgr_outcome]*sfr_capcost_probability[u_one_sfr_outcome];
								obj_function = get_g_weight_leafvalue(leaf_values[u_stage_one][u_stage_two][u_stage_three][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome]);
								temp_double[g_stage_one] += chance*obj_function;
							}
						}
					}
				}
			}
		}
		g_one_hedge = g_one_strategy[getIndexOfMax(temp_double)];
		
	}
	
	public int getIndexOfMax(double[] values) {
		// look for the maximum utility
		int maxIndex=0,i;
		double maxValue=values[0];
		for(i=0; i<values.length; i++) {
			if (values[i] > maxValue) {
				maxValue = values[i];
				maxIndex = i;
			}
		}
		return(maxIndex);
	}

	public void printHedgingStrategies() {
	
		int[] hedge_row = new int[10];
		int g_stage_one;
		int dispcost_outcome;
		int u_stage_one, u_one_htgr_outcome, u_one_sfr_outcome;
		int g_stage_two;
		int u_stage_two, u_two_htgr_outcome, u_two_sfr_outcome;
		int u_stage_three;
		
		try {

			String user_dir = System.getProperty("user.dir");
			File output_target = new File(user_dir+File.separatorChar+"HedgingStrategyResults.txt");

			if(output_target.exists()) output_target.delete();
			FileWriter output_filewriter = new FileWriter(output_target);
			PrintWriter output_writer = new PrintWriter(output_filewriter);
			
			output_writer.print("reprocessing_cost waste_disposal_cost reactor_build htgr_outcome sfr_outcome capital_subsidy reactor_build htgr_outcome sfr_outcome reactor_build");
			output_writer.print("\n");
			
			
			hedge_row[0] = g_one_hedge;
			g_stage_one = g_one_hedge;
			
			for (dispcost_outcome=0; dispcost_outcome<n_dispcost_outcome; dispcost_outcome++) {
				
				hedge_row[1] = dispcost_outcome;
				u_stage_one = u_one_hedge[g_stage_one][dispcost_outcome];
				hedge_row[2] = u_stage_one;
				
				if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==0) {
					
					u_one_htgr_outcome = 3;
					u_one_sfr_outcome = 3;
					hedge_row[3] = u_one_htgr_outcome;
					hedge_row[4] = u_one_sfr_outcome;
					g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
					hedge_row[5] = g_stage_two;
					u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
					hedge_row[6] = u_stage_two;
					
					if (u_two_gainedinfo[u_stage_one][u_stage_two][1]==0 && u_two_gainedinfo[u_stage_one][u_stage_two][2]==0) {
						u_two_htgr_outcome = 3;
						hedge_row[7] = u_two_htgr_outcome;
						u_two_sfr_outcome = 3;
						hedge_row[8] = u_two_sfr_outcome;
						/* let the htgr and sfr capital cost = 0 ; the resultant leaf value has no tie with the outcome */
						u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][0][0];
						hedge_row[9] = u_stage_three;
						output_writer.print(hedge_row[0] + " " + hedge_row[1] + " " + hedge_row[2] + " " + hedge_row[3] + " " + hedge_row[4] + " " + hedge_row[5] + " " + hedge_row[6] + " " + hedge_row[7] + " " + hedge_row[8] + " " + hedge_row[9]);
						output_writer.print("\n");
					} else if (u_two_gainedinfo[u_stage_one][u_stage_two][1]==1 && u_two_gainedinfo[u_stage_one][u_stage_two][2]==0) {
						u_two_sfr_outcome = 3;
						for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_htgr_capcost; u_two_htgr_outcome++) {
							hedge_row[7] = u_two_htgr_outcome;
							hedge_row[8] = u_two_sfr_outcome;
							u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][0];
							hedge_row[9] = u_stage_three;
							output_writer.print(hedge_row[0] + " " + hedge_row[1] + " " + hedge_row[2] + " " + hedge_row[3] + " " + hedge_row[4] + " " + hedge_row[5] + " " + hedge_row[6] + " " + hedge_row[7] + " " + hedge_row[8] + " " + hedge_row[9]);
							output_writer.print("\n");
						}
					} else if (u_two_gainedinfo[u_stage_one][u_stage_two][1]==0 && u_two_gainedinfo[u_stage_one][u_stage_two][2]==1) {
						u_two_htgr_outcome = 3;
						for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
							hedge_row[7] = u_two_htgr_outcome;
							hedge_row[8] = u_two_sfr_outcome;
							u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][0][u_two_sfr_outcome];
							hedge_row[9] = u_stage_three;
							output_writer.print(hedge_row[0] + " " + hedge_row[1] + " " + hedge_row[2] + " " + hedge_row[3] + " " + hedge_row[4] + " " + hedge_row[5] + " " + hedge_row[6] + " " + hedge_row[7] + " " + hedge_row[8] + " " + hedge_row[9]);
							output_writer.print("\n");
						}
					} else if (u_two_gainedinfo[u_stage_one][u_stage_two][1]==1 && u_two_gainedinfo[u_stage_one][u_stage_two][2]==1) {
						for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_htgr_capcost; u_two_htgr_outcome++) {
							for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
								hedge_row[7] = u_two_htgr_outcome;
								hedge_row[8] = u_two_sfr_outcome;
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_two_sfr_outcome];
								hedge_row[9] = u_stage_three;
								output_writer.print(hedge_row[0] + " " + hedge_row[1] + " " + hedge_row[2] + " " + hedge_row[3] + " " + hedge_row[4] + " " + hedge_row[5] + " " + hedge_row[6] + " " + hedge_row[7] + " " + hedge_row[8] + " " + hedge_row[9]);
								output_writer.print("\n");
							}
						}
					}
		
				} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==0) {
					
					u_one_sfr_outcome = 3;
					for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
						hedge_row[3] = u_one_htgr_outcome;
						hedge_row[4] = u_one_sfr_outcome;
						g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
						hedge_row[5] = g_stage_two;
						u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
						hedge_row[6] = u_stage_two;
						if (u_two_gainedinfo[u_stage_one][u_stage_two][2] == 0) {
							hedge_row[7] = u_one_htgr_outcome;
							hedge_row[8] = u_one_sfr_outcome;
							u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][0];
							hedge_row[9] = u_stage_three;
							output_writer.print(hedge_row[0] + " " + hedge_row[1] + " " + hedge_row[2] + " " + hedge_row[3] + " " + hedge_row[4] + " " + hedge_row[5] + " " + hedge_row[6] + " " + hedge_row[7] + " " + hedge_row[8] + " " + hedge_row[9]);
							output_writer.print("\n");
						} else if (u_two_gainedinfo[u_stage_one][u_stage_two][2] == 1) {
							for (u_two_sfr_outcome=0; u_two_sfr_outcome<n_sfr_capcost; u_two_sfr_outcome++) {
								hedge_row[7] = u_one_htgr_outcome;
								hedge_row[8] = u_two_sfr_outcome;
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_two_sfr_outcome];
								hedge_row[9] = u_stage_three;
								output_writer.print(hedge_row[0] + " " + hedge_row[1] + " " + hedge_row[2] + " " + hedge_row[3] + " " + hedge_row[4] + " " + hedge_row[5] + " " + hedge_row[6] + " " + hedge_row[7] + " " + hedge_row[8] + " " + hedge_row[9]);
								output_writer.print("\n");
							}
						}
					}
					
				} else if (u_one_gainedinfo[u_stage_one][1]==0 && u_one_gainedinfo[u_stage_one][2]==1) {
					
					u_one_htgr_outcome = 3;
					for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
						hedge_row[3] = u_one_htgr_outcome;
						hedge_row[4] = u_one_sfr_outcome;
						g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
						hedge_row[5] = g_stage_two;
						u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
						hedge_row[6] = u_stage_two;
						if (u_two_gainedinfo[u_stage_one][u_stage_two][1] == 0) {
							hedge_row[7] = u_one_htgr_outcome;
							hedge_row[8] = u_one_sfr_outcome;
							u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][0][u_one_sfr_outcome];
							hedge_row[9] = u_stage_three;
							output_writer.print(hedge_row[0] + " " + hedge_row[1] + " " + hedge_row[2] + " " + hedge_row[3] + " " + hedge_row[4] + " " + hedge_row[5] + " " + hedge_row[6] + " " + hedge_row[7] + " " + hedge_row[8] + " " + hedge_row[9]);
							output_writer.print("\n");
						} else if (u_two_gainedinfo[u_stage_one][u_stage_two][1] == 1) {
							for (u_two_htgr_outcome=0; u_two_htgr_outcome<n_htgr_capcost; u_two_htgr_outcome++) {
								hedge_row[7] = u_two_htgr_outcome;
								hedge_row[8] = u_one_sfr_outcome;
								u_stage_three = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_two_htgr_outcome][u_one_sfr_outcome];
								hedge_row[9] = u_stage_three;
								output_writer.print(hedge_row[0] + " " + hedge_row[1] + " " + hedge_row[2] + " " + hedge_row[3] + " " + hedge_row[4] + " " + hedge_row[5] + " " + hedge_row[6] + " " + hedge_row[7] + " " + hedge_row[8] + " " + hedge_row[9]);
								output_writer.print("\n");
							}
						}
					}
					
				} else if (u_one_gainedinfo[u_stage_one][1]==1 && u_one_gainedinfo[u_stage_one][2]==1) { /* no uncertainties left */
					
					for (u_one_htgr_outcome=0; u_one_htgr_outcome<n_htgr_capcost; u_one_htgr_outcome++) {
						for (u_one_sfr_outcome=0; u_one_sfr_outcome<n_sfr_capcost; u_one_sfr_outcome++) {
							hedge_row[3] = u_one_htgr_outcome;
							hedge_row[4] = u_one_sfr_outcome;
							g_stage_two = g_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome];
							hedge_row[5] = g_stage_two;
							u_stage_two = u_two_hedge[g_stage_one][dispcost_outcome][u_stage_one][u_one_htgr_outcome][u_one_sfr_outcome][g_stage_two];
							hedge_row[6] = u_stage_two;
							hedge_row[7] = u_one_htgr_outcome;
							hedge_row[8] = u_one_sfr_outcome;
							hedge_row[9] = u_three_perfectinfo[u_stage_one][u_stage_two][g_stage_one][g_stage_two][dispcost_outcome][u_one_htgr_outcome][u_one_sfr_outcome];
							output_writer.print(hedge_row[0] + " " + hedge_row[1] + " " + hedge_row[2] + " " + hedge_row[3] + " " + hedge_row[4] + " " + hedge_row[5] + " " + hedge_row[6] + " " + hedge_row[7] + " " + hedge_row[8] + " " + hedge_row[9]);
							output_writer.print("\n");
						}
					}
					
				}
				
			}

			output_writer.close();

		} catch (IOException e) {
			System.out.print("Error writing Hedging Strategy results");
		}
		System.out.print("Finished printing hedging strategy results");
	}
	
	public void printPerfectInformationStrategies() {
		
		int[] hedge_row = new int[10];
		int g_stage_one;
		int dispcost_outcome;
		int u_stage_one, u_one_htgr_outcome, u_one_sfr_outcome;
		int g_stage_two;
		int u_stage_two, u_two_htgr_outcome, u_two_sfr_outcome;
		int u_stage_three;
		int i, j, k;

		try {

			String user_dir = System.getProperty("user.dir");
			File output_target = new File(user_dir+File.separatorChar+"PerfectInformationStrategyResults.txt");

			if(output_target.exists()) output_target.delete();
			FileWriter output_filewriter = new FileWriter(output_target);
			PrintWriter output_writer = new PrintWriter(output_filewriter);

			output_writer.print("reprocessing_cost waste_disposal_cost reactor_build htgr_outcome sfr_outcome capital_subsidy reactor_build htgr_outcome sfr_outcome reactor_build");
			output_writer.print("\n");
			
			//for (i=0; i<)

		} catch (IOException e) {
			System.out.print("Error writing Hedging Strategy results");
		}

	}

	
}