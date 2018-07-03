
public class DMInputs {

	static double[] GCriterionWeights = {(double) 1/3, (double) 1/3, (double) 1/3};
	static double[] UCriterionWeights = {(double) 1};
	
//	static double[] ReprocessingCostOutcomes = {950,1050,1300};
//	static double[][] DisposalCostOutcomes = {{600,3000},{800,4000},{1000,5000}};
	static double[][] DisposalCostProbabilities = {
		{(double) 1/3, (double) 1/3, (double) 1/3},
		{(double) 4/9, (double) 3/9, (double) 2/9},
		{(double) 5/9, (double) 4/9, (double) 1/9}
	};

	static double[] ChosenReprocessingCost = {903,1120,1339};
	static double[][] DisposalCostOutcomes = {{44,229},{135,1160},{191,1910},{265,1377},{550,5500},{801,6880}};
	
	static double LWRCapitalCost = 2000;
	static double[] HTGRCapitalCost = {1800, 2100, 2400};
	static double[] htgr_capcost_probability = {(double) 1/4, (double) 1/2, (double) 1/4};
	static double[] SFRCapitalCost = {1600, 2000, 2400};
	static double[] sfr_capcost_probability = {(double) 1/4, (double) 1/2, (double) 1/4};
	
	static double[][] ChosenCapitalSubsidy = {{200,0,0},{0,200,0},{0,0,200},{0,100,100}};
	static int CapitalSubsidyYear = 2045;
	
	static double[][] decay_heat = { // [reactor type][actinides, fission products]
		{1.849E+03, 1.322E+03},
		{4.145E+03, 3.396E+03},
		{9.360E+03, 2.789E+03}
	};
	
	static double[][] FrontEndProliferation = {
		{0.4123,0.4123,0.0656,0.4189,1},
		{0.4123,0.4123,0.0131,0.4160,1},
		{0.4123,0.4123,1,0.2476,1}
	};
	static double[][] BackEndProliferation = {
		{0.3585,1,0.2466,0.3446,1,1,0.3699},
		{0.3698,1,0.2603,0.3603,1,1,0.3698},
		{0.3524,1,0.2514,0.3591,1,1,0.3699}
	};
	
	public static void main(String args[]) {
		
	}
	
	public static double[] getGWeights() {return(GCriterionWeights);}
	public static double[] getUWeights() {return(UCriterionWeights);}
	
	public static double[] getChosenReprocessingCost() {return(ChosenReprocessingCost);}
	
	public static double[][] getDisposalCostOutcomes() {return(DisposalCostOutcomes);}
	public static double[][] getDisposalCostProbabilities() {return(DisposalCostProbabilities);}
	
	public static double getLWRCapitalCost() {return(LWRCapitalCost);}
	public static double[] getHTGRCapitalCost() {return(HTGRCapitalCost);}
	public static double[] getSFRCapitalCost() {return(SFRCapitalCost);}
	
	public static double[] getHTGRCapCostProb() {return(htgr_capcost_probability);}
	public static double[] getSFRCapCostProb() {return(sfr_capcost_probability);}
	
	public static double[][] getChosenCapitalSubsidy() {return(ChosenCapitalSubsidy);}
	public static int getCapitalSubsidyYear() {return(CapitalSubsidyYear);}
	
	public static double[][] getDecayHeat() {return(decay_heat);}
	
	public static double[][] getFEProliferationMetric() {return(FrontEndProliferation);}
	public static double[][] getBEProliferationMetric() {return(BackEndProliferation);}
	
}
