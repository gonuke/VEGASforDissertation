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

	/* decision criteria weighting */
	static double[] u_weight = DMInputs.getUWeighting(); // coe; decay heat; proliferation resistance
	static double[] g_weight = DMInputs.getGWeighting(); // coe; decay heat; proliferation resistance
	static double[] uw = DMInputs.getOUWeight(); // coe; decay heat; proliferation resistance
	static double[] gw = DMInputs.getOGWeight(); // coe; decay heat; proliferation resistance
	
	
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
	static double[][][][][][][][] SFRDat;
	
	static int[][][][][][][] u_three_pi;
	static int[][][][][][] u_two_pi;
	static int[][][][][] g_two_pi;
	static int[][][][] u_one_pi;
	static int[][][] g_one_pi;

	static int[][][][][][] u_two_hedge;
	static int[][][][][] g_two_hedge;
	static int[][] u_one_hedge;
	static int g_one_hedge;
	
	static double[][][][][][][] u_two_exp;
	static double[][][][][] g_two_exp;
	static double[][][][] u_one_exp;
	static double[][] g_one_exp;
	
	static double[][][][][] u_two_sfrexp;
	static double[][][] u_one_sfrexp;
	
	static boolean heat_map = false;
	
	public DecisionMaker() {

	}
	
	public static void main(String args[]) {

		if (!heat_map) {
			DecisionMaker decide = new DecisionMaker();
			decide.makeDecisions();	
		}
		
		if (heat_map) { 
			double tot = 1.;
			double quan = (double) 1 / (double) 10;
			int quantiles = 10;
			int sims=0, sim_no=1;
			
			for (int i=0; i<quantiles; i++) for (int j=i; j<quantiles; j++) sims++;

			try {
				
				double[][] gtwomap = new double[quantiles][quantiles];
				double[][] utwomap = new double[quantiles][quantiles];
				double[][] therm = new double[quantiles][quantiles];
				
				String user_dir = System.getProperty("user.dir");

				/* G's stage one hedging strategies */
				double[][] gmap = new double[quantiles][quantiles]; int g_o = 0;
				
				File gone_target = new File(user_dir+File.separatorChar+"GOneDMHedge.txt");
				if(gone_target.exists()) gone_target.delete();
				FileWriter gone_filewriter = new FileWriter(gone_target);
				PrintWriter gone_writer = new PrintWriter(gone_filewriter);
				u_weight = uw;
				
				for (int i=0; i<quantiles; i++) {
					for (int j=0; j<quantiles-i; j++) {
						
						System.out.print("Scoping G's Stage One hedging strategies over G's criteria range .. simulation " + sim_no + " of " + sims + ".\n"); sim_no++;

						g_weight[1] = i*quan;
						g_weight[2] = j*quan;
						g_weight[0] = tot - g_weight[1] - g_weight[2];
						DecisionMaker decide = new DecisionMaker();
						decide.rangeOfDecisions();	
						
						g_o = g_one_hedge;
						gmap[i][j] = g_o;
						
					}
				}
				
				for (int i=quantiles-1; i>-1; i--) {
					for (int j=0; j<quantiles; j++) {
						gone_writer.print(gmap[j][i] + " ");
					}
					gone_writer.print("\n");
				}
				gone_writer.close();
				
				/* G's expected metric values for each hedge */
				File goneexp_target = new File(user_dir+File.separatorChar+"GOneExp.txt");
				if(goneexp_target.exists()) goneexp_target.delete();
				FileWriter goneexp_filewriter = new FileWriter(goneexp_target);
				PrintWriter goneexp_writer = new PrintWriter(goneexp_filewriter);
				
				goneexp_writer.print("g_one coe heat pr" + "\n");
				for (int i=0; i<g_one.length; i++) {
					goneexp_writer.print(i);
					for (int j=0; j<g_weight.length; j++) {
						// g one; disp; u one; metric
						goneexp_writer.print(" " + g_one_exp[i][j]);
					}
					goneexp_writer.print("\n");
				}
				goneexp_writer.close();
				
				/* If altering U's criteria */
				File gone_u_target = new File(user_dir+File.separatorChar+"GOneDMHedgeUWeight.txt");
				if(gone_u_target.exists()) gone_u_target.delete();
				FileWriter gone_u_filewriter = new FileWriter(gone_u_target);
				PrintWriter gone_u_writer = new PrintWriter(gone_u_filewriter);
				g_weight = gw;
				
				sim_no=1; 
				for (int i=0; i<quantiles; i++) {
					for (int j=0; j<quantiles-i; j++) {
						
						System.out.print("Scoping G's Stage One hedging strategy over U's criteria range .. simulation " + sim_no + " of " + sims + ".\n"); sim_no++;

						u_weight[1] = i*quan;
						u_weight[2] = j*quan;
						u_weight[0] = tot - u_weight[1] - u_weight[2];
						DecisionMaker decide = new DecisionMaker();
						decide.rangeOfDecisions();	
						
						g_o = g_one_hedge;
						gmap[i][j] = g_o;
						
					}
				}
				
				for (int i=quantiles-1; i>-1; i--) {
					for (int j=0; j<quantiles; j++) {
						gone_u_writer.print(gmap[j][i] + " ");
					}
					gone_u_writer.print("\n");
				}
				gone_u_writer.close();
				
				/* U's stage one hedging strategies */
				
				double[][] umap = new double[quantiles][quantiles];
				int disp = 2; /* Take a moderate waste disposal cost */
				g_o = 1; /* Take G's most common stage one hedge */
				g_weight = gw;
				
				sim_no=1;
				for (int i=0; i<quantiles; i++) {
					for (int j=0; j<quantiles-i; j++) {
						
						System.out.print("Scoping U's Stage One hedging strategy over U's criteria range .. simulation " + sim_no + " of " + sims + ".\n"); sim_no++;
						
						u_weight[1] = i*quan;
						u_weight[2] = j*quan;
						u_weight[0] = tot - u_weight[1] - u_weight[2];
						DecisionMaker decide = new DecisionMaker();
						decide.rangeOfDecisions();	

						int u_o = u_one_hedge[g_o][disp];
						umap[i][j] = u_o;
						
					}
				}
				
				File uone_target = new File(user_dir+File.separatorChar+"UOneDMHedge.txt");
				if(uone_target.exists()) uone_target.delete();
				FileWriter uone_filewriter = new FileWriter(uone_target);
				PrintWriter uone_writer = new PrintWriter(uone_filewriter);
				
				for (int i=quantiles-1; i>-1; i--) {
					for (int j=0; j<quantiles; j++) {
						uone_writer.print(umap[j][i] + " ");
					}
					uone_writer.print("\n");
				}
				uone_writer.close();
				
//				File output_target = new File(user_dir+File.separatorChar+"HeatMapResults.txt");
//				if(output_target.exists()) output_target.delete();
//				FileWriter output_filewriter = new FileWriter(output_target);
//				PrintWriter output_writer = new PrintWriter(output_filewriter);
//				
//				for (int i=quantiles-1; i>-1; i--) {
//					for (int j=0; j<quantiles; j++) {
//						output_writer.print(umap[j][i] + " ");
//					}
//					output_writer.print("\n");
//				}
//				output_writer.close();
				
				
				sim_no=1;
				/* write gtwo and utwo maps
				 * G's capital subsidy and U's response 
				 * given G's optimal stage one hedge, most likely disposal cost outcome
				 * and U hedging with HTGRs in stage one
				 */
				for (int i=0; i<quantiles; i++) {
					for (int j=0; j<quantiles-i; j++) {
						
						System.out.print("Scoping Stage Two heat map range .. simulation " + sim_no + " of " + sims + ".\n"); sim_no++;
						
						u_weight[1] = i*quan;
						u_weight[2] = j*quan;
						u_weight[0] = tot - u_weight[1] - u_weight[2];
						DecisionMaker decide = new DecisionMaker();
						decide.rangeOfDecisions();	

						g_o = g_one_hedge;
						disp=2;
						int u_o=1;
						/* change the htgr capital cost only since U's stage one hedge fixed with HTGR */
						int htgr=0;
						int sfr=3;
						
						int g_tw = g_two_hedge[g_o][disp][u_o][htgr][sfr];
						gtwomap[i][j] = g_tw;
						
						int u_tw = u_two_hedge[g_o][disp][u_o][htgr][sfr][g_tw];
						utwomap[i][j] = u_tw;
						
						therm[i][j] = u_one_sfrexp[g_one_hedge][2][u_one_hedge[g_one_hedge][2]]*100;
						
					}
				}
				
				
				File therm_target = new File(user_dir+File.separatorChar+"ThermalProbability.txt");
				File gtwo_target = new File(user_dir+File.separatorChar+"GTwo.txt");
				File utwo_target = new File(user_dir+File.separatorChar+"UTwo.txt");
				
				if(therm_target.exists()) therm_target.delete();
				FileWriter therm_filewriter = new FileWriter(therm_target);
				PrintWriter therm_writer = new PrintWriter(therm_filewriter);
				
				if(gtwo_target.exists()) gtwo_target.delete();
				FileWriter gtwo_filewriter = new FileWriter(gtwo_target);
				PrintWriter gtwo_writer = new PrintWriter(gtwo_filewriter);
				
				if(utwo_target.exists()) utwo_target.delete();
				FileWriter utwo_filewriter = new FileWriter(utwo_target);
				PrintWriter utwo_writer = new PrintWriter(utwo_filewriter);

				for (int i=quantiles-1; i>-1; i--) {
					for (int j=0; j<quantiles; j++) {
						therm_writer.print(therm[j][i] + " ");
						gtwo_writer.print(gtwomap[j][i] + " ");
						utwo_writer.print(utwomap[j][i] + " ");
					}
					therm_writer.print("\n");
					gtwo_writer.print("\n");
					utwo_writer.print("\n");
				}
				therm_writer.close();
				gtwo_writer.close();
				utwo_writer.close();
				
				File exp_target = new File(user_dir+File.separatorChar+"ExpValue.txt");
				if(exp_target.exists()) exp_target.delete();
				FileWriter exp_filewriter = new FileWriter(exp_target);
				PrintWriter exp_writer = new PrintWriter(exp_filewriter);
				
				File twoexp_target = new File(user_dir+File.separatorChar+"TwoExpValue.txt");
				if(twoexp_target.exists()) twoexp_target.delete();
				FileWriter twoexp_filewriter = new FileWriter(twoexp_target);
				PrintWriter twoexp_writer = new PrintWriter(twoexp_filewriter);
				
				// u_two_exp = new double[disp_cost.length][htgr_cost.length+1][sfr_cost.length+1][g_two.length][u_two.length][u_weight.length];
				
				disp = 2;
				int htgr = 1;
				int sfr = 3;
				int u_o = 1; // assume the first stage hedge is HTGR
				
				exp_writer.print("u_one coe heat pr" + "\n");
				twoexp_writer.print("u_one coe heat pr" + "\n");
				for (int i=0; i<u_one.length; i++) {
					exp_writer.print(i);
					twoexp_writer.print(i);
					for (int j=0; j<u_weight.length; j++) {
						// g one; disp; u one; metric
						exp_writer.print(" " + u_one_exp[disp][htgr][i][j]);
						g_o = g_one_hedge;
						int g_tw = g_two_hedge[g_o][disp][u_o][htgr][sfr];
						twoexp_writer.print(" " + u_two_exp[disp][htgr][sfr][g_o][g_tw][i][j]);
					}
					exp_writer.print("\n");
					twoexp_writer.print("\n");
				}
				exp_writer.close();
				twoexp_writer.close();
				
			} catch (IOException e) {
				System.out.print("Error writing heat map results");
			}
			System.out.print("Finishing printing heat map results");

		}

		
	}	
	
	
	public void rangeOfDecisions() {
		dimensionArrays();
		/* get the data, then normalize it */
		loadData();
		normalizeData();
		/* get sfr penetration data */
		loadSFRPenData();
		/* get the perfect info strategies based on that info */
		getPerfectInformationStrategies();
		/* get the hedging strategies */
		getHedgingStrategies();
	}
	
	public void makeDecisions() {
		dimensionArrays();
		/* get the data, then normalize it */
		loadData();
		normalizeData();
		/* get the perfect info strategies based on that info */
		getPerfectInformationStrategies();
		/* get the hedging strategies */
		getHedgingStrategies();
		/* print the hedging strategy results */
		printHedgingStrategies();
		/* print the perfect information strategies */
		printPerfectInformationStrategies();
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
		SFRDat = new double [u_one.length][u_one.length][u_one.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		
		u_three_pi = new int[u_one.length][u_two.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		u_two_pi = new int[u_one.length][g_one.length][g_two.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		g_two_pi = new int[u_one.length][g_one.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		u_one_pi = new int[g_one.length][disp_cost.length][htgr_cost.length][sfr_cost.length];
		g_one_pi = new int[disp_cost.length][htgr_cost.length][sfr_cost.length];
		
		/* if the htgr or sfr cost is (rx_cost.length+1) then it means the previous stage didn't yield information about the cost */
		u_two_hedge = new int[g_one.length][disp_cost.length][u_one.length][htgr_cost.length+1][sfr_cost.length+1][g_two.length];
		g_two_hedge = new int[g_one.length][disp_cost.length][u_one.length][htgr_cost.length+1][sfr_cost.length+1];
		u_one_hedge = new int[g_one.length][disp_cost.length];
		
		u_two_exp = new double[disp_cost.length][htgr_cost.length+1][sfr_cost.length+1][g_one.length][g_two.length][u_two.length][u_weight.length];
		g_two_exp = new double[disp_cost.length][htgr_cost.length+1][sfr_cost.length+1][g_two.length][g_weight.length];
		u_one_exp = new double[g_one.length][disp_cost.length][u_one.length][u_weight.length];
		g_one_exp = new double[g_one.length][g_weight.length];
		
		u_two_sfrexp = new double[disp_cost.length][htgr_cost.length+1][sfr_cost.length+1][g_two.length][u_two.length];
		u_one_sfrexp = new double[g_one.length][disp_cost.length][u_one.length];
		
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

	public void loadSFRPenData() {

		String inputFile = "ResultsGen.txt";
		String file_path = System.getProperty("user.dir") + File.separatorChar + inputFile;
		File data = new File(file_path);
		BufferedReader buf;
		String current_line = " anything ";
		StringTokenizer st;
		int i,j,k;
		
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
				SFRDat[ints[0]][ints[1]][ints[2]][ints[3]][ints[4]][ints[5]][ints[6]][ints[7]] = Double.valueOf( st.nextToken() ).doubleValue();
				SFRDat[ints[0]][ints[1]][ints[2]][ints[3]][ints[4]][ints[5]][ints[6]][ints[7]] /= 644.15;
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
		if (!heat_map) System.out.print("Finished printing normalized data" + "\n");
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
										for (int i=0; i<u_weight.length; i++) u_two_exp[disp][htgr+1][sfr+1][g_o][g_tw][u_tw][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
										u_two_sfrexp[disp][htgr+1][sfr+1][g_tw][u_tw] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr];
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
										for (int i=0; i<u_weight.length; i++) u_two_exp[disp][htgr][sfr+1][g_o][g_tw][u_tw][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
										u_two_sfrexp[disp][htgr][sfr+1][g_tw][u_tw] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr];
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
										for (int i=0; i<u_weight.length; i++) u_two_exp[disp][htgr+1][sfr][g_o][g_tw][u_tw][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
										u_two_sfrexp[disp][htgr+1][sfr][g_tw][u_tw] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr];
									}
								}
								u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr][g_tw] = u_two[getIndexOfMax(temp_double)];
							}
						}
						
						if (one_info[u_o][1]==1 && one_info[u_o][2]==1) {
							chance = 1;
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_two_hedge[g_o][disp][u_o][htgr][sfr][g_tw] = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr]; // no hedge if information already known
									u_tw = u_two_pi[u_o][g_o][g_tw][disp][htgr][sfr];
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									for (int i=0; i<u_weight.length; i++) u_two_exp[disp][htgr][sfr][g_o][g_tw][u_tw][i] = NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
									u_two_sfrexp[disp][htgr][sfr][g_tw][u_tw] = SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr];
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

					if (one_info[u_o][1]==0 && one_info[u_o][2]==0) { // no information; hedge against all costs
						double[] temp_double = new double[g_two.length];
						for (g_tw=0; g_tw<g_two.length; g_tw++) {
							u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr]*sfr_prob[sfr];
									val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[g_tw] += chance*val;
									for (int i=0; i<g_weight.length; i++) g_two_exp[disp][htgr+1][sfr+1][g_tw][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
								}
							}
						}
						g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length] = g_two[getIndexOfMax(temp_double)];
						g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length];
						u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
					}

					if (one_info[u_o][1]==1 && one_info[u_o][2]==0) { // known htgr cost; hedge against unknown sfr cost
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							double[] temp_double = new double[g_two.length];
							for (g_tw=0; g_tw<g_two.length; g_tw++) {
								u_tw = u_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length][g_tw];
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = sfr_prob[sfr];
									val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[g_tw] += chance*val;
									for (int i=0; i<g_weight.length; i++) g_two_exp[disp][htgr][sfr+1][g_tw][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
								}
							}
							g_two_hedge[g_o][disp][u_o][htgr][sfr_cost.length] = g_two[getIndexOfMax(temp_double)];
							g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length];
							u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
						}
					}
					
					if (one_info[u_o][1]==0 && one_info[u_o][2]==1) { // known sfr cost; hedge against unknown htgr cost
						for (sfr=0; sfr<sfr_cost.length; sfr++) {
							double[] temp_double = new double[g_two.length];
							for (g_tw=0; g_tw<g_two.length; g_tw++) {
								u_tw = u_two_hedge[g_o][disp][u_o][sfr][htgr_cost.length][g_tw];
								for (htgr=0; htgr<htgr_cost.length; htgr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr];
									val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[g_tw] += chance*val;
									for (int i=0; i<g_weight.length; i++) g_two_exp[disp][htgr+1][sfr][g_tw][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
								}
							}
							g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr] = g_two[getIndexOfMax(temp_double)]; 
							g_tw = g_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length];
							u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
						}
					}
					
					if (one_info[u_o][1]==1 && one_info[u_o][2]==1) { /* all uncertainties resolved; g two hedge is just the perfect information strategy then */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								g_tw = g_two_pi[u_o][g_o][disp][htgr][sfr];
								g_two_hedge[g_o][disp][u_o][htgr][sfr] = g_tw;
								u_tw = u_two_hedge[g_o][disp][u_o][htgr_cost.length][sfr_cost.length][g_tw];
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								for (int i=0; i<g_weight.length; i++) g_two_exp[disp][htgr+1][sfr+1][g_tw][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
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
						if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==0) { /* two_info {0,0,0} */ /* htgr and sfr costs assumed 0 -- not result on NormDat since not built */
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][0];
							temp_double[u_o] += getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]); 
							for (int i=0; i<u_weight.length; i++) u_one_exp[g_o][disp][u_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0][i]; 
							u_one_sfrexp[g_o][disp][u_o] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]; 
						}
						if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][0];
								chance = htgr_prob[htgr];
								val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0]);
								temp_double[u_o] += chance*val;
								for (int i=0; i<u_weight.length; i++) u_one_exp[g_o][disp][u_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0][i]; 
								u_one_sfrexp[g_o][disp][u_o] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]; 
							}
						}
						if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][sfr];
								chance = sfr_prob[sfr];
								val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr]);
								temp_double[u_o] += chance*val;
								for (int i=0; i<u_weight.length; i++) u_one_exp[g_o][disp][u_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr][i];
								u_one_sfrexp[g_o][disp][u_o] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]; 
							}
						}
						if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==1) { /* two_info {0,1,1} */
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr]*sfr_prob[sfr];
									val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[u_o] += chance*val;
									for (int i=0; i<u_weight.length; i++) u_one_exp[g_o][disp][u_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
									u_one_sfrexp[g_o][disp][u_o] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]; 
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
								for (int i=0; i<u_weight.length; i++) u_one_exp[g_o][disp][u_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0][i];
								u_one_sfrexp[g_o][disp][u_o] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]; 
							}
							if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
								for (sfr=0; sfr<sfr_cost.length; sfr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr]*sfr_prob[sfr];
									val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[u_o] += chance*val;
									for (int i=0; i<u_weight.length; i++) u_one_exp[g_o][disp][u_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
									u_one_sfrexp[g_o][disp][u_o] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]; 
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
								for (int i=0; i<u_weight.length; i++) u_one_exp[g_o][disp][u_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr][i];
								u_one_sfrexp[g_o][disp][u_o] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]; 
							}
							if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
								for (htgr=0; htgr<htgr_cost.length; htgr++) {
									u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
									chance = htgr_prob[htgr]*sfr_prob[sfr];
									val = getVal(u_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
									temp_double[u_o] += chance*val;
									for (int i=0; i<u_weight.length; i++) u_one_exp[g_o][disp][u_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
									u_one_sfrexp[g_o][disp][u_o] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]; 
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
								for (int i=0; i<u_weight.length; i++) u_one_exp[g_o][disp][u_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
								u_one_sfrexp[g_o][disp][u_o] += chance*SFRDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0]; 
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
						for (int i=0; i<g_weight.length; i++) g_one_exp[g_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][0][i];
					}
					if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][0];
							chance = disp_prob[g_o][disp]*htgr_prob[htgr];
							val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0]);
							temp_double[g_o] += chance*val;
							for (int i=0; i<g_weight.length; i++) g_one_exp[g_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0][i];
						}
					}
					if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
						for (sfr=0; sfr<sfr_cost.length; sfr++) {
							u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][0][sfr];
							chance = disp_prob[g_o][disp]*sfr_prob[sfr];
							val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr]);
							temp_double[g_o] += chance*val;
							for (int i=0; i<g_weight.length; i++) g_one_exp[g_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr][i];
						}
					}
					if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==1) { /* two_info {0,1,1} */
						for (htgr=0; htgr<htgr_cost.length; htgr++) {
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								chance = disp_prob[g_o][disp]*htgr_prob[htgr]*sfr_prob[sfr];
								val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
								temp_double[g_o] += chance*val;
								for (int i=0; i<g_weight.length; i++) g_one_exp[g_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
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
							for (int i=0; i<g_weight.length; i++) g_one_exp[g_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][0][i];
						}
						if (two_info[u_o][u_tw][1]==0 && two_info[u_o][u_tw][2]==1) { /* two_info {0,0,1} */
							for (sfr=0; sfr<sfr_cost.length; sfr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								chance = disp_prob[g_o][disp]*htgr_prob[htgr]*sfr_prob[sfr];
								val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
								temp_double[g_o] += chance*val;
								for (int i=0; i<g_weight.length; i++) g_one_exp[g_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
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
							for (int i=0; i<g_weight.length; i++) g_one_exp[g_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][0][sfr][i];
						}
						if (two_info[u_o][u_tw][1]==1 && two_info[u_o][u_tw][2]==0) { /* two_info {0,1,0} */
							for (htgr=0; htgr<htgr_cost.length; htgr++) {
								u_th = u_three_pi[u_o][u_tw][g_o][g_tw][disp][htgr][sfr];
								chance = disp_prob[g_o][disp]*htgr_prob[htgr]*sfr_prob[sfr];
								val = getVal(g_weight,NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr]);
								temp_double[g_o] += chance*val;
								for (int i=0; i<g_weight.length; i++) g_one_exp[g_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
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
							for (int i=0; i<g_weight.length; i++) g_one_exp[g_o][i] += chance*NormDat[u_o][u_tw][u_th][g_o][g_tw][disp][htgr][sfr][i];
						}
					}
				}
			
			}
		}
		g_one_hedge = g_one[getIndexOfMax(temp_double)];

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
	
	public double[] getExpVal(double chance, double[] vals) {
		double[] exp_val = new double[vals.length];
		for (int i=0; i<vals.length; i++) exp_val[i] = chance*vals[i];
		return(exp_val);
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
