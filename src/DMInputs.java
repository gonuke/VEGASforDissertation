
public class DMInputs {

	static double[] GCriterionWeights = {(double) 0.333, (double) 0.333, (double) .333};
	static double[] UCriterionWeights = {(double) .9, (double) 0.1, (double) 0.};
	
//	static double[] ReprocessingCostOutcomes = {950,1050,1300};
//	static double[][] DisposalCostOutcomes = {{600,3000},{800,4000},{1000,5000}};

	//static double[] ChosenReprocessingCost = {903,1120,1339};
	static double[] ChosenReprocessingCost = {2387, 2171, 1964};
	static double[][] DisposalCostOutcomes = {{144,933},{602,4133},{801,6359},{987,8795}};
	/* disposal cost needs to be anti-correlated with reprocessing costs -- inverse in amount allowed for R&D */
	static double[][] DisposalCostProbabilities = {
		{(double) 0, (double) .25, (double) .5, (double) .25},
		{(double) .05, (double) .3, (double) .65, (double) .1},
		{(double) .1, (double) .5, (double) .4, (double) 0.}
	};
//	static double[][] DisposalCostProbabilities = {
//		{(double) 0, (double) .25, (double) .5, (double) .25},
//		{(double) 0, (double) .25, (double) .5, (double) .25},
//		{(double) 0, (double) .25, (double) .5, (double) .25}
//	};
	
	static double LWRCapitalCost = 4100; // foak 3500
	//static double[] HTGRCapitalCost = {1800, 2400, 3000};
	//static double[] HTGRCapitalCost = {2700, 3000, 3500}; //foak 4500
	//static double[] HTGRCapitalCost = {3000, 4000, 4500};
	static double[] HTGRCapitalCost = {3580, 5370, 8950};
	static double[] htgr_capcost_probability = {(double) .25, (double) .5, (double) .25};
	//static double[] htgr_capcost_probability = {(double) (1/3), (double) (1/3), (double) (1/3)};
	//static double[] SFRCapitalCost = {1500, 1750, 2700};
	//static double[] SFRCapitalCost = {2700, 3000, 4000}; // foak 4200};
	static double[] SFRCapitalCost = {3300, 4155, 5900};
	static double[] sfr_capcost_probability = {(double) .25, (double) .5, (double) .25};
	
	//static int[][][] CapitalSubsidy = {
//		{{0,0,0}},
//		{{0,1,0}},
//		{{0,0,1}},
//		{{0,1,0},{0,0,1},{0,1,1}}
//	};
	static int[][] CapitalSubsidy = {{1,0,0},{0,1,0},{0,0,1},{0,1,1}};
	//static int[][] CapitalSubsidy = {{0,1,0},{0,0,1},{0,1,1}};
	static double SubsidyAmount = 277; /* $/kWe for SFR */
	static int CapitalSubsidyYear = 2045;
	
	static double[][] decay_heat = { // [reactor type][actinides, fission products]
		{1.849E+03, 1.322E+03},
		{4.145E+03, 3.396E+03},
		{9.360E+03, 2.789E+03}
	};
	
	/* didn't calculate transport */
	/* mining; conversion; enrichment; fuel fab; transport */
	static double[][] FrontEndProliferation = {
		{0.7912,0.7912,0.7880,0.8069,1},
		{0.7912,0.7912,0.7815,0.7999,1},
		{0.7910,0.7910,1,0.5752,1}
	};
	/* didn't calculate snf transport; hlw vitrification; hlw storage
	/* snf storage; snf transport; reprocess; snf disposal; hlw vitrification; hlw storage; hlw disposal */
	static double[][] BackEndProliferation = {
		{0.8536,1,0.5830,0.8253,1,0.9139,0.9189},
		{0.8805,1,0.6171,0.8579,1,0.9362,0.9394},
		{0.8154,1,0.5986,0.8378,1,0.9162,0.9167}
	};
	
	public static boolean[] calcFrontEndNP = {true, true, true, true, false};
	public static boolean[] calcBackEndNP = {true, false, true, true, false, true, false};
	
	public static void main(String args[]) {
		
	}
	
	public static double[] getGWeighting() {return(GCriterionWeights);}
	public static double[] getUWeighting() {return(UCriterionWeights);}
	
	public static double[] getReprocessingCost() {return(ChosenReprocessingCost);}
	
	public static double[][] getDisposalCostOutcomes() {return(DisposalCostOutcomes);}
	public static double[][] getDisposalCostProbabilities() {return(DisposalCostProbabilities);}
	
	public static double getLWRCapitalCost() {return(LWRCapitalCost);}
	public static double[] getHTGRCapitalCost() {return(HTGRCapitalCost);}
	public static double[] getSFRCapitalCost() {return(SFRCapitalCost);}
	
	public static double[] getHTGRCapCostProb() {return(htgr_capcost_probability);}
	public static double[] getSFRCapCostProb() {return(sfr_capcost_probability);}
	
	public static int[][] getCapitalSubsidy() {return(CapitalSubsidy);}
	public static double getSubsidyAmount() {return(SubsidyAmount);}
	public static int getCapitalSubsidyYear() {return(CapitalSubsidyYear);}
	
	public static double[][] getDecayHeat() {return(decay_heat);}
	
	public static double[][] getFEProliferationMetric() {return(FrontEndProliferation);}
	public static double[][] getBEProliferationMetric() {return(BackEndProliferation);}
	public static boolean[] getCalcFEPR() {return(calcFrontEndNP);}
	public static boolean[] getCalcBEPR() {return(calcBackEndNP);}
	
}
