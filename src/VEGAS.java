/*
 * VEGAS.java
 * Erich Schneider, the University of Texas at Austin
 * Created on April 4, 2003, 1:05 PM, at a Starbucks in Las Vegas, NV
 * Version June 2012
 */
import java.text.*;
import java.math.*;
import java.lang.*;
import java.lang.Math.*;
import java.lang.Object.*;
import java.util.*;
import java.io.*;
import java.nio.*;
/**
 *
 * @author  eschneider
 * @version ALPHA
 */
public class VEGAS {

	/* Variables added by Birdy */
	static boolean verbose;
	static boolean ReprocessOnDemand;
	static boolean boar=true;
	
	static boolean only_one=true;
	static int[] robustInts = {3,3,3,1,1,1,1,1,1}; /* TODO */
	/* robustInts{0,1,2,3,4,5,6,7}
	 * 0 = U's first reactor build decision
	 * 1 = U's second reactor build decision
	 * 2 = U's third reactor build decision
	 * 3 = G's chosen reprocessing cost
	 * 4 = G's chosen capital subsidy
	 * 5 = waste disposal cost outcome
	 * 6 = htgr capital cost outcome
	 * 7 = sfr capital cost outcome
	 */

	static double[] ChosenReprocessingCost = DMInputs.getChosenReprocessingCost();
	static double[][] ChosenCapitalSubsidy = DMInputs.getChosenCapitalSubsidy();
	
	static double[][] DisposalCost = DMInputs.getDisposalCostOutcomes();
	static double LWRCapitalCost = DMInputs.getLWRCapitalCost();
	static double[] HTGRCapitalCost = DMInputs.getHTGRCapitalCost();
	static double[] SFRCapitalCost = DMInputs.getSFRCapitalCost();
	

	static int[] FirstReactorBuildDecision = {0,1,2,3};
	static int[] SecondReactorBuildDecision = {0,1,2,3};
	static int[][][] FinalReactorBuildDecision = {
		{{0},{0,1},{0,2},{0,1,2,3}},
		{{0,1},{0,1},{0,1,2,3},{0,1,2,3}},
		{{0,2},{0,1,2,3},{0,2},{0,1,2,3}},
		{{0,1,2,3},{0,1,2,3},{0,1,2,3},{0,1,2,3}}
	};

	static double yearlySimulationValues[][][][][][][][][][];
	
	static double[][] decay_heat = DMInputs.getDecayHeat();
	static double[][] front_end_proliferation = DMInputs.getFEProliferationMetric();
	static double[][] back_end_proliferation = DMInputs.getBEProliferationMetric();

	static boolean[] RX_PROTOTYPE;
	
	static int[] facilitiesAddedSoFar;
	static double[] yearlyReactorCharge;
	
	static double[][] DisposedAmount; /* Cumulative amount disposed [1-SNF; 2-HLW][year] */
	static double[][] DisposalCostModifier; /* [1-SNF; 2-HLW][year]; */
	static double DisposalGrowthCoeff = 0.; 
	
	static int runNumber = 0; /* for counting runs */
	
	static int repTechNo;
	static double[][] SFReprocessedByTier;
	static double[][] reprocessingUnitCost;
	
	static double[][] SFReprocessedByReactor;

	final static boolean printOutputFiles=true;

	static double socialDR = 0.0;
	static double socialPVF = 0.;
	static boolean bug = true;

	static int[] InitialGenerators;
	static double[] InitialFractions;

	static double NewReactorLifetime;
	static double DiscountRate;
	static double RiskFreeROR;
	static double ReturnOnEquity;
	static double EquityFraction;
	static double ConstructionTime;
	static double NUFraction;
	static double TailsFraction;

	static String[] FRONTENDTECH=new String[5];
	static double[] DEFAULTFECOST = new double[FRONTENDTECH.length];
	static double[] TIMELAG_FRONTEND = new double[FRONTENDTECH.length];
	static int[] FRONTENDREFYEAR = new int[FRONTENDTECH.length];
	static double[] FRONTENDREFAMOUNT = new double[FRONTENDTECH.length];
	static double[] FRONTENDAMOUNTEXP = new double[FRONTENDTECH.length];
	static double[] FRONTENDTIMEEXP = new double[FRONTENDTECH.length];
	static double[][] TotalFrontEndUse;                                          // [fontendtech][year]


	static String[] BACKENDTECH=new String[7];
	static double[] DEFAULTBECOST = new double[BACKENDTECH.length];
	static double[][][] DYNAMICBECOST; // This isn't used yet.. maybe delete
	static double[] TIMELAG_BACKEND_DD=new double[BACKENDTECH.length];
	static double[] TIMELAG_BACKEND = new double[BACKENDTECH.length];
	static double[] BACKEND_TIMELAG = new double[BACKENDTECH.length]; // because the above gets rewritten in augmentBackEndRepCharges
	static double REPROCESS_RECOVERY_FRACTION;

	final static double[] BACKENDMASS_DD={1,1,0,1,0,0,0};

	final static double OMFRACTION=0.04;

	static String[] REACTORNAMES;  
	static int[] BELONGS_TO_TIER;
	static int[] YEAR_AVAILABLE;
	static double[] PLANT_SIZE;
	static double[] BURNUP;
	static double[] RECIRC;
	static double[] AVAILABILITY;
	static double[] EFFICIENCY;
	static double[] CAPITALCOST;
	static double[] ANNUAL_OM;
	static double[] U_ENRICHMENT;
	static boolean[] ALLOWED_TO_USE_SEP_ACTINIDES;
	static double[][] FRONTENDMASS;
	static double[][] BACKENDMASS;
	static double[][] MASS_IN;
	static double[][] MASS_OUT;
	static int[][] ReprocessHierarchy;
	static int[][] ReplaceWithType;
	static int[][] YearReplaceWithTypeSpecified;
	static int[] COOLING_TIMES;  
	static int[] RES_TIMES;
	static int[] BATCHES_PER_CORE;
	static double[] NOAKCapitalCost;
	static double[][] FEBathkeFOM;
	static double[][] BEBathkeFOM;

	static double TRUSTORCOST;
	static double TRUVITDISPCOST;
	static double LegacySF; 
	static int LegacySFType;
	static int START_YEAR;	
	static int END_YEAR;
	static int EQUILIBRIUM_YEAR;

	static int YearInitialFleetStartsRetiring=2010;
	static int YearInitialFleetFinishesRetiring=2045;
	static double InitialGenCap=97.e6;
	final static double EPS=1.e-6;

	static int[] YearDemandSpecified;
	static double[] targetGenCap;
	static double[] growthRate;
	static double[] specifiedGenCap;
	static String NFCParameterFile="NFC_parameters.txt";
	static String ReactorParameterFile="Reactor_parameters.txt";

	static int NumberOfTiers=3;                       // number of reprocessing tiers in use
	static int MaxReprocChanges=0;                    // specifies maximum number of changes in reprocessing capacity in any tier
	static int[] ReprocessingAdditions;               // reprocessing addition by tier
	static int[][] YearReprocessingSpecified;         // [tier][number]
	static double[][] specifiedTierCap;               // [tier][number]
	static double[][] growthRateTier;                 // [tier][number]

	static int NumberOfCycles;                        // number of possible fuel cycles with combinations from available reactors
	static int[][] Cycle;                             // list of possible cycles [number][tier reactor]; -1 if tier is not used 
	static double[][] CycleCOE;                       // prices for all cycles every year [cycle_number][year_count]
	static double[][] fuel_eq_Outp;                   // global variable for fuel eq model outputs
	static double[][] throughput_eq_Outp;             // global variable for reactors throughput
	static double[][][] materials_eq_Outp;            // global variable for U Pu and Ma
	static double[][] uranium_input;                  // global variable for natural uranium input
	static double FuelCyclePricesYear[][];            // All fuel cycle prices in certain year 
	static double NuclearReactorNumber[][];           // Reactor number from equlibrium 
	static int CycleNumber;                           // Minimum price cycle number
	static boolean ScenarioManual=false;              // Sets dynamic decision making tool initial value false. Initially decisions are made dynamically. Estonians

	static int[] HierarchyByYear;
	static int[][] FacilityHierarchy;
	static double[][] FacilityPercentage;
	static int[][] BuildOrder;

	static double[] tempcoe;                          // debugging storage for coe
	static double[] tempfe;                           // debugging storage for fe
	static double[] tempbe;                           // debugging storage for be
	static double[] tempom;                           // debugging storage for o&m
	static double[] tempbl;                           // debugging storage for build

	static boolean print_cost_coefficients=false,add_to_integrals=false;
	static boolean tilt=false;

	static Vector[][] AmountOfSFReprocessed;
	static int[][] facilitiesAdded;
	static double[] massConv;
	static double[] yearlyCapitalCharge;
	static double[] yearlyOM;
	static double[][] unitCostsFE;
	static double[][] unitCostsBE;
	static double[][] genCap;               // electricity generation capacity [reator type][year]
	static double[][] SFReprocessed;
	static double[][] SFGenerated;
	static int[][] yearSFReprocessed;
	static double[][] ActinideWasteStream;
	static double[] totalGenCap;
	static double[][] puDemand;
	static double[][] maDemand;
	static double[] totalPuCharged;
	static double[] totalMACharged;
	static double[] puStockpile;
	static double[] maStockpile;
	static double[] puUnavailable;
	static double[] maUnavailable;
	static double[] puAvailable;
	static double[] maAvailable;
	static double[] legacySFStockpile;
	static double[] TRUStorageCost;
	static double[][] integratedCosts;
	static double totalEnergyGenerated, frontEndCharges,reactorCharges,backEndCharges;
	static double averageCOE;

	static double[][] NatUraniumUse;               //[reactor type][year]
	static double[][] FrontEndPriceModifier;    //[front end tech][year]
	static int UraniumPriceDefaultYear=2005;
	
	static double[][] modUnitCostsBE = new double[BACKENDTECH.length][3]; // [tech number][ACC/FOM/VOM] Added by Birdy
	static double[] BEAnnualCapitalCost = new double[BACKENDTECH.length];
	static double[] BEPlantSize = new double[BACKENDTECH.length];
	static double[] BEFixedOM = new double[BACKENDTECH.length];
	static double[] BEVariableOM = new double[BACKENDTECH.length];

	public VEGAS() {
		loadNFCParameters();
		dimensionArrays(); 
	}

	public void dimensionArrays() {

		AmountOfSFReprocessed = new Vector[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		facilitiesAdded = new int[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		massConv=new double[BURNUP.length];
		yearlyCapitalCharge=new double[BURNUP.length];
		yearlyOM=new double[BURNUP.length];
		genCap = new double[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		SFReprocessed = new double[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		SFGenerated = new double[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		yearSFReprocessed = new int[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		ActinideWasteStream = new double[3][END_YEAR-START_YEAR+1];
		totalGenCap = new double[END_YEAR-START_YEAR+1];
		puDemand = new double[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		maDemand = new double[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		totalPuCharged = new double[END_YEAR-START_YEAR+1];
		totalMACharged = new double[END_YEAR-START_YEAR+1];
		puStockpile = new double[END_YEAR-START_YEAR+1];
		maStockpile = new double[END_YEAR-START_YEAR+1];
		puUnavailable = new double[END_YEAR-START_YEAR+1];
		maUnavailable = new double[END_YEAR-START_YEAR+1];
		puAvailable = new double[END_YEAR-START_YEAR+1];
		maAvailable = new double[END_YEAR-START_YEAR+1];
		legacySFStockpile = new double[END_YEAR-START_YEAR+1];
		tempcoe = new double[END_YEAR-START_YEAR+1];
		tempfe = new double[END_YEAR-START_YEAR+1];
		tempbe = new double[END_YEAR-START_YEAR+1];
		tempom = new double[END_YEAR-START_YEAR+1];
		tempbl = new double[END_YEAR-START_YEAR+1];
		TRUStorageCost = new double[END_YEAR-START_YEAR+1];
		integratedCosts=new double[REACTORNAMES.length][1+FRONTENDTECH.length+BACKENDTECH.length+2];
		FrontEndPriceModifier=new double[FRONTENDTECH.length+1][END_YEAR-START_YEAR+1];
		NatUraniumUse=new double[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		TotalFrontEndUse=new double[FRONTENDTECH.length][END_YEAR-START_YEAR+1];
		DisposedAmount = new double[2][END_YEAR-START_YEAR+1];
		DisposalCostModifier = new double[2][END_YEAR-START_YEAR+1];
		SFReprocessedByTier = new double[NumberOfTiers][END_YEAR-START_YEAR+1];
		reprocessingUnitCost = new double[REACTORNAMES.length][END_YEAR-START_YEAR+1];
		SFReprocessedByReactor = new double[REACTORNAMES.length][END_YEAR-START_YEAR+1];
	
		facilitiesAddedSoFar = new int[REACTORNAMES.length];
		yearlyReactorCharge = new double[END_YEAR-START_YEAR+1]; // 9 units added to reach NOAK; with or without the capital subsidy
		yearlySimulationValues = new double[ChosenReprocessingCost.length][DisposalCost.length][FirstReactorBuildDecision.length][ChosenCapitalSubsidy.length][SecondReactorBuildDecision.length][HTGRCapitalCost.length][SFRCapitalCost.length][FinalReactorBuildDecision.length][END_YEAR-START_YEAR+1][4];

	}

	public double getFeedMass(double targ_enrich) {

		return((targ_enrich-TailsFraction)/(NUFraction-TailsFraction));
	}

	public double getSWU(double targ_enrich) {

		double p=targ_enrich/100.;
		double t=TailsFraction/100.;
		double f=NUFraction/100.;

		double F=(p-t)/(f-t);
		double T=F-1.;
		return((2.*p-1.)*Math.log(p/(1-p))+T*(2*t-1.)*Math.log(t/(1-t))-F*(2*f-1.)*Math.log(f/(1-f)));
	}

	public void getAllCyclesPrices(int year_count) {
		//static double[][] CycleCOE;                       // prices for all cycles every year [cycle_number][year_count]
		CycleCOE = new double[NumberOfCycles+1][END_YEAR-START_YEAR+1];
		for (int i=0; i<NumberOfCycles; i++) {

			if (verbose) {
				System.out.println(" ");
				System.out.println("Cycle " + i +":");
			}

			CycleCOE[i][year_count] = getCyclePrice(year_count, Cycle[i], fuel_eq_Outp[i]);

			if (verbose) {System.out.println("Cycle " + i +" price in " + (START_YEAR+year_count) + ": " + CycleCOE[i][year_count]);}

		}
	}

	public double getCyclePrice(int year_count, int []cycle, double []ratio) {  // function to calculate the prices of the possible fuel cycles for the current year
		double dummy_price = 999999;
		int length = 0;
		boolean available=true;
		for (int i=0; i<NumberOfTiers; i++) {
			if (cycle[i] != -1) {
				length++;
				if (YEAR_AVAILABLE[cycle[i]] > year_count+START_YEAR) {
					available = false;
				}
			}
		}
		double fe_costs = 0.;
		double be_costs = 0.;
		double reactor_costs = 0.;
		double om_costs = 0.;
		for (int j=0; j<length; j++) {
			fe_costs += augmentFrontEndChargesnp(targetGenCap[year_count]*ratio[j], cycle[j], year_count);
			be_costs += augmentBackEndDDChargesnp(cycle[j], targetGenCap[year_count]*ratio[j]);
			reactor_costs += targetGenCap[year_count]*ratio[j]*yearlyCapitalCharge[cycle[j]]/AVAILABILITY[cycle[j]];
			om_costs += targetGenCap[year_count]*ratio[j]*yearlyOM[cycle[j]]/AVAILABILITY[cycle[j]];
		}
		double price = (fe_costs+be_costs+reactor_costs+om_costs)/targetGenCap[year_count]/100.;    // where does the availability come in?
		if (available == false) {
			price = dummy_price;
		}
		if ((cycle[0] == 2) && (cycle[1] == -1) && (cycle[2] == -1)) {
			tempcoe[year_count] = price;
			tempfe[year_count] = fe_costs/targetGenCap[year_count]/100.;
			tempbe[year_count] = be_costs/targetGenCap[year_count]/100.;
			tempom[year_count] = om_costs/targetGenCap[year_count]/100.;
			tempbl[year_count] = reactor_costs/targetGenCap[year_count]/100.;
		}
		return price;

	}

	public void getPossibleCycles() {           // function to get the number of possible fuel cycles and the list of them
		int maxcycles = 0;
		for(int i=0; i<NumberOfTiers; i++) {
			int addcycles = 1;
			for (int j=0; j<i+1; j++) {
				addcycles *= (REACTORNAMES.length-j);
			}
			maxcycles += addcycles;                 // get maximum mathematically possible cycles with the number of tiers and reactors for array length
		}
		int[][] cycles = new int[maxcycles+1][NumberOfTiers+1];
		int cyclecount = 0;

		// ##### this next part only supports 3 tiers at the moment ##### //
		for(int i=0; i<REACTORNAMES.length; i++) {
			if (BELONGS_TO_TIER[i]==0) {                // if this is an open cycle (only one reactor type used, no reprocessing)
				cycles[cyclecount][0] = i;
				for (int xt=1; xt<NumberOfTiers+1; xt++) {
					cycles[cyclecount][xt] = -1;
				}
				cyclecount++;
			} else {                                    // if rector reprocesses other reactor SF
				for (int j=0; j<ReprocessHierarchy[i].length; j++) {
					if (BELONGS_TO_TIER[ReprocessHierarchy[i][j]] == 0) {   // if reprocesses the SF of a Tier 0 reactor
						cycles[cyclecount][0] = ReprocessHierarchy[i][j];
						cycles[cyclecount][1] = i;
						for (int xt=2; xt<NumberOfTiers+1; xt++) {
							cycles[cyclecount][xt] = -1;
						}
						cyclecount++;
					} else if (ReprocessHierarchy[i][j]!=i) {               // if reprocesses the SF of a higher tier reactor (desregarding itself)
						for (int k=0; k<ReprocessHierarchy[ReprocessHierarchy[i][j]].length; k++) {
							if (BELONGS_TO_TIER[ReprocessHierarchy[ReprocessHierarchy[i][j]][k]] == 0) {
								cycles[cyclecount][0] = ReprocessHierarchy[ReprocessHierarchy[i][j]][k];
								cycles[cyclecount][1] = ReprocessHierarchy[i][j];
								cycles[cyclecount][2] = i;
								for (int xt=3; xt<NumberOfTiers+1; xt++) {
									cycles[cyclecount][xt] = -1;
								}
								cyclecount++;
							}
						}
					}
				}
			}
		}
		// ##### the part above only supports 3 tiers at the moment ##### //

		for (int xt=0; xt<NumberOfTiers+1; xt++) {          // create an empty cycle at the end
			cycles[cyclecount][xt] = -1;
		}
		NumberOfCycles = cyclecount;                        // move the collected cycle count
		Cycle = new int[cyclecount+1][NumberOfTiers];       // and the cycles to global variables
		for (int i=0; i<cyclecount+1; i++) {                    // cycle numeration starts at 0, additional empty cycle at the end
			for (int j=0; j<NumberOfTiers; j++) {
				Cycle[i][j] = cycles[i][j];
			}
		}  
		
	}

	public void massEquilibriumClculation() {
		int i,j,c;

		fuel_eq_Outp=new double[Cycle.length][NumberOfTiers];
		throughput_eq_Outp=new double[Cycle.length][NumberOfTiers];
		materials_eq_Outp=new double[Cycle.length][NumberOfTiers][NumberOfTiers];
		uranium_input=new double[Cycle.length][NumberOfTiers];
		// reading part 
		int[] data=new int[3];
		for(c=0;c<Cycle.length;c++){           // Mass equilibrium calculation for each fuel cycle
			boolean tier0=false;               // Variables initialization
			boolean tier1=false;
			boolean tier2=false;
			boolean PUREX=false;
			boolean UREX=false;
			boolean Fastr=false;
			double xp=0.04,xt=0.003,xf=0.00711,crf=1,F=0,SWU,P=1,T=0,B=0,FB=0,PWRb=0,FRb=0,MA=0,U=0,pPu=0,Pu=0,Du=0,TRU=0,mpinU=0,mpinMa=0,mpinPu=0,MAf=0,Puf=0,Duf=0,PWRe=0,MOXe=0,FBe=0;
			double Pup=0,MDu=0,MOXb=0,outU=0,outPu=0,outMa=0,inU=0,inPu=0,pinU=0,pinPu=0,pinMa=0,moutU=0,moutPu=0,moutMa=0,moxB=0,rep=0,outUm=0,outPum=0,outMam=0,inMa=0,Mout=0,Bout=0;

			for(j=0;j<NumberOfTiers;j++){
				data[j]=Cycle[c][j];
			}
			int len=data.length;
			for(i=0;i<data.length;i++){        
				if(data[i]==-1)
				{
					len--;
				}
			}

			int[] indet=new int[len];
			double[][] reactor_count_by_type=new double[len][11];     // reactor parametrs variable        
			for(i=0,j=0;i<data.length;){                                  // set reactor numbers for each cycle 
				if(data[i]==-1)
				{
					i++;                
				}
				else
				{                
					indet[j]=data[i];
					j++;
					i++;
				}
			}

			for(i=0;i<len;i++){                                              // storing reactors data into one variable 
				reactor_count_by_type[i][0]=BELONGS_TO_TIER[indet[i]];
				reactor_count_by_type[i][1]=MASS_IN[indet[i]][0];
				reactor_count_by_type[i][2]=MASS_IN[indet[i]][1];
				reactor_count_by_type[i][3]=MASS_IN[indet[i]][2];
				reactor_count_by_type[i][4]=MASS_OUT[indet[i]][0];
				reactor_count_by_type[i][5]=MASS_OUT[indet[i]][1];
				reactor_count_by_type[i][6]=MASS_OUT[indet[i]][2]; 
				reactor_count_by_type[i][7]=BURNUP[indet[i]];
				reactor_count_by_type[i][8]=U_ENRICHMENT[indet[i]];
				reactor_count_by_type[i][9]=ReprocessHierarchy[indet[i]][0];
				reactor_count_by_type[i][10]=EFFICIENCY[indet[i]];
			}       

			// Initialize belongs to tier
			for(i=0;i<len;i++){
				if(reactor_count_by_type[i][0]==0)
				{
					tier0=true;
					xp=reactor_count_by_type[i][8]/100;
					B=reactor_count_by_type[i][7];
					outU=reactor_count_by_type[i][4];
					outPu=reactor_count_by_type[i][5];
					outMa=reactor_count_by_type[i][6];
					PWRe=reactor_count_by_type[i][10];                        
				}
				if(reactor_count_by_type[i][0]==1)
				{
					tier1=true;
					inU=reactor_count_by_type[i][1];
					inPu=reactor_count_by_type[i][2];
					inMa=reactor_count_by_type[i][3];
					outUm=reactor_count_by_type[i][4];
					outPum=reactor_count_by_type[i][5];
					outMam=reactor_count_by_type[i][6];
					moxB=reactor_count_by_type[i][7];
					MOXe=reactor_count_by_type[i][10];

				}
				if(reactor_count_by_type[i][0]==2)    
				{
					tier2=true;
					if(reactor_count_by_type[i][9]==indet[i]){
						Fastr=true;
						mpinU=reactor_count_by_type[i][1];
						mpinPu=reactor_count_by_type[i][2];
						mpinMa=reactor_count_by_type[i][3];
						FB=reactor_count_by_type[i][7];
						FBe=reactor_count_by_type[i][10];
						// Reactor reprocess fuel from itself CR calculation 
						if(reactor_count_by_type[i][6]/reactor_count_by_type[i][3]<reactor_count_by_type[i][5]/reactor_count_by_type[i][2]){
							crf=1/(1-reactor_count_by_type[i][6]/reactor_count_by_type[i][3]);
						}
						else{
							crf=1/(1-reactor_count_by_type[i][5]/reactor_count_by_type[i][2]);
						}                        
					}
					else if(tier1==false&&tier2==true){
						inU=reactor_count_by_type[i][1];
						inPu=reactor_count_by_type[i][2];
						inMa=reactor_count_by_type[i][3];
						outUm=reactor_count_by_type[i][4];
						outPum=reactor_count_by_type[i][5];
						outMam=reactor_count_by_type[i][6];
						moxB=reactor_count_by_type[i][7];
					}
				}
			}
			// Check if Pu>0 and MA>0 or 0 to use UREX or PUREX, or PUREX and then UREX 
			if(tier1==true||tier2==true){
				for(i=0;i<len;i++){
					if(reactor_count_by_type[i][3]>0&&reactor_count_by_type[i][2]>0)
					{
						UREX=true;
						pinU=reactor_count_by_type[i][1];
						pinPu=reactor_count_by_type[i][2];
						pinMa=reactor_count_by_type[i][3];
						//moxB=reactor_count_by_type[i][7];                           

					}
					if(reactor_count_by_type[i][3]==0&&reactor_count_by_type[i][2]>0){
						PUREX=true;                              
						moutU=reactor_count_by_type[i][4];
						moutPu=reactor_count_by_type[i][5];
						moutMa=reactor_count_by_type[i][6];
						//moxB=reactor_count_by_type[i][7];
						rep=reactor_count_by_type[i][9];
						if(reactor_count_by_type[i][9]==indet[i]){
							Fastr=true;
						}
					}
				}
			}

			F=1*(xp-xt)/(xf-xt);           // input feed mass calculation 

			// Logics for reprocessing facilities 
			// Purex  
			if(PUREX==true&&UREX==false){
				Pup=P*outPu;        
				MDu=Pup*inU/inPu;
			}       

			// UREX after PUREX Reprocess
			if(UREX==true&&PUREX==true){            
				//If MOX with MA fuel
				if(inMa>0&&inPu>0){
					U=P*outU;
					MA=P*outMa;
					Pu=P*outPu;
					if(outPu/inPu>outMa/inMa){ 
						Du=MA*pinU/pinMa;        
						pPu=MA*pinPu/pinMa;
						//Metal Fuel                        
						if(outPum/mpinPu>outMam/mpinMa){
							MAf=(Du+pPu+MA)*outMam;
							Duf=MAf*mpinU/mpinMa;
							Puf=MAf*mpinPu/mpinMa;                        
						}
						else{
							Puf=(Du+pPu+MA)*outPum;
							Duf=Puf*mpinU/mpinPu;
							MAf=Puf*mpinMa/mpinPu;
						}
					}
					else{
						Du=Pu*pinU/pinPu;
						MA=Pu*pinMa/pinPu;
						//Metal Fuel
						if(outPum/mpinPu>outMam/mpinMa){
							MAf=(Du+Pu+MA)*outMam;
							Duf=MAf*mpinU/mpinMa;
							Puf=MAf*mpinPu/mpinMa;                        
						}
						else{
							Puf=(Du+pPu)*outPum;
							Duf=Puf*mpinU/mpinPu;
							MAf=Puf*mpinMa/mpinPu;
						}
					}                
				}
				else{                       //MOX without MA fuel
					Pup=P*outPu;        
					MDu=Pup*inU/inPu;
					//Metal Fuel
					if(outPum/mpinPu>outMam/mpinMa){
						MAf=(MDu+Pup)*outMam+P*outMa;
						Duf=MAf*mpinU/mpinMa;
						Puf=MAf*mpinPu/mpinMa;                        
					}
					else{
						Puf=(MDu+Pup)*outPum;
						Duf=Puf*mpinU/mpinPu;
						MAf=Puf*mpinMa/mpinPu;
					}
				}
			}        

			if(UREX==true&&PUREX==false)
			{
				U=P*outU;
				MA=P*outMa;
				Pu=P*outPu;
				//Metal Fuel Fab
				if(tier1==true&&tier2==true){
					if(outPu/inPu>outMa/inMa){
						Pu=0;
						Du=MA*inU/inMa;        
						pPu=MA*inPu/inMa;
					}
					else{
						pPu=0;
						Du=Pu*inU/inPu;
						MA=Pu*inMa/inPu;
					}
					if(mpinPu>0&mpinMa>0){
						if(outPum/mpinPu>outMam/mpinPu){
							MAf=(Du+Pu+MA+pPu)*outMam;
							Duf=MAf*mpinU/mpinMa;
							Puf=MAf*mpinPu/mpinMa;                        
						}
						else{
							Puf=(Du+Pu+MA+pPu)*outPum;
							Duf=Puf*mpinU/mpinPu;
							MAf=Puf*mpinMa/mpinPu;
						}
					}
				}
				else{
					if(outPu/pinPu>outMa/pinMa){ 
						Pu=0;
						Du=MA*pinU/pinMa;        
						pPu=MA*pinPu/pinMa;
					}
					else{
						Du=Pu*pinU/pinPu;
						MA=Pu*pinMa/pinPu;
					}
				}
			}            

			if(tier0==true){
				// UOX LWR        
				PWRb=P*B*PWRe;
				materials_eq_Outp[c][0][0]=P;
			}
			if(tier0!=false&&tier1==true&&PUREX==true){
				//MOX Burnup
				MOXb=(Pup+Du)*moxB;
				materials_eq_Outp[c][1][0]=Du;
				materials_eq_Outp[c][1][1]=Pup;
			}
			if(UREX==true&&tier1==true&&tier0==true||PUREX==true&&tier0==true&&tier2==true){
				//MOX Burnup 
				MOXb=(MA+Du+Pu+MDu+Pup)*moxB*MOXe;
				Mout=MA+Du+Pu+MDu+Pup;
				materials_eq_Outp[c][1][0]=Du+MDu;
				materials_eq_Outp[c][1][1]=Pup+Pu;
				materials_eq_Outp[c][1][2]=MA;
			}
			if(tier0!=false&&tier2==true){
				// Fast burner reactor
				FRb=(MAf+Puf+Duf)*crf*FB*FBe;
				Bout=(MAf+Puf+Duf)*crf;
				materials_eq_Outp[c][2][0]=Duf*crf;
				materials_eq_Outp[c][2][1]=Puf*crf;
				materials_eq_Outp[c][2][2]=MAf*crf;
			}
			if(tier0!=false&&tier2==true&&UREX==true&&PUREX==false&&tier1==false){
				// Fast burner reactor
				FRb=(MA+Pu+Du+pPu)*crf*FB*FBe;
				Bout=(MA+Pu+Du+pPu)*crf;
				materials_eq_Outp[c][2][0]=Du*crf;
				materials_eq_Outp[c][2][1]=(Pu+pPu)*crf;
				materials_eq_Outp[c][2][2]=MA*crf;
			}

			// Ratio for each tier             
			for(int g=0;g<NumberOfTiers;g++)
			{
				int reactnum=Cycle[c][g];
				if(reactnum==-1){
					fuel_eq_Outp[c][g]=0;
				}
				else if(BELONGS_TO_TIER[reactnum]==0){
					fuel_eq_Outp[c][g]=PWRb/(PWRb+MOXb+FRb);    
				}
				else if(BELONGS_TO_TIER[reactnum]==1)
				{
					fuel_eq_Outp[c][g]=MOXb/(PWRb+MOXb+FRb);
				}
				else if(BELONGS_TO_TIER[reactnum]==2)
				{
					fuel_eq_Outp[c][g]=FRb/(PWRb+MOXb+FRb);
				}

			}


			// Fuel fractions  
			throughput_eq_Outp[c][0]=P;                   // Uranium fuel 
			throughput_eq_Outp[c][1]=Mout;                // Plutonium fuel 
			throughput_eq_Outp[c][2]=Bout;                // Metal fuel 
			// Uranium feed input for each tier
			uranium_input[c][0]=F;
			uranium_input[c][0]=0;
			uranium_input[c][0]=0;
		}
	}

	public void reInitializeEverything() {

		int i,j;

		for(i=0; i<END_YEAR-START_YEAR+1; i++) {
			totalPuCharged[i]=0.;
			totalMACharged[i]=0.;
			puStockpile[i]=0.;
			maStockpile[i]=0.;
			puUnavailable[i]=0.;
			maUnavailable[i]=0.;
			legacySFStockpile[i]=0.;
			TRUStorageCost[i]=0.;
			targetGenCap[i]=0.;
			totalGenCap[i]=0.;
			for(j=0; j<REACTORNAMES.length; j++) {
				genCap[j][i]=0.;
				SFReprocessed[j][i]=0.;
				SFGenerated[j][i]=0.;
				yearSFReprocessed[j][i]=END_YEAR+1;
				puDemand[j][i]=0.;
				maDemand[j][i]=0.;
				facilitiesAdded[j][i]=0;
				AmountOfSFReprocessed[j][i]=null;
				NatUraniumUse[j][i]=0.;
			}
			for (j=0; j<FRONTENDTECH.length; j++) {
				FrontEndPriceModifier[j][i]=1.;
			}
		}

	}

	public void getMassConv() {

		int i;

		for (i=0; i<massConv.length; i++) {
			massConv[i]=BURNUP[i]*1000.*(1.-RECIRC[i])*EFFICIENCY[i];
		}
	}

	/* original Vegas implementation for reactor charges */
	public void getReactorCharges() {

		int i,j,k;
		double effective_dr=ReturnOnEquity*EquityFraction+DiscountRate*(1-EquityFraction);

		for(i=0; i<CAPITALCOST.length; i++) {
			yearlyCapitalCharge[i]=Math.pow((1+effective_dr),ConstructionTime)*CAPITALCOST[i]*(effective_dr)*Math.pow((1+effective_dr),NewReactorLifetime)/(Math.pow((1+effective_dr),NewReactorLifetime)-1.)*1.1;  // for D&D
			yearlyOM[i]=ANNUAL_OM[i];
		}
	}
	
	public double[] getUnitReactorCharges(double[] capital_subsidy) {
		
		int n_rx,i,j,k;
		double learn=0, capital_cost;
		double effective_dr=ReturnOnEquity*EquityFraction+DiscountRate*(1-EquityFraction);
		int[] addedSoFar = new int[REACTORNAMES.length];
		double[] yearly_reactor = new double[END_YEAR-START_YEAR+1];

		for (n_rx=0; n_rx<CAPITALCOST.length; n_rx++) {

			learn = Math.log(NOAKCapitalCost[n_rx]/CAPITALCOST[n_rx]);
			learn /= Math.log(9);
			
			for (i=0; i<1; i++) { // this is just for i==0 .. why not just have a conditional? this seems dumb
				for (k=0; k<genCap[n_rx][i]/PLANT_SIZE[n_rx]; k++) {
					capital_cost = (addedSoFar[n_rx]>=8) ? NOAKCapitalCost[n_rx] : CAPITALCOST[n_rx]*Math.pow(addedSoFar[n_rx]+1,learn)-capital_subsidy[n_rx];
					for (j=i; j<i+NewReactorLifetime; j++) {
						yearly_reactor[j] += PLANT_SIZE[n_rx]*Math.pow((1+effective_dr),ConstructionTime)*capital_cost*(effective_dr)*Math.pow((1+effective_dr),NewReactorLifetime)/(Math.pow((1+effective_dr),NewReactorLifetime)-1.)*1.1;
						if (j==END_YEAR-START_YEAR) break;
					}
					addedSoFar[n_rx]++;
				}
			}

			for (i=1; i<END_YEAR-START_YEAR+1; i++) {

				for (k=0; k<facilitiesAdded[n_rx][i]; k++) {
					capital_cost = (addedSoFar[n_rx]>=8) ? NOAKCapitalCost[n_rx] : CAPITALCOST[n_rx]*Math.pow(addedSoFar[n_rx]+1,learn)-capital_subsidy[n_rx];
					for (j=i; j<i+NewReactorLifetime; j++) {
						yearly_reactor[j] += PLANT_SIZE[n_rx]*Math.pow((1+effective_dr),ConstructionTime)*capital_cost*(effective_dr)*Math.pow((1+effective_dr),NewReactorLifetime)/(Math.pow((1+effective_dr),NewReactorLifetime)-1.)*1.1;
						if (j==END_YEAR-START_YEAR) break;
					}
					addedSoFar[n_rx]++;
				}
			}
			
		}
		return(yearly_reactor);

	}


	public void assignGenerationCapacity() {

		int i,j,year;

		totalEnergyGenerated=0.;
		frontEndCharges=0.;
		backEndCharges=0.;
		reactorCharges=0.;
		year=START_YEAR;
		genCap[0][0]=InitialGenCap;
		for(i=1; i<genCap.length; i++) {
			for(j=0; j<genCap[0].length; j++) {
				genCap[i][j]=0.;
				yearSFReprocessed[i][j]=END_YEAR+1;
			}
		}
		dynamicallyAllocateCapacity();
	}

	public void dynamicallyAllocateCapacity() {

		int i,j,k=0;
		int facility_to_use, old_facility_to_use;
		int[][] number_added=new int[END_YEAR-START_YEAR+1][REACTORNAMES.length];
		int years_in_ramp_up=0;
		int[] ramp_up = {1,2,3,5};

		genCap[0][0]=0.;
		frontEndCharges=0.;
		backEndCharges=0.;
		totalEnergyGenerated=0.;
		for(i=0; i<InitialGenerators.length; i++) {
			genCap[InitialGenerators[i]][0]=InitialGenCap*InitialFractions[i];
		}
		for(i=1; i<YearInitialFleetStartsRetiring-START_YEAR+1; i++) { // up to the year before the initial fleet starts retiring
			for(j=0; j<InitialGenerators.length; j++) {
				genCap[InitialGenerators[j]][i]=genCap[InitialGenerators[j]][i-1]; // the initial fleet keeps generating
			}
		}
		for(i=YearInitialFleetStartsRetiring-START_YEAR+1; i<YearInitialFleetFinishesRetiring-START_YEAR+1; i++) { // in the retiring years
			for(j=0; j<InitialGenerators.length; j++) {
				genCap[InitialGenerators[j]][i]=genCap[InitialGenerators[j]][i-1]-InitialGenCap*InitialFractions[j]/(YearInitialFleetFinishesRetiring-YearInitialFleetStartsRetiring);
				//added[InitialGenerators[j]][i] = (int) ((genCap[InitialGenerators[j]][i]-genCap[InitialGenerators[j]][i-1])/PLANT_SIZE[InitialGenerators[j]]);
			}
		}
		for(j=0; j<REACTORNAMES.length; j++) {
			for(i=0; i<END_YEAR-START_YEAR+1; i++) {
				totalGenCap[i]+=genCap[j][i];
			} 
		}  

		// add new facilities beginning in YearInitialFleetStartsRetiring
		double added_this_year=0.;
		double current_demand_growth=0.;
		double current_capacity_target=0.;
		double annual_increment=0.;
		int growth_to_use=0, hierarchy_to_use=0;
		int count=0;
		boolean facility_built=false;
		targetGenCap[0]=InitialGenCap;

		for(i=1; i<END_YEAR-START_YEAR+1; i++) {
			
			/* sets the target generating capacity for year i */
			if(i+START_YEAR > YearDemandSpecified[growth_to_use]) {
				growth_to_use++;
				while(i+START_YEAR > YearDemandSpecified[growth_to_use]) growth_to_use++;
				if(specifiedGenCap[growth_to_use] > 0.) {
					annual_increment=(specifiedGenCap[growth_to_use]-targetGenCap[i-1])/(double)(YearDemandSpecified[growth_to_use+1]-i-START_YEAR+1);
				}
				else {
					annual_increment=0.;
					current_demand_growth=growthRate[growth_to_use-1];
				}
			}
			
			if(annual_increment == 0.) {
				targetGenCap[i]=targetGenCap[i-1]*(1.+current_demand_growth/100.);
			} else {
				targetGenCap[i]=targetGenCap[i-1]+annual_increment;
			}
		
		}
		
		
		/* allocates the capacity for year i (off-set by 1 year since initial generating capacity defined */
		for(i=1; i<END_YEAR-START_YEAR+1; i++) {
			
			if(ScenarioManual==false) SetRulesBasedOnCycles(i);	// Set building order dynamically 
			
			if(i+START_YEAR > HierarchyByYear[hierarchy_to_use+1]) {	// Building order year hierarchy
				hierarchy_to_use++;
				count=0;
			}
			
			while(totalGenCap[i]<targetGenCap[i]) {
				
				/* change building orders dynamically */
				if(ScenarioManual==false) setBuildOrders();                    
				
				if(count>=BuildOrder[hierarchy_to_use].length) count=0;
				facility_to_use=BuildOrder[hierarchy_to_use][count];
				
				facility_built=false;
				
				if(PLANT_SIZE[facility_to_use] < 0.8*(targetGenCap[i]-totalGenCap[i])) { /* need to compare the different facility to use plant size to the genCap difference */
					
					facility_built=true;
					count++;
					facilitiesAdded[facility_to_use][i]++; 
					
					for(j=i; j<Math.min(END_YEAR-START_YEAR+1,i+NewReactorLifetime); j++) {
						genCap[facility_to_use][j]+=PLANT_SIZE[facility_to_use];
						totalGenCap[j]+=PLANT_SIZE[facility_to_use]; 
					}
					
				}
				else break;
				
				if (RX_PROTOTYPE[facility_to_use]==true && facility_built==true) {
					
					years_in_ramp_up=0;
					for (j=1; j<=i; j++) if(facilitiesAdded[facility_to_use][j]>0) years_in_ramp_up++;
					if (years_in_ramp_up>0 && years_in_ramp_up<=ramp_up.length) {
						if (facilitiesAdded[facility_to_use][i]>ramp_up[years_in_ramp_up-1]) {
							/* remove the last build facility */
							facilitiesAdded[facility_to_use][i]--;
							for(j=i; j<Math.min(END_YEAR-START_YEAR+1,i+NewReactorLifetime); j++) {
								genCap[facility_to_use][j]-=PLANT_SIZE[facility_to_use];
								totalGenCap[j]-=PLANT_SIZE[facility_to_use]; 
							}
							
							old_facility_to_use = facility_to_use;
							for (k=0; k<BuildOrder[hierarchy_to_use].length; k++) { /* reset the facility to use */
								if (BuildOrder[hierarchy_to_use][k]!=old_facility_to_use) {
									facility_to_use=BuildOrder[hierarchy_to_use][k];
									break;
								}
							} 
							// if you go through the entire Build Order array without replacing the facility type
							for (j=0; j<YearReplaceWithTypeSpecified[old_facility_to_use].length; j++) {
								if (i >= YearReplaceWithTypeSpecified[old_facility_to_use][j]-START_YEAR) {
									if (facility_to_use==old_facility_to_use) facility_to_use = ReplaceWithType[old_facility_to_use][j];	
								}
							}

							/* add the different building in the hierarchy */
							if(PLANT_SIZE[facility_to_use] < 0.8*(targetGenCap[i]-totalGenCap[i])) {
								facilitiesAdded[facility_to_use][i]++;
								for(j=i; j<Math.min(END_YEAR-START_YEAR+1,i+NewReactorLifetime); j++) {
									genCap[facility_to_use][j]+=PLANT_SIZE[facility_to_use];
									totalGenCap[j]+=PLANT_SIZE[facility_to_use]; 
								}
							}
							else break;
							
						}
					}
				}
				
				
			}
			
			if(ScenarioManual==false){ /* for Estonians */
				for(j=0; j<REACTORNAMES.length; j++) {  
					SFGenerated[j][i]+=genCap[j][i]*capacityToMass(j);
					totalEnergyGenerated+=genCap[j][i]*365.*AVAILABILITY[j];
					frontEndCharges+=augmentFrontEndChargesnp(genCap[j][i],j,i);
					reactorCharges+=augmentReactorCharges(genCap[j][i],j);
					NatUraniumUse[j][i]=SFGenerated[j][i]*FRONTENDMASS[j][0];       //initial NU use estimations based on TryToBuild
				}
			}
			
		}
		
		
		/* increments the total generating capacity, SF inventories, and front end and reactor charges */
		if(ScenarioManual==true){
			for(j=0; j<REACTORNAMES.length; j++) { 
				for(i=0; i<END_YEAR-START_YEAR+1; i++) {
					//totalGenCap[i]+=genCap[j][i];
					SFGenerated[j][i]+=genCap[j][i]*capacityToMass(j);
					totalEnergyGenerated+=genCap[j][i]*365.*AVAILABILITY[j];
					frontEndCharges+=augmentFrontEndChargesnp(genCap[j][i],j,i);
					reactorCharges+=augmentReactorCharges(genCap[j][i],j);                       

				}
			}
		}

	}

	public void loadReactorParameters() {

		double dummy_double,amount_of_hlw;
		int dummy_int,n_rx,i,j;
		String dummy_string;
		String file_path = System.getProperty("user.dir") + File.separatorChar + ReactorParameterFile;
		File reactor_data = new File(file_path);
		BufferedReader buf;
		String current_line = " anything ", temp_string;
		StringTokenizer st = new StringTokenizer(current_line);
		boolean infiniteLoop = true;
		int line_counter = 0, reactor_counter = 0;
		try {
			buf = new BufferedReader(new FileReader(reactor_data));
			current_line = buf.readLine();
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);			
			n_rx=st.countTokens()-1;
			dimensionReactorVariables(n_rx);
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				REACTORNAMES[i]=st.nextToken();
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				BELONGS_TO_TIER[i]=Integer.valueOf( st.nextToken() ).intValue();
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				YEAR_AVAILABLE[i]=Integer.valueOf( st.nextToken() ).intValue();
			}
			current_line = buf.readLine(); /* TODO added by Birdy for new reactor type flag */
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				dummy_string=st.nextToken();
				RX_PROTOTYPE[i]=(dummy_string.startsWith("f") || dummy_string.startsWith("F"))? false:true;
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				dummy_string=st.nextToken();
				ALLOWED_TO_USE_SEP_ACTINIDES[i]=(dummy_string.startsWith("f") || dummy_string.startsWith("F"))? false:true;
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				PLANT_SIZE[i]=Double.valueOf( st.nextToken() ).doubleValue()*1000.;
			}		
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				BURNUP[i]=Double.valueOf( st.nextToken() ).doubleValue();
			}	
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				EFFICIENCY[i]=Double.valueOf( st.nextToken() ).doubleValue()/100.;
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				RECIRC[i]=Double.valueOf( st.nextToken() ).doubleValue()/100.;
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				AVAILABILITY[i]=Double.valueOf( st.nextToken() ).doubleValue()/100.;
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				CAPITALCOST[i]=Double.valueOf( st.nextToken() ).doubleValue();
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				ANNUAL_OM[i]=Double.valueOf( st.nextToken() ).doubleValue();
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				U_ENRICHMENT[i]=Double.valueOf( st.nextToken() ).doubleValue();
			}
			for(j=0; j<3; j++) {
				current_line = buf.readLine();
				st = new StringTokenizer(current_line);
				dummy_string=st.nextToken();
				for(i=0; i<n_rx; i++) {
					MASS_IN[i][j]=Double.valueOf( st.nextToken() ).doubleValue();
				}
			}
			for(j=0; j<3; j++) {
				current_line = buf.readLine();
				st = new StringTokenizer(current_line);
				dummy_string=st.nextToken();
				for(i=0; i<n_rx; i++) {
					MASS_OUT[i][j]=Double.valueOf( st.nextToken() ).doubleValue();
				}
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				if(U_ENRICHMENT[i] > NUFraction) {
					FRONTENDMASS[i][0]=getFeedMass(U_ENRICHMENT[i])*MASS_IN[i][0];
					FRONTENDMASS[i][1]=getFeedMass(U_ENRICHMENT[i])*MASS_IN[i][0];
					FRONTENDMASS[i][2]=getSWU(U_ENRICHMENT[i])*MASS_IN[i][0];
				} else if (U_ENRICHMENT[i] > TailsFraction) {
					FRONTENDMASS[i][0]=MASS_IN[i][0];
					FRONTENDMASS[i][1]=MASS_IN[i][0];
					FRONTENDMASS[i][2]=0.;
				} else {
					FRONTENDMASS[i][0]=0.;
					FRONTENDMASS[i][1]=0.;
					FRONTENDMASS[i][2]=0.;
				}
				FRONTENDMASS[i][3]=1.;
				FRONTENDMASS[i][4]=1.;
				amount_of_hlw=MASS_IN[i][0]+MASS_IN[i][1]+MASS_IN[i][2]-MASS_OUT[i][0]-(MASS_OUT[i][1]+MASS_OUT[i][2])*REPROCESS_RECOVERY_FRACTION;
				BACKENDMASS[i][0]=1.;
				BACKENDMASS[i][1]=1.;
				BACKENDMASS[i][2]=1.;
				BACKENDMASS[i][3]=0.;
				BACKENDMASS[i][4]=amount_of_hlw;
				BACKENDMASS[i][5]=amount_of_hlw;
				BACKENDMASS[i][6]=amount_of_hlw;				
				COOLING_TIMES[i]=Integer.valueOf( st.nextToken() ).intValue();
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				RES_TIMES[i]=Integer.valueOf( st.nextToken() ).intValue();
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				BATCHES_PER_CORE[i]=Integer.valueOf( st.nextToken() ).intValue();
			}
			current_line=buf.readLine();
			unitCostsFE = new double[n_rx][FRONTENDTECH.length];
			for(j=0; j<FRONTENDTECH.length; j++) {
				current_line = buf.readLine();
				st = new StringTokenizer(current_line);
				dummy_string=st.nextToken();
				for(i=0; i<n_rx; i++) {
					dummy_string=st.nextToken();
					if(dummy_string.compareToIgnoreCase("D")==0) {
						unitCostsFE[i][j]=DEFAULTFECOST[j];
					} else {
						unitCostsFE[i][j]=Double.valueOf(dummy_string).doubleValue();
					}
				}				
			}
			unitCostsBE = new double[n_rx][BACKENDTECH.length];
			for(j=0; j<BACKENDTECH.length; j++) {
				current_line = buf.readLine();
				st = new StringTokenizer(current_line);
				dummy_string=st.nextToken();
				for(i=0; i<n_rx; i++) {
					dummy_string=st.nextToken();
					if(dummy_string.compareToIgnoreCase("D")==0) {
						unitCostsBE[i][j]=DEFAULTBECOST[j];
						modUnitCostsBE[j][0] = BEAnnualCapitalCost[j];
						modUnitCostsBE[j][1] = BEFixedOM[j];
						modUnitCostsBE[j][2] = BEVariableOM[j];
					} else {
						unitCostsBE[i][j]=Double.valueOf(dummy_string).doubleValue();
					}
				}				
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				dummy_string=st.nextToken();
				if(dummy_string.compareToIgnoreCase("D")==0) {
					NOAKCapitalCost[i] = CAPITALCOST[i];
				} else {
					NOAKCapitalCost[i] = Double.valueOf(dummy_string).doubleValue();
				}
			}
			current_line=buf.readLine();
			current_line=buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {	
				dummy_string=st.nextToken();
				if(dummy_string.compareToIgnoreCase("X")==0) {				
					ReprocessHierarchy[i]=new int[1];
					ReprocessHierarchy[i][0]=i;
				} else {
					ReprocessHierarchy[i]=parseCommaDelineatedString(dummy_string);
				}
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				dummy_string=st.nextToken();
				if(dummy_string.compareToIgnoreCase("X")==0) {
					ReplaceWithType[i]=new int[1];
					ReplaceWithType[i][0]=i;
				} else {
					ReplaceWithType[i]=parseCommaDelineatedString(dummy_string);
				}
				//ReplaceWithType[i]=Integer.valueOf( st.nextToken() ).intValue();
			}
			current_line = buf.readLine();
			st = new StringTokenizer(current_line);
			dummy_string=st.nextToken();
			for(i=0; i<n_rx; i++) {
				dummy_string=st.nextToken();
				if(dummy_string.compareToIgnoreCase("X")==0) {
					YearReplaceWithTypeSpecified[i]=new int[1];
					YearReplaceWithTypeSpecified[i][0]=i;
				} else {
					YearReplaceWithTypeSpecified[i]=parseCommaDelineatedString(dummy_string);
				}
			}
			
			/* adding here TODO */
			current_line=buf.readLine();
			FEBathkeFOM = new double[n_rx][FRONTENDTECH.length];
			for(j=0; j<FRONTENDTECH.length; j++) {
				current_line = buf.readLine();
				st = new StringTokenizer(current_line);
				dummy_string=st.nextToken();
				for(i=0; i<n_rx; i++) {
					dummy_string=st.nextToken();
					if(dummy_string.compareToIgnoreCase("D")==0) {
						FEBathkeFOM[i][j]=DEFAULTFECOST[j];
					} else {
						FEBathkeFOM[i][j]=Double.valueOf(dummy_string).doubleValue();
					}
				}				
			}
			BEBathkeFOM = new double[n_rx][BACKENDTECH.length]; // don't need this..
			for(j=0; j<BACKENDTECH.length; j++) {
				current_line = buf.readLine();
				st = new StringTokenizer(current_line);
				dummy_string=st.nextToken();
				for(i=0; i<n_rx; i++) {
					dummy_string=st.nextToken();
					if(dummy_string.compareToIgnoreCase("D")==0) {
						BEBathkeFOM[i][j]=DEFAULTBECOST[j];
					} else {
						BEBathkeFOM[i][j]=Double.valueOf(dummy_string).doubleValue();
					}
				}				
			}
		}
		catch(IOException IOE) {
			System.err.println("VEGAS Error 01: Error reading reactor input file "+ReactorParameterFile);
			System.err.println(IOE.toString());
		}
	}

	public int[] parseCommaDelineatedString(String the_string) {

		int[] the_hierarchy;
		String temp_string;
		int location=0;

		temp_string=the_string;
		temp_string=temp_string.replaceAll(","," ");
		StringTokenizer st=new StringTokenizer(temp_string);
		the_hierarchy=new int[st.countTokens()];
		for(int i=0; i<the_hierarchy.length; i++) {
			the_hierarchy[i]=Integer.valueOf( st.nextToken() ).intValue();
		}
		return the_hierarchy;
	}

	public void dimensionReactorVariables(int how_many) {

		REACTORNAMES=new String[how_many];  
		BELONGS_TO_TIER=new int[how_many];
		YEAR_AVAILABLE=new int[how_many];
		RX_PROTOTYPE=new boolean[how_many];
		PLANT_SIZE=new double[how_many];
		BURNUP=new double[how_many];
		RECIRC=new double[how_many];
		AVAILABILITY=new double[how_many];
		EFFICIENCY=new double[how_many];
		CAPITALCOST=new double[how_many];
		ANNUAL_OM=new double[how_many];
		ALLOWED_TO_USE_SEP_ACTINIDES=new boolean[how_many];
		FRONTENDMASS=new double[how_many][FRONTENDTECH.length];
		BACKENDMASS=new double[how_many][BACKENDTECH.length];
		MASS_IN=new double[how_many][3];
		MASS_OUT=new double[how_many][3];
		ReprocessHierarchy=new int[how_many][];
		ReplaceWithType=new int[how_many][];
		YearReplaceWithTypeSpecified=new int[how_many][];
		COOLING_TIMES=new int[how_many];  
		RES_TIMES=new int[how_many];
		BATCHES_PER_CORE=new int[how_many];	
		U_ENRICHMENT=new double[how_many];
		NOAKCapitalCost=new double[how_many];
		
	}

	public void loadNFCParameters() {

		Parser nfc_file_parser = new Parser(NFCParameterFile,(System.getProperty("user.dir") + File.separatorChar));
		String dummy_string="anything";
		double dummy_double=0.;
		int dummy_int=0;
		int number_of_scenario_rules=0;              // default value for scenario rules
		
		verbose = (Boolean.parseBoolean(nfc_file_parser.getParam(dummy_string, "VerboseMode"))==true) ? true : false;
		ReprocessOnDemand = (Boolean.parseBoolean(nfc_file_parser.getParam(dummy_string, "ReprocessOnDemand"))==true) ? true : false;
		DisposalGrowthCoeff = nfc_file_parser.getParam(dummy_double, "DisposalCostGrowthCoefficient");
		
		socialDR=nfc_file_parser.getParam(dummy_double,"SocialDiscountRate")/100.;
		DiscountRate=nfc_file_parser.getParam(dummy_double,"FinancialDiscountRate")/100.;
		ReturnOnEquity=nfc_file_parser.getParam(dummy_double,"RequiredReturnOnEquity")/100.;
		EquityFraction=nfc_file_parser.getParam(dummy_double,"EquityFractionForNewCapacity")/100.;
		RiskFreeROR=nfc_file_parser.getParam(dummy_double,"RiskFreeRateOfReturn")/100.;
		NewReactorLifetime=nfc_file_parser.getParam(dummy_double,"NewReactorLifetime");
		ConstructionTime=nfc_file_parser.getParam(dummy_double,"ReactorConstructionTime");
		NUFraction=nfc_file_parser.getParam(dummy_double,"NaturalUEnrichment");
		TailsFraction=nfc_file_parser.getParam(dummy_double,"TailsUEnrichment");
		TRUSTORCOST=nfc_file_parser.getParam(dummy_double,"SeparatedActinideStorageCost");
		TRUVITDISPCOST=nfc_file_parser.getParam(dummy_double,"SeparatedActinideVitDisposalCost");
		LegacySF=nfc_file_parser.getParam(dummy_double,"LegacySNF")*1000;
		START_YEAR=nfc_file_parser.getParam(dummy_int,"StartYear");
		END_YEAR=nfc_file_parser.getParam(dummy_int,"EndYear");
		EQUILIBRIUM_YEAR=nfc_file_parser.getParam(dummy_int,"AssessEquilibriumCostsAtYear");
		LegacySFType=nfc_file_parser.getParam(dummy_int,"LegacySNFIsOfType");
		InitialGenCap=nfc_file_parser.getParam(dummy_double,"InitialGenerationCapacity")*1.e6;
		YearInitialFleetStartsRetiring=nfc_file_parser.getParam(dummy_int,"YearInitialFleetBeginsRetiring");
		YearInitialFleetFinishesRetiring=nfc_file_parser.getParam(dummy_int,"YearInitialFleetFinishesRetiring");
		int number_of_initial_types=nfc_file_parser.getListSize("InitialCapacityData");
		InitialGenerators=new int[number_of_initial_types];
		InitialFractions=new double[number_of_initial_types];
		for(int i=0; i<number_of_initial_types; i++) {              
			setInitialParameters(nfc_file_parser.getSub("InitialCapacityData",i),i);
		}

		ScenarioManual=Boolean.parseBoolean(nfc_file_parser.getParam(dummy_string,"ScenarioSet"));           // reading and setting boolean value from NFC file
		if(ScenarioManual==true)
		{
			number_of_scenario_rules=nfc_file_parser.getListSize("ScenarioRules");
			HierarchyByYear=new int[number_of_scenario_rules+1];
			HierarchyByYear[number_of_scenario_rules]=END_YEAR+1;
			FacilityHierarchy=new int[number_of_scenario_rules][];
			FacilityPercentage=new double[number_of_scenario_rules][];
			BuildOrder=new int[number_of_scenario_rules][];
		}
		else{
			number_of_scenario_rules=1;                                          // Initial scenario rule
			HierarchyByYear=new int[number_of_scenario_rules+1];
			HierarchyByYear[number_of_scenario_rules]=END_YEAR+1;
			FacilityHierarchy=new int[1][];                                          // Initial lenght of FacilityHierarchy
			FacilityPercentage=new double[1][];                                      // Initial lenght of FacilityPercentage
		}
		for(int i=0; i<FRONTENDTECH.length; i++) {              
			setFrontEndTech(nfc_file_parser.getSub("FrontEndTech",i),i);
		}
		for(int i=0; i<BACKENDTECH.length; i++) {              
			setBackEndTech(nfc_file_parser.getSub("BackEndTech",i),i);
		}
		int number_of_demand_specifiers=nfc_file_parser.getListSize("GrowthSpecified");
		YearDemandSpecified=new int[number_of_demand_specifiers+2];
		growthRate=new double[number_of_demand_specifiers+2];
		specifiedGenCap=new double[number_of_demand_specifiers+2];
		targetGenCap = new double[END_YEAR-START_YEAR+1];
		YearDemandSpecified[0]=START_YEAR;
		YearDemandSpecified[YearDemandSpecified.length-1]=END_YEAR+1;
		specifiedGenCap[0]=InitialGenCap;
		growthRate[0]=0.;
		loadReactorParameters();
		for(int i=0; i<number_of_demand_specifiers; i++) {              
			setDemandSpecifiers(nfc_file_parser.getSub("GrowthSpecified",i),i+1);
		}	
		if(ScenarioManual==true)
		{
			for(int i=0; i<number_of_scenario_rules; i++) {                             // Scenarios rules setting in case of manual decision     
				setScenarioRules(nfc_file_parser.getSub("ScenarioRules",i),i);
				BuildOrder[i]=makeBuildOrderArray(i);
			}
		} 
		for(int i=0; i<3; i++) {                      // get maximum number of reprocessing capacity changes to form an array           
			setReprocessingChanges(nfc_file_parser.getSub("ReprocessingCapacity",i));
		}
		ReprocessingAdditions=new int[NumberOfTiers];
		YearReprocessingSpecified=new int[NumberOfTiers][MaxReprocChanges+1];       // +1 to avoid array index out of bounds errors
		specifiedTierCap=new double[NumberOfTiers][MaxReprocChanges+1];
		growthRateTier=new double[NumberOfTiers][MaxReprocChanges+1];
		for(int i=0; i<3; i++) {                      // read reprocessing by tier           
			setReprocessingCapacity(nfc_file_parser.getSub("ReprocessingCapacity",i),i);
		}
	}

	public void setBuildOrders(){                     // Building orders for nuclear reactors
		if(ScenarioManual==false){
			Parser nfc_file_parser = new Parser(NFCParameterFile,(System.getProperty("user.dir") + File.separatorChar));
			BuildOrder=new int[1][]; 
			for(int i=0; i<1; i++) {         // Scenarios rules setting      
				setScenarioRules(nfc_file_parser.getSub("ScenarioRules",i),i);
				BuildOrder[i]=makeBuildOrderArray(i);
			} 
		}
	}

	public void setReprocessingChanges(Parser reprocessing_parser) {                    // determinte the max number of reprocessing changes function
		int capacity_additions=reprocessing_parser.getListSize("AddCapacity");
		if (capacity_additions>MaxReprocChanges) {
			MaxReprocChanges=capacity_additions;
		}
	}

	public void setReprocessingCapacity(Parser reprocessing_parser, int tier) {         // reprocessing capacity set function
		int i;
		ReprocessingAdditions[tier]=reprocessing_parser.getListSize("AddCapacity");      // get number of changes for a tier
		for(i=0; i<ReprocessingAdditions[tier]; i++) {
			setReprocess(reprocessing_parser.getSub("AddCapacity",i),i,tier);        // get reprocessing parameters
		}
		YearReprocessingSpecified[tier][i]=2200;            // additional value at the end to avoid array indexes being out of bounds
		specifiedTierCap[tier][i]=0.;
		growthRateTier[tier][i]=1.;
	}

	public void setReprocess(Parser a_repr, int number, int tier) {                     // reprocessing capacity set subfunction
		int dummy_int=0;
		double dummy_double=0.;
		YearReprocessingSpecified[tier][number]=a_repr.getParam(dummy_int,"Year");
		specifiedTierCap[tier][number]=a_repr.getParam(dummy_double,"Capacity")*1.e3;                   // change tons to kg
		growthRateTier[tier][number]=a_repr.getParam(dummy_double,"CapacityGrowthRate")/100.+1.;	// default growth rate multiplier of 1.
	}

	public int[] makeBuildOrderArray(int i) {

		double[] percentage_to_use = new double[FacilityHierarchy[i].length];
		double[] how_often_to_add=new double[FacilityHierarchy[i].length];
		double[] current_queue=new double[FacilityHierarchy[i].length];
		double next_up;
		int the_next_one;
		double norm = 0.;
		int[] the_order = new int[200];
		int j,k;

		for(j=0; j<FacilityHierarchy[i].length; j++) {
			percentage_to_use[j]=FacilityPercentage[i][j]/PLANT_SIZE[FacilityHierarchy[i][j]];
			norm+=percentage_to_use[j];
		}
		for(j=0; j<percentage_to_use.length; j++) {
			percentage_to_use[j]=percentage_to_use[j]/norm;
		}
		for(j=0; j<percentage_to_use.length; j++) {
			how_often_to_add[j]=1./percentage_to_use[j];
			current_queue[j]=how_often_to_add[j];
		}
		for(j=0; j<the_order.length; j++) {
			the_next_one=-1;
			next_up=1.e6;
			for(k=0; k<percentage_to_use.length; k++) {
				if(current_queue[k] < next_up) {
					the_next_one=k;
					next_up=current_queue[k];
				}
			}
			the_order[j]=FacilityHierarchy[i][the_next_one];
			current_queue[the_next_one]+=how_often_to_add[the_next_one];
			for(k=0; k<percentage_to_use.length; k++) {
				current_queue[k]-=1.;
			}
		}
		return(the_order);
	}

	public void setFrontEndTech(Parser tech_parser, int tech_number) {

		int dummy_int=0;
		double dummy_double=0.;
		String dummy_string="Anything";

		FRONTENDTECH[tech_number]=tech_parser.getParam(dummy_string,"Name");
		TIMELAG_FRONTEND[tech_number]=tech_parser.getParam(dummy_double, "DefaultLeadTime");
		DEFAULTFECOST[tech_number]=tech_parser.getParam(dummy_double, "DefaultCost");
		FRONTENDREFYEAR[tech_number]=tech_parser.getParam(dummy_int, "ReferenceYear");
		FRONTENDREFAMOUNT[tech_number]=tech_parser.getParam(dummy_double, "ReferenceAmount");
		FRONTENDAMOUNTEXP[tech_number]=tech_parser.getParam(dummy_double, "AmountExponent");
		FRONTENDTIMEEXP[tech_number]=tech_parser.getParam(dummy_double, "TimeExponent");
	}

	public void setBackEndTech(Parser tech_parser, int tech_number) {

		int dummy_int=0;
		double dummy_double=0.;
		String dummy_string="Anything";
		CharSequence cs = "Reprocessing"; /* search for the reprocessing technology */

		BACKENDTECH[tech_number]=tech_parser.getParam(dummy_string,"Name");
		if(BACKENDTECH[tech_number].contains(cs)) repTechNo = tech_number;
		TIMELAG_BACKEND_DD[tech_number]=tech_parser.getParam(dummy_double, "DefaultLagTime");
		BACKEND_TIMELAG[tech_number]=tech_parser.getParam(dummy_double, "DefaultLagTime");
		DEFAULTBECOST[tech_number]=tech_parser.getParam(dummy_double, "DefaultCost");
		if(tech_number==repTechNo) {
			REPROCESS_RECOVERY_FRACTION=1.-tech_parser.getParam(dummy_double,"PercentOfFeedLost")/100.;
		}
		BEAnnualCapitalCost[tech_number]=tech_parser.getParam(dummy_double, "AnnualCapitalCost");
		BEPlantSize[tech_number]=tech_parser.getParam(dummy_double, "PlantSize");
		BEFixedOM[tech_number]=tech_parser.getParam(dummy_double, "FixedOM");
		BEVariableOM[tech_number]=tech_parser.getParam(dummy_double, "VariableOM");
	}

	public void setInitialParameters(Parser type_parser, int type_number) {

		int dummy_int=0;
		double dummy_double=0.;

		InitialGenerators[type_number]=type_parser.getParam(dummy_int,"Type");
		InitialFractions[type_number]=type_parser.getParam(dummy_double,"Percentage")/100.;
	}

	public void setDemandSpecifiers(Parser demand_parser, int series_number) {

		int dummy_int=0;
		double dummy_double=0.;

		YearDemandSpecified[series_number]=demand_parser.getParam(dummy_int,"Year");
		specifiedGenCap[series_number]=demand_parser.getParam(dummy_double,"Capacity")*1.e6;
		growthRate[series_number]=demand_parser.getParam(dummy_double,"GrowthRate");
		
	}       


	public void setScenarioRules(Parser rule_parser, int rule_number) { // Scenario from NCF_parametrs file 
		getPossibleCycles();
		massEquilibriumClculation();
		int dummy_int=0;
		double dummy_double=0.;

		if(ScenarioManual==false){                                              // case for dynamic decission 
			HierarchyByYear[rule_number]=rule_parser.getParam(dummy_int,"Year");
			int try_to_build=1;                                                 // size of array
			//Filter facilityHierarchy lenght
			int len=NumberOfTiers;
			for(int i=0;i<NumberOfTiers;i++){
				if(Cycle[CycleNumber][i]==-1){
					len--;
				}
			}
			FacilityHierarchy[rule_number]=new int[len];
			FacilityPercentage[rule_number]=new double[len];
			for(int i=0; i<try_to_build; i++) {
				setARuleDynamic(try_to_build);
			}
		}
		else{                                                                             // case for manual building orders                    
			HierarchyByYear[rule_number]=rule_parser.getParam(dummy_int,"Year");
			int try_to_build=rule_parser.getListSize("TryToBuild");
			FacilityHierarchy[rule_number]=new int[try_to_build];
			FacilityPercentage[rule_number]=new double[try_to_build];
			for(int i=0; i<try_to_build; i++) {
				setARule(rule_parser.getSub("TryToBuild",i),i,rule_number);
			}


		}
	}

	public void setARuleDynamic(int number) {                           // functions sets dynamic building orders 

		int k=0;
		for(int i=0;i<number;i++){
			for(int j=0;j<NumberOfTiers;j++){
				if(Cycle[CycleNumber][j]==-1){
					j++;
				}
				else{
					FacilityHierarchy[i][j]=Cycle[CycleNumber][j];                            
				}
			}
		}
		//Copy precentage to Facility variable
		for(int i=0;i<number;i++){
			for(int j=0;j<NumberOfTiers;){
				if(fuel_eq_Outp[CycleNumber][j]==0){
					j++;
					k++;
				}
				else{
					FacilityPercentage[i][j-k]=fuel_eq_Outp[CycleNumber][j]*100;
					j++;
				}                        
			}
		}             
	}

	public void setARule(Parser a_rule, int number, int hierarchy) {             // function sets manual bulding orders

		int dummy_int=0;
		double dummy_double=0.;

		FacilityHierarchy[hierarchy][number]=a_rule.getParam(dummy_int,"FacilityNumber");
		FacilityPercentage[hierarchy][number]=a_rule.getParam(dummy_double,"Percentage");
	}

	public double augmentFrontEndCharges(double capacity, int reactor_type, int year_count, PrintWriter output_writer) {        // front end cost calculation

		int i;
		double charge=0;
		double pvf;

		if (year_count>FRONTENDREFYEAR[0]-START_YEAR) {         // if past the reference date, total NU use must be calculated
			TotalFrontEndUse[0][year_count]=TotalFrontEndUse[0][year_count-1];      // the value of the previous year is taken
			for (int j=0; j<REACTORNAMES.length; j++) {                                 //and all reactor use in current year is added to that
				TotalFrontEndUse[0][year_count]+=NatUraniumUse[j][year_count];
			}
		} else {                                                // else total NU usage is just the reference amount
			TotalFrontEndUse[0][year_count]=FRONTENDREFAMOUNT[0];
		}
		for(i=0; i<FRONTENDTECH.length; i++) {
			pvf=Math.exp(TIMELAG_FRONTEND[i]*DiscountRate);
			if (year_count>FRONTENDREFYEAR[i]-START_YEAR) {
				double timeprice=java.lang.Math.pow(year_count+START_YEAR-FRONTENDREFYEAR[i],FRONTENDTIMEEXP[i]);        //price increase due to time
				double amountprice=java.lang.Math.pow(TotalFrontEndUse[i][year_count]/FRONTENDREFAMOUNT[i],FRONTENDAMOUNTEXP[i]);    //price increase due to comsumption
				FrontEndPriceModifier[i][year_count]=timeprice*amountprice;      //total price increase
			}
			charge+=FRONTENDMASS[reactor_type][i]*unitCostsFE[reactor_type][i]*pvf*FrontEndPriceModifier[i][year_count];
			if(add_to_integrals) integratedCosts[reactor_type][1+i]+=FRONTENDMASS[reactor_type][i]*unitCostsFE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*socialPVF;
			if(print_cost_coefficients) if(printOutputFiles) output_writer.print((FRONTENDMASS[reactor_type][i]*unitCostsFE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type))+" ");
		}
		charge=charge*capacity*capacityToMass(reactor_type);
		return(charge);
	}

	/* Version for @birdybird's dissertation
	 * Don't pass the PrintWriter
	 */
	public double augmentFrontEndCharges(double capacity, int reactor_type, int year_count) {        // front end cost calculation

		int i;
		double charge=0;
		double pvf;

		if (year_count>FRONTENDREFYEAR[0]-START_YEAR) {         // if past the reference date, total NU use must be calculated
			TotalFrontEndUse[0][year_count]=TotalFrontEndUse[0][year_count-1];      // the value of the previous year is taken
			for (int j=0; j<REACTORNAMES.length; j++) {                                 //and all reactor use in current year is added to that
				TotalFrontEndUse[0][year_count]+=NatUraniumUse[j][year_count];
			}
		} else {                                                // else total NU usage is just the reference amount
			TotalFrontEndUse[0][year_count]=FRONTENDREFAMOUNT[0];
		}
		for(i=0; i<FRONTENDTECH.length; i++) {
			pvf=Math.exp(TIMELAG_FRONTEND[i]*DiscountRate);
			if (year_count>FRONTENDREFYEAR[i]-START_YEAR) {
				double timeprice=java.lang.Math.pow(year_count+START_YEAR-FRONTENDREFYEAR[i],FRONTENDTIMEEXP[i]);        //price increase due to time
				double amountprice=java.lang.Math.pow(TotalFrontEndUse[i][year_count]/FRONTENDREFAMOUNT[i],FRONTENDAMOUNTEXP[i]);    //price increase due to comsumption
				FrontEndPriceModifier[i][year_count]=timeprice*amountprice;      //total price increase
			}
			charge+=FRONTENDMASS[reactor_type][i]*unitCostsFE[reactor_type][i]*pvf*FrontEndPriceModifier[i][year_count];
			if(add_to_integrals) integratedCosts[reactor_type][1+i]+=FRONTENDMASS[reactor_type][i]*unitCostsFE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*socialPVF;
		}
		charge=charge*capacity*capacityToMass(reactor_type);
		return(charge);
	}
	
	/* added by Birdy for proliferation metric calculation */
	public double getFrontEndProliferation(double capacity, int reactor_type) {
		
		double metric=0.;

		for(int i=0; i<FRONTENDTECH.length; i++) {
			metric+=FRONTENDMASS[reactor_type][i]*FEBathkeFOM[reactor_type][i];
		}
		
		metric=metric*capacity*capacityToMass(reactor_type);
		return(metric);
		
	}

	/* OVERLOAD FUNCTION FROM ESTONIANS */
	public double augmentFrontEndChargesnp(double capacity, int reactor_type, int year_count) { 

		int i;
		double charge=0;
		double pvf;

		if (year_count>FRONTENDREFYEAR[0]-START_YEAR) {         // if past the reference date, total NU use must be calculated
			TotalFrontEndUse[0][year_count]=TotalFrontEndUse[0][year_count-1];      // the value of the previous year is taken
			for (int j=0; j<REACTORNAMES.length; j++) {                                 //and all reactor use in current year is added to that
				TotalFrontEndUse[0][year_count]+=NatUraniumUse[j][year_count];
			}
		} else {                                                // else total NU usage is just the reference amount
			TotalFrontEndUse[0][year_count]=FRONTENDREFAMOUNT[0];
		}
		for(i=0; i<FRONTENDTECH.length; i++) {
			pvf=Math.exp(TIMELAG_FRONTEND[i]*DiscountRate);
			if (year_count>FRONTENDREFYEAR[i]-START_YEAR) {
				double timeprice=java.lang.Math.pow(year_count+START_YEAR-FRONTENDREFYEAR[i],FRONTENDTIMEEXP[i]);        //price increase due to time
				double amountprice=java.lang.Math.pow(TotalFrontEndUse[i][year_count]/FRONTENDREFAMOUNT[i],FRONTENDAMOUNTEXP[i]);    //price increase due to comsumption
				FrontEndPriceModifier[i][year_count]=timeprice*amountprice;      //total price increase
			}
			charge+=FRONTENDMASS[reactor_type][i]*unitCostsFE[reactor_type][i]*pvf*FrontEndPriceModifier[i][year_count];
			if(add_to_integrals) integratedCosts[reactor_type][1+i]+=FRONTENDMASS[reactor_type][i]*unitCostsFE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*socialPVF;
		}
		charge=charge*capacity*capacityToMass(reactor_type);
		return(charge);
	}

	public double augmentReactorCharges(double capacity, int reactor_type) {

		return((yearlyCapitalCharge[reactor_type]+yearlyOM[reactor_type])*capacity);
	}

	public double capacityToMass(int cycle_number) {

		return(365.*AVAILABILITY[cycle_number]/massConv[cycle_number]);
	}

	public void getPuDemand() {

		int i,j;

		for(i=0; i<END_YEAR-START_YEAR+1;i++) {
			totalPuCharged[i]=0;
			puUnavailable[i]=0.;
			puAvailable[i]=0.;
			for(j=0; j<REACTORNAMES.length;j++) {
				puDemand[j][i]=genCap[j][i]*capacityToMass(j)*MASS_IN[j][1];
				totalPuCharged[i]+=puDemand[j][i];
			}
		}
	}

	public void getMADemand() {

		int i,j;

		for(i=0; i<END_YEAR-START_YEAR+1;i++) {
			totalMACharged[i]=0;
			maUnavailable[i]=0.;
			maAvailable[i]=0.;
			for(j=0; j<REACTORNAMES.length;j++) {
				maDemand[j][i]=genCap[j][i]*capacityToMass(j)*MASS_IN[j][2];
				totalMACharged[i]+=maDemand[j][i];
			}
		}
	}

	public double getSFBasedOnPuDemand(int reactor_type, double pu_demand) {

		return(pu_demand/(MASS_OUT[reactor_type][1])/REPROCESS_RECOVERY_FRACTION);
	}

	public double getSFBasedOnMADemand(int reactor_type, double ma_demand) {

		return(ma_demand/(MASS_OUT[reactor_type][2])/REPROCESS_RECOVERY_FRACTION);
	}

	public boolean useLegacySF(int year, double pu_demand, double ma_demand) {

		boolean is_enough=false;
		double amount_needed=0;

		amount_needed=Math.max(getSFBasedOnPuDemand(LegacySFType,pu_demand),getSFBasedOnMADemand(LegacySFType,ma_demand));
		if(amount_needed > legacySFStockpile[year-START_YEAR]) is_enough=false;
		else is_enough=true;
		return(is_enough);
	}

	public boolean useNewSF(int year_counter, int source_reactor, double pu_demand, double ma_demand) {

		boolean is_enough=false;
		double amount_needed=0;

		amount_needed=Math.max(getSFBasedOnPuDemand(source_reactor,pu_demand),getSFBasedOnMADemand(source_reactor,ma_demand));
		if(amount_needed > (SFGenerated[source_reactor][year_counter]-SFReprocessed[source_reactor][year_counter])) is_enough=false;
		else is_enough=true;
		return(is_enough);
	}

	public double getPuYieldOfSF(int reactor_type, double kg_of_SF) {

		return(kg_of_SF*MASS_OUT[reactor_type][1]);
	}

	public double getMAYieldOfSF(int reactor_type, double kg_of_SF) {

		return(kg_of_SF*MASS_OUT[reactor_type][2]);
	}

	public void updateLegacyStockpile(int year) {

		int i;

		for(i=year-START_YEAR; i<END_YEAR-START_YEAR; i++) {
			legacySFStockpile[i+1]=legacySFStockpile[i];         
		}

	}

	/* added by Birdy to fix implementation of limiting builds based on reprocessing capacity */
	public double[] allTheReprocessing(int year, double[] throughput_by_tier, double[] capacity_by_tier) {
		
		int i,j,k;
		double amount_to_use,amount_available;
		double pu_yield,ma_yield;
		
		for(k=0; k < throughput_by_tier.length; k++) {
			if(capacity_by_tier[k]>throughput_by_tier[k]) {

				for(j=0; j < REACTORNAMES.length; j++) {
					if(BELONGS_TO_TIER[j] == k) {

						if(j==LegacySFType) {
							if( legacySFStockpile[year-START_YEAR] > EPS) { // if there's legacy fuel in the stockpile else
								amount_to_use = Math.min(capacity_by_tier[k]-throughput_by_tier[k], legacySFStockpile[year-START_YEAR]);
								if(amount_to_use>EPS) {
									throughput_by_tier[k] += amount_to_use;
									legacySFStockpile[year-START_YEAR] -= amount_to_use;

									pu_yield = getPuYieldOfSF(LegacySFType,amount_to_use)*REPROCESS_RECOVERY_FRACTION;
									ma_yield = getMAYieldOfSF(LegacySFType,amount_to_use)*REPROCESS_RECOVERY_FRACTION;
									ActinideWasteStream[1][year-START_YEAR] += getPuYieldOfSF(LegacySFType,amount_to_use)*(1.-REPROCESS_RECOVERY_FRACTION);
									ActinideWasteStream[2][year-START_YEAR] += getMAYieldOfSF(LegacySFType,amount_to_use)*(1.-REPROCESS_RECOVERY_FRACTION);

									puStockpile[year-START_YEAR] += pu_yield;
									maStockpile[year-START_YEAR] += ma_yield;

									updateLegacyStockpile(year);
								} 
								
							} else {
								for(i=0; i < year-COOLING_TIMES[j]-RES_TIMES[j]-START_YEAR; i++) {
									amount_available = Math.max(SFGenerated[j][i]-SFReprocessed[j][i], 0.);
									if(amount_available>EPS) {
										amount_to_use = Math.min(capacity_by_tier[k]-throughput_by_tier[k], amount_available);
										if(amount_to_use>EPS) {
											throughput_by_tier[k] += amount_to_use;
											SFReprocessed[j][i] += amount_to_use;
											SFReprocessedByReactor[j][year-START_YEAR] += amount_to_use;
											yearSFReprocessed[j][i] = year;
											AmountOfSFReprocessed[j][i].addElement(new Integer(year));
											AmountOfSFReprocessed[j][i].addElement(new Double(amount_to_use));
											if(k==0) {
												System.out.print("here again");
											}
											pu_yield = getPuYieldOfSF(j,amount_to_use)*REPROCESS_RECOVERY_FRACTION;
											ma_yield = getMAYieldOfSF(j,amount_to_use)*REPROCESS_RECOVERY_FRACTION;
											ActinideWasteStream[1][year-START_YEAR] += getPuYieldOfSF(j,amount_to_use)*(1-REPROCESS_RECOVERY_FRACTION);
											ActinideWasteStream[2][year-START_YEAR] += getMAYieldOfSF(j,amount_to_use)*(1-REPROCESS_RECOVERY_FRACTION);

											puStockpile[year-START_YEAR] += pu_yield;
											maStockpile[year-START_YEAR] += ma_yield;
										}
									}
								}
							}

						} else if (j!=LegacySFType) {

							for(i=0; i < year-COOLING_TIMES[j]-RES_TIMES[j]-START_YEAR; i++) {
								amount_available = Math.max(SFGenerated[j][i]-SFReprocessed[j][i], 0.);
								if(amount_available>EPS) {
									amount_to_use = Math.min(capacity_by_tier[k]-throughput_by_tier[k], amount_available);
									if(amount_to_use>EPS) {
										throughput_by_tier[k] += amount_to_use;
										SFReprocessed[j][i] += amount_to_use;
										SFReprocessedByReactor[j][year-START_YEAR] += amount_to_use;
										yearSFReprocessed[j][i] = year;
										AmountOfSFReprocessed[j][i].addElement(new Integer(year));
										AmountOfSFReprocessed[j][i].addElement(new Double(amount_to_use));

										pu_yield = getPuYieldOfSF(j,amount_to_use)*REPROCESS_RECOVERY_FRACTION;
										ma_yield = getMAYieldOfSF(j,amount_to_use)*REPROCESS_RECOVERY_FRACTION;
										ActinideWasteStream[1][year-START_YEAR] += getPuYieldOfSF(j,amount_to_use)*(1-REPROCESS_RECOVERY_FRACTION);
										ActinideWasteStream[2][year-START_YEAR] += getMAYieldOfSF(j,amount_to_use)*(1-REPROCESS_RECOVERY_FRACTION);

										puStockpile[year-START_YEAR] += pu_yield;
										maStockpile[year-START_YEAR] += ma_yield;
									}
								}
							}

						}

					}
				} // all the reactor types

			} 
		} // all the tiers

		return(throughput_by_tier);
	}

	public double[] traverseReprocessHierarchy(int year, int reactor_type, double pu_demand, double ma_demand, boolean is_for_real, double[] capacity_by_tier) {
		
		int i,j;
		boolean is_enough=false;
		double amount_to_use,amount_available,pu_yield,ma_yield;
		double[] throughput_by_tier=new double[3];
		double reprocessed;
		
		for(i=0; i<throughput_by_tier.length;i++) throughput_by_tier[i]=0.; // for each reprocessing tier, initialize the throughput as 0
		for(i=0; i<ReprocessHierarchy[reactor_type].length; i++) { // start with the first reactor in reactor_type's reprocessing hierarchy
			if(ReprocessHierarchy[reactor_type][i]==LegacySFType && legacySFStockpile[year-START_YEAR] > EPS) { // if it calls for legacy fuel and there's nonzero legacy fuel in the stockpile
				is_enough=useLegacySF(year,pu_demand,ma_demand); 
				if(is_enough) {
					amount_to_use=Math.max(getSFBasedOnPuDemand(LegacySFType,pu_demand),getSFBasedOnMADemand(LegacySFType,ma_demand));
					if (amount_to_use + throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]] > capacity_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]) amount_to_use = capacity_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]-throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]];
					// remaining demand is taken care of when the yield from reprocessing amount_to_use is decremented
					throughput_by_tier[0]+=amount_to_use;
					if(is_for_real) legacySFStockpile[year-START_YEAR]-=amount_to_use;
				}
				else {
					amount_to_use=legacySFStockpile[year-START_YEAR];
					if (amount_to_use + throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]] > capacity_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]) amount_to_use = capacity_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]-throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]];
					throughput_by_tier[0]+=amount_to_use;
					if(is_for_real) legacySFStockpile[year-START_YEAR]=0.;
				}
				pu_yield=getPuYieldOfSF(LegacySFType,amount_to_use)*REPROCESS_RECOVERY_FRACTION;
				ma_yield=getMAYieldOfSF(LegacySFType,amount_to_use)*REPROCESS_RECOVERY_FRACTION;
				if(is_for_real) ActinideWasteStream[1][year-START_YEAR]+=getPuYieldOfSF(LegacySFType,amount_to_use)*(1.-REPROCESS_RECOVERY_FRACTION);
				if(is_for_real) ActinideWasteStream[2][year-START_YEAR]+=getMAYieldOfSF(LegacySFType,amount_to_use)*(1.-REPROCESS_RECOVERY_FRACTION);
				if(pu_yield <= pu_demand+EPS) {
					pu_demand-=pu_yield;
				}
				else {
					if(is_for_real) puStockpile[year-START_YEAR]+=pu_yield-pu_demand;
					pu_demand=0.;
				}
				if(ma_yield <= ma_demand+EPS) {
					ma_demand-=ma_yield;
				}
				else {
					if(is_for_real) maStockpile[year-START_YEAR]+=ma_yield-ma_demand;
					ma_demand=0.;
				}
				if(is_for_real) updateLegacyStockpile(year);
			}
			if(pu_demand > EPS || ma_demand > EPS) {
				for(j=0; j<year-COOLING_TIMES[ReprocessHierarchy[reactor_type][i]]-RES_TIMES[ReprocessHierarchy[reactor_type][i]]-START_YEAR;j++) {
					amount_available=Math.max(SFGenerated[ReprocessHierarchy[reactor_type][i]][j]-SFReprocessed[ReprocessHierarchy[reactor_type][i]][j],0.);
					if(amount_available>EPS && (pu_demand>EPS || ma_demand>EPS)) {
						is_enough=useNewSF(j,ReprocessHierarchy[reactor_type][i],pu_demand,ma_demand); // only checks if there's enough spent fuel
						if(is_enough) {
							amount_to_use=Math.max(getSFBasedOnPuDemand(ReprocessHierarchy[reactor_type][i],pu_demand),getSFBasedOnMADemand(ReprocessHierarchy[reactor_type][i],ma_demand));
							if (amount_to_use + throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]] > capacity_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]) amount_to_use = capacity_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]-throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]];
							throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]+=amount_to_use;
							if(is_for_real) SFReprocessed[ReprocessHierarchy[reactor_type][i]][j]+=amount_to_use;
							if(is_for_real) SFReprocessedByReactor[ReprocessHierarchy[reactor_type][i]][year-START_YEAR]+=amount_to_use;
							if(is_for_real) yearSFReprocessed[ReprocessHierarchy[reactor_type][i]][j]=year;
							if(is_for_real) AmountOfSFReprocessed[ReprocessHierarchy[reactor_type][i]][j].addElement(new Integer(year));
							if(is_for_real) AmountOfSFReprocessed[ReprocessHierarchy[reactor_type][i]][j].addElement(new Double(amount_to_use));			   
						}
						else {
							amount_to_use=SFGenerated[ReprocessHierarchy[reactor_type][i]][j]-SFReprocessed[ReprocessHierarchy[reactor_type][i]][j];
							if (amount_to_use + throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]] > capacity_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]) amount_to_use = capacity_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]-throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]];
							throughput_by_tier[BELONGS_TO_TIER[ReprocessHierarchy[reactor_type][i]]]+=amount_to_use;
							if(is_for_real) SFReprocessed[ReprocessHierarchy[reactor_type][i]][j]=SFGenerated[ReprocessHierarchy[reactor_type][i]][j];
							if(is_for_real) SFReprocessedByReactor[ReprocessHierarchy[reactor_type][i]][year-START_YEAR]=SFGenerated[ReprocessHierarchy[reactor_type][i]][year-START_YEAR];
							if(is_for_real) yearSFReprocessed[ReprocessHierarchy[reactor_type][i]][j]=year;
							if(is_for_real) AmountOfSFReprocessed[ReprocessHierarchy[reactor_type][i]][j].addElement(new Integer(year));
							if(is_for_real) AmountOfSFReprocessed[ReprocessHierarchy[reactor_type][i]][j].addElement(new Double(amount_to_use));				   
						}
						pu_yield=getPuYieldOfSF(ReprocessHierarchy[reactor_type][i],amount_to_use)*REPROCESS_RECOVERY_FRACTION;
						ma_yield=getMAYieldOfSF(ReprocessHierarchy[reactor_type][i],amount_to_use)*REPROCESS_RECOVERY_FRACTION;
						if(is_for_real) ActinideWasteStream[1][year-START_YEAR]+=getPuYieldOfSF(ReprocessHierarchy[reactor_type][i],amount_to_use)*(1.-REPROCESS_RECOVERY_FRACTION);
						if(is_for_real) ActinideWasteStream[2][year-START_YEAR]+=getMAYieldOfSF(ReprocessHierarchy[reactor_type][i],amount_to_use)*(1.-REPROCESS_RECOVERY_FRACTION);		       
						if(pu_yield <= pu_demand+EPS) {
							pu_demand-=pu_yield;
						}
						else {
							if(is_for_real) puStockpile[year-START_YEAR]+=(pu_yield-pu_demand);
							pu_demand=0.;
						}
						if(ma_yield <= ma_demand+EPS) {
							ma_demand-=ma_yield;
						}
						else {
							if(is_for_real) maStockpile[year-START_YEAR]+=(ma_yield-ma_demand);
							ma_demand=0.;
						}  
					}
				}
			}
			else break;
			//if((pu_demand<EPS) && (ma_demand<EPS)) break;    
		}
		reprocessed=legacySFStockpile[30];
		if(pu_demand>EPS || ma_demand>EPS) {
			if(is_for_real && bug) 

				if (verbose) System.out.println("Tilt!"+reprocessed+"  Pu or MA exhausted in year "+year+" Unsatisfied demand: Pu: "+pu_demand+" MA: "+ma_demand+" Facility w/ unsatisfied demand: "+REACTORNAMES[reactor_type]);			

			tilt=true;
		}

		return(throughput_by_tier);
	}

	public boolean reprocessSF() {

		int i,j,year,count=0;

		double pu_demand, ma_demand;
		boolean is_for_real,all_done=false;
		double[] throughput_by_tier=new double[3];
		double[] capacity_by_feed_tier = new double[3];
		double t1_gen_cap_guess=0.,cum_t0_cap_switched=0.,available_t0_cap;
		final int[] T0_HIERARCHY={18};
		final int[] T0_SYS_HIERARCHY={0};
		boolean infinite_loop=true;
		boolean switch_capacity=false;
		int doing_tier=0;

		puStockpile[0]=0;
		maStockpile[0]=0;
		for(i=0; i<END_YEAR-START_YEAR+1; i++) {   // main time loop####################################################################################################
			for(j=0; j<capacity_by_feed_tier.length;j++) capacity_by_feed_tier[j]=0.;
			if(i>0) {
				puStockpile[i]=puStockpile[i-1];
				maStockpile[i]=maStockpile[i-1];
			}
			year=i+START_YEAR;          

			for (int k=0; k<NumberOfTiers; k++) {  //iterate through all tiers
				capacity_by_feed_tier[k]=9.e15;    //default effectively infinite reprocessing value
				for(j=0; j<ReprocessingAdditions[k]; j++) {
					if(year>=YearReprocessingSpecified[k][j]) {
						if (specifiedTierCap[k][j]!=0. || capacity_by_feed_tier[k]==9.e15) {   // if the cap is not 0 or only the default infinite value
							capacity_by_feed_tier[k]=specifiedTierCap[k][j];
						}
						else {                                                               // if growth is specified instead
							if (year>=YearReprocessingSpecified[k][j+1]) {                      // if several different growths have to be included
								capacity_by_feed_tier[k]=capacity_by_feed_tier[k]*java.lang.Math.pow(growthRateTier[k][j],YearReprocessingSpecified[k][j+1]-YearReprocessingSpecified[k][j]);
							}
							else {                                                              // in this is the last growth that interests us
								capacity_by_feed_tier[k]=capacity_by_feed_tier[k]*java.lang.Math.pow(growthRateTier[k][j],year-YearReprocessingSpecified[k][j]);
							}
						}
					}
				} 
			}
			
			for(doing_tier=0; doing_tier<3; doing_tier++) {
				for(j=0; j<REACTORNAMES.length;j++) {
					if(BELONGS_TO_TIER[j]==doing_tier) {
						if(puDemand[j][i] > 0. || maDemand[j][i] > 0.) {
							is_for_real=true;
							pu_demand=puDemand[j][i];
							ma_demand=maDemand[j][i];
							while(infinite_loop) {
								if(ALLOWED_TO_USE_SEP_ACTINIDES[j]) {
									if(puStockpile[i]>0.) {
										if(pu_demand > puStockpile[i]) {
											pu_demand-=puStockpile[i];
											if(is_for_real) puStockpile[i]=0.;
										}
										else {
											if(is_for_real) puStockpile[i]-=pu_demand;
											pu_demand=0.;
										}
									}
									if(maStockpile[i]>0.) {
										if(ma_demand > maStockpile[i]) {
											ma_demand-=maStockpile[i];
											if(is_for_real) maStockpile[i]=0.;
										}
										else {
											if(is_for_real) maStockpile[i]-=ma_demand;
											ma_demand=0.;
										}
									}
								}
								if(pu_demand >0. || ma_demand >0.) {
									throughput_by_tier=traverseReprocessHierarchy(year,j,pu_demand,ma_demand,is_for_real,capacity_by_feed_tier);
									//if(throughput_by_tier[0]>2100.e3) System.out.println("PROBLEM! "+throughput_by_tier[0]+" "+is_for_real+" "+pu_demand);
								}
								
								for (int k=0; k<NumberOfTiers; k++) {
									if (throughput_by_tier[k]>capacity_by_feed_tier[k]) { 

										if (verbose) System.out.println("Over reprocessing tier "+k+"! Year: "+year+" Capacity: "+capacity_by_feed_tier[k]+" Demand: "+throughput_by_tier[k]);

										tilt=true;
									}
								}
								if (tilt==true) {
									if (!switch_capacity) modifyCapacity(j,i+START_YEAR);
									tilt=false;
									return(false);
								}
								if(is_for_real) break;
							}
						} else {
							for(int k=0; k<NumberOfTiers; k++) throughput_by_tier[k] = 0.; // otherwise throughput by tier equals previous year's throughput
						}
						
					}
				}
			}
			if(!ReprocessOnDemand) throughput_by_tier = allTheReprocessing(year, throughput_by_tier, capacity_by_feed_tier);
		}
		return(true);   
	}

	public void SetRulesBasedOnCycles(int year){                          //    Find cheapest fuel cycle option  from all cycles based on price

		FuelCyclePricesYear=new double[1][NumberOfCycles];
		for(int j=0;j<1;j++){
			for(int i=0;i<NumberOfCycles;i++){
				FuelCyclePricesYear[j][i]=getCyclePrice(year, Cycle[i], fuel_eq_Outp[i]);
			}} 
		//Copy array
		double[] fuelcyclearray=new double[NumberOfCycles];
		for(int i=0;i<NumberOfCycles;i++){
			fuelcyclearray[i]=FuelCyclePricesYear[0][i];                
		}
		//Find minimum value
		Arrays.sort(fuelcyclearray);
		//            System.out.println(fuelcyclearray[0]); 
		// Return Fuel cycle number
		for(int i=0;i<NumberOfCycles;i++){
			if(fuelcyclearray[0]==FuelCyclePricesYear[0][i]){
				CycleNumber=i;
			}
		}      

	}

	public void modifyCapacity(int reactor_type, int year) { /* function called if reprocessing throughput by tier > capacity by tier */

		int year_counter,type_to_replace_with,i,j;
		boolean success=false;


		type_to_replace_with=19;
		year_counter=year;
		while(!success) {
			if(reactor_type<0) 
				if (verbose) System.out.println("It's reactor type, in year "+year_counter);
			if(year_counter-START_YEAR < 0) 
				if (verbose) System.out.println("It's start year, "+year_counter+", rx type "+reactor_type);
			if(facilitiesAdded[reactor_type][year_counter-START_YEAR]>0) {
				success=true;
				facilitiesAdded[reactor_type][year_counter-START_YEAR]--;
				if(bug) 
					if (verbose) System.out.println("Removed "+REACTORNAMES[reactor_type]+" commissioned in "+year_counter);
				//type_to_replace_with=ReplaceWithType[reactor_type];
				for(j=0; j<YearReplaceWithTypeSpecified[reactor_type].length; j++) {
					if (year_counter >= YearReplaceWithTypeSpecified[reactor_type][j]) type_to_replace_with=ReplaceWithType[reactor_type][j];
				}
				for(j=year_counter-START_YEAR; j<Math.min(END_YEAR-START_YEAR+1,year_counter-START_YEAR+NewReactorLifetime); j++) {
					genCap[reactor_type][j]-=PLANT_SIZE[reactor_type];
					totalGenCap[j]-=PLANT_SIZE[reactor_type];
					frontEndCharges-=augmentFrontEndChargesnp(PLANT_SIZE[reactor_type],reactor_type,j);
					reactorCharges-=augmentReactorCharges(PLANT_SIZE[reactor_type],reactor_type);
				}
			}
			else year_counter--;
		}
		boolean added_one=false;
		for(i=year_counter-START_YEAR; i<END_YEAR-START_YEAR+1; i++) {
			while(totalGenCap[i]<targetGenCap[i]) {
				if(PLANT_SIZE[type_to_replace_with] < 0.5*(targetGenCap[i]-totalGenCap[i])) {
					if(bug) 
						if (verbose) System.out.println("Replaced with "+REACTORNAMES[type_to_replace_with]+" in "+(i+START_YEAR));
					facilitiesAdded[type_to_replace_with][i]++;
					for(j=i; j<Math.min(END_YEAR-START_YEAR+1,i+NewReactorLifetime); j++) {
						genCap[type_to_replace_with][j]+=PLANT_SIZE[type_to_replace_with];
						totalGenCap[j]+=PLANT_SIZE[type_to_replace_with];
						frontEndCharges+=augmentFrontEndChargesnp(PLANT_SIZE[type_to_replace_with],type_to_replace_with,j);   ////////////////not sure: i or j?
						reactorCharges+=augmentReactorCharges(PLANT_SIZE[type_to_replace_with],type_to_replace_with);
					}
					added_one=true;
				}
				else break;
				if(added_one) break;
			}
			if(added_one) break;
		} 

		frontEndCharges=0.;
		reactorCharges=0.;
		for(j=0; j<REACTORNAMES.length; j++) {
			for(i=0; i<END_YEAR-START_YEAR+1; i++) {
				//totalGenCap[i]+=genCap[j][i];
				SFGenerated[j][i]=genCap[j][i]*capacityToMass(j);
				totalEnergyGenerated=genCap[j][i]*365.*AVAILABILITY[j];
				NatUraniumUse[j][i]=SFGenerated[j][i]*FRONTENDMASS[j][0];        // Natural Uranium use by reactor type and year
			} 
		}
	}
	
	/* Version for @birdybird's dissertation
	 * Don't pass the PrintWriter
	 */
	public double augmentBackEndDDCharges(int reactor_type, double capacity, int year) {

		double charge=0.;
		double pvf;

		for(int i=0; i<BACKENDTECH.length; i++) {
			pvf=Math.exp(-RiskFreeROR*TIMELAG_BACKEND_DD[i]);
			
			if (i==3) {
				if (DisposalGrowthCoeff>EPS) {
					DisposedAmount[0][year] = (year==0) ? 0. : DisposedAmount[0][year-1]; 
					DisposedAmount[0][year] += (capacity*capacityToMass(reactor_type)/1000000)*BACKENDMASS_DD[i];
					double dummy = 1.,delta=0.;

					delta = (year==0) ? DisposedAmount[0][year] : DisposedAmount[0][year] - DisposedAmount[0][year-1];
					if (delta>EPS) {
						dummy = (year==0) ? Math.pow(1+DisposalGrowthCoeff, DisposedAmount[0][year]) - 1 : Math.pow(1+DisposalGrowthCoeff, DisposedAmount[0][year]) - Math.pow(1+DisposalGrowthCoeff, DisposedAmount[0][year-1]);
						dummy /= Math.log(1+DisposalGrowthCoeff);
						dummy /= delta;
					} else {
						dummy = (year==0) ? Math.pow(1+DisposalGrowthCoeff, 0) : DisposalCostModifier[0][year-1];
					}
					DisposalCostModifier[0][year] = dummy;
				} else {
					DisposalCostModifier[0][year] = 1.;
				}
				
				charge+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf*DisposalCostModifier[0][year];
				if(add_to_integrals) integratedCosts[reactor_type][1+FRONTENDTECH.length+i]+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*socialPVF*DisposalCostModifier[0][year];
			}

			charge+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf;
			if(add_to_integrals) integratedCosts[reactor_type][1+FRONTENDTECH.length+i]+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*socialPVF;
			
		}
		charge=charge*capacity*capacityToMass(reactor_type);
		return(charge);
	}
	
	public double augmentBackEndDDCharges(int reactor_type, double capacity, PrintWriter output_writer, int year) {

		double charge=0.;
		double pvf;

		for(int i=0; i<BACKENDTECH.length; i++) {
			pvf=Math.exp(-RiskFreeROR*TIMELAG_BACKEND_DD[i]);
			
			if (i==3) {
				if (DisposalGrowthCoeff>EPS) {
					DisposedAmount[0][year] = (year==0) ? 0. : DisposedAmount[0][year-1]; 
					DisposedAmount[0][year] += (capacity*capacityToMass(reactor_type)/1000000)*BACKENDMASS_DD[i];
					double dummy = 1.,delta=0.;

					delta = (year==0) ? DisposedAmount[0][year] : DisposedAmount[0][year] - DisposedAmount[0][year-1];
					if (delta>EPS) {
						dummy = (year==0) ? Math.pow(1+DisposalGrowthCoeff, DisposedAmount[0][year]) - 1 : Math.pow(1+DisposalGrowthCoeff, DisposedAmount[0][year]) - Math.pow(1+DisposalGrowthCoeff, DisposedAmount[0][year-1]);
						dummy /= Math.log(1+DisposalGrowthCoeff);
						dummy /= delta;
					} else {
						dummy = (year==0) ? Math.pow(1+DisposalGrowthCoeff, 0) : DisposalCostModifier[0][year-1];
					}
					DisposalCostModifier[0][year] = dummy;
				} else {
					DisposalCostModifier[0][year] = 1.;
				}
				
				charge+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf*DisposalCostModifier[0][year];
				if(add_to_integrals) integratedCosts[reactor_type][1+FRONTENDTECH.length+i]+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*socialPVF*DisposalCostModifier[0][year];
				if(print_cost_coefficients) if(printOutputFiles) output_writer.print((BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*DisposalCostModifier[0][year])+" ");
			}

			charge+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf;
			if(add_to_integrals) integratedCosts[reactor_type][1+FRONTENDTECH.length+i]+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*socialPVF;
			if(print_cost_coefficients) if(printOutputFiles) output_writer.print((BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type))+" ");
			
		}
		charge=charge*capacity*capacityToMass(reactor_type);
		return(charge);
	}
	
	public double augmentBackEndDDChargesnp(int reactor_type, double capacity) { /* overload from Estonians */

		double charge=0.;
		double pvf;

		for(int i=0; i<BACKENDTECH.length; i++) {
			pvf=Math.exp(-RiskFreeROR*TIMELAG_BACKEND_DD[i]);
			charge+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf;
			if(add_to_integrals) integratedCosts[reactor_type][1+FRONTENDTECH.length+i]+=BACKENDMASS_DD[i]*unitCostsBE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*socialPVF;
		}
		charge=charge*capacity*capacityToMass(reactor_type);
		return(charge);
	}
	
	/* added by Birdy for proliferation metric calculation */
	public double getBackEndDDProliferation(int reactor_type, double capacity, int year_count) {
		
		double metric=0.;

		for(int i=0; i<BACKENDTECH.length; i++) {
			metric+=BACKENDMASS_DD[i]*back_end_proliferation[reactor_type][i];
		}
		
		metric=metric*capacity*capacityToMass(reactor_type);
		return(metric);
		
	}

	public int countSameTier(int reactor_type, int year) {
		
		int count=0;
		for (int n=0; n<REACTORNAMES.length; n++) {
			if (BELONGS_TO_TIER[n] == BELONGS_TO_TIER[reactor_type] && SFGenerated[n][year] > EPS) count += 1;
		}
		return (count);
	
	}
	
	/**
	 * getRepUnitCost returns the unit cost for tier 0 reprocessing for a given year
	 * 
	 * @param reactor_type : only tier 0 reactors
	 * @param tech_number : only reprocessing
	 * @param year : raw year (add START_YEAR) to get simulation year
	 * @param sf_reprocessed_by_tier[] : sf reprocessed for a given year indexed over the 3 tiers
	 * @return unitCost
	 */
	public double getRepUnitCost(int reactor_type, int year, double[] sf_reprocessed_by_tier) {
		
		double unitCost = 0.;
		double tech_capacity = 0.;
		double dummy_double = 0.;

		// determine the tier 0 reprocessing capacity for this year
		for (int i=0; i<ReprocessingAdditions[0]; i++) {
			if (year >= YearReprocessingSpecified[0][i]-START_YEAR) tech_capacity = specifiedTierCap[0][i];
		}
		
		int tech = repTechNo;
		
		if (modUnitCostsBE[repTechNo][0] == 0.) unitCost = unitCostsBE[reactor_type][tech];
		else {
			
			if (tech_capacity == 0) unitCost = 0.;
			else {

				double facilities = tech_capacity/BEPlantSize[tech]; //Plant size specified in MT/yr; convert to kg/yr

				if (sf_reprocessed_by_tier[0] < EPS) {

					if (genCap[reactor_type][year] > 0.) {
						dummy_double = ( modUnitCostsBE[tech][0] + modUnitCostsBE[tech][1] ) * facilities;
						unitCost = dummy_double/countSameTier(reactor_type, year);
					} else unitCost = 0.;

				} else {

					dummy_double = ( (modUnitCostsBE[tech][0] + modUnitCostsBE[tech][1])/sf_reprocessed_by_tier[0] ) * facilities;
					dummy_double += modUnitCostsBE[tech][2];
					unitCost = dummy_double*BACKENDMASS[reactor_type][tech];

				}

			}

		}

		return (unitCost);

	}
	
	public double augmentBackEndRepCharges(int reactor_type, double capacity, int year_of_discharge, int year_of_reprocessing, PrintWriter output_writer) {

		double charge=0.;
		double pvf;

		double repYear = year_of_reprocessing-year_of_discharge;
		
		TIMELAG_BACKEND[0]=BACKEND_TIMELAG[0];
		TIMELAG_BACKEND[1]=BACKEND_TIMELAG[1];
		TIMELAG_BACKEND[2]=repYear;
		TIMELAG_BACKEND[3]=BACKEND_TIMELAG[3];
		TIMELAG_BACKEND[4]=repYear+BACKEND_TIMELAG[4];
		TIMELAG_BACKEND[5]=repYear+BACKEND_TIMELAG[5];
		TIMELAG_BACKEND[6]=repYear+BACKEND_TIMELAG[6];
		for(int i=0; i<BACKENDTECH.length; i++) {
			pvf=Math.exp(-TIMELAG_BACKEND[i]*RiskFreeROR);
			charge+=BACKENDMASS[reactor_type][i]*unitCostsBE[reactor_type][i]*pvf;
			if(add_to_integrals) integratedCosts[reactor_type][1+FRONTENDTECH.length+i]+=BACKENDMASS[reactor_type][i]*unitCostsBE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type)*socialPVF;
			if(print_cost_coefficients) if(printOutputFiles) output_writer.print((BACKENDMASS[reactor_type][i]*unitCostsBE[reactor_type][i]*pvf*capacity*capacityToMass(reactor_type))+" ");
		}
		charge=charge*capacity*capacityToMass(reactor_type);
		return(charge);
	}    
	
	/* Version for @birdybird's dissertation
	 * Don't pass the PrintWriter
	 */
	public double augmentBackEndRepCharges(int reactor_type, double capacity, int year_of_discharge, int year_of_reprocessing) {

		double charge=0.;
		double pvf;

		double repYear = year_of_reprocessing-year_of_discharge;
		
		TIMELAG_BACKEND[0]=BACKEND_TIMELAG[0];
		TIMELAG_BACKEND[1]=BACKEND_TIMELAG[1];
		TIMELAG_BACKEND[2]=repYear;
		TIMELAG_BACKEND[3]=BACKEND_TIMELAG[3];
		TIMELAG_BACKEND[4]=repYear+BACKEND_TIMELAG[4];
		TIMELAG_BACKEND[5]=repYear+BACKEND_TIMELAG[5];
		TIMELAG_BACKEND[6]=repYear+BACKEND_TIMELAG[6];
		for(int i=0; i<BACKENDTECH.length; i++) {
			pvf=Math.exp(-TIMELAG_BACKEND[i]*RiskFreeROR);
			charge+=BACKENDMASS[reactor_type][i]*unitCostsBE[reactor_type][i]*pvf;
		}
		charge=charge*capacity*capacityToMass(reactor_type);
		return(charge);
	}   
	
	/* added by Birdy for proliferation metric calculation */
	public double getBackEndRepProliferation(int reactor_type, double capacity) {
		
		double metric=0.;

		for(int i=0; i<BACKENDTECH.length; i++) {
			metric+=BACKENDMASS[reactor_type][i]*back_end_proliferation[reactor_type][i];
		}
		
		metric=metric*capacity*capacityToMass(reactor_type);
		return(metric);
		
	}

	public void assessBackEndCosts() { /* Estonians */

		int i,j;
		double sf_dd;

		for(i=0; i<SFGenerated.length; i++) {
			for(j=0; j<SFGenerated[0].length; j++) {
				if(SFGenerated[i][j]>EPS) {
					sf_dd=SFGenerated[i][j]-SFReprocessed[i][j];
					backEndCharges+=augmentBackEndDDChargesnp(i,sf_dd/SFGenerated[i][j]*genCap[i][j]);
					backEndCharges+=augmentBackEndRepCharges(i,SFReprocessed[i][j]/SFGenerated[i][j]*genCap[i][j],j+START_YEAR,yearSFReprocessed[i][j]);
				}
			}
		}
	}

	public double vitrifyAndDisposeOfTRU(double amount_of_tru) {
		return(amount_of_tru*TRUVITDISPCOST);
	}

	public double getAverageCOE() { /* Estonians */
		if (verbose) System.out.println((frontEndCharges+backEndCharges+reactorCharges)/totalEnergyGenerated/24*100);
		return((frontEndCharges+backEndCharges+reactorCharges)/totalEnergyGenerated/24*100);
	}

	public void setLegacySFStockpile() {

		for(int i=0; i<END_YEAR-START_YEAR+1; i++) legacySFStockpile[i]=LegacySF;
	}

	public void assessTRUStorageCosts() { /* Estonians */

		int i;
		double tru_stored;

		tru_stored=0.;
		for(i=0; i<END_YEAR-START_YEAR+1; i++) {
			tru_stored=puStockpile[i]+maStockpile[i];
			TRUStorageCost[i]=tru_stored*TRUSTORCOST;
			backEndCharges+=TRUStorageCost[i];
		}
		backEndCharges+=tru_stored*TRUVITDISPCOST;
	}

	// This function is called before printAnnualReports and is needed to dynamically calculate the reprocessing unit cost
	public void getReprocessingInfo(boolean dynamic_unit_cost) {

		int i,j,k,l,m;
		double[] sf_reprocessed_by_tier=new double[3];
		double[] sf_reprocessed_by_reactor=new double[REACTORNAMES.length];
		int year_of_reprocessing;
		double amount_reprocessed_this_year;
		double tempCost=0;

		if (dynamic_unit_cost) {
			try {
				String user_dir = System.getProperty("user.dir");
				File repUnitCost_target = new File(user_dir+File.separatorChar+"repUnitCosts");

				if(repUnitCost_target.exists()) repUnitCost_target.delete();
				FileWriter output_filewriter_6 = new FileWriter(repUnitCost_target);
				PrintWriter output_writer_6 = new PrintWriter(output_filewriter_6);

				// need to get the sf reprocessed by tier ahead of time..
				for(i=0; i<END_YEAR-START_YEAR+1; i++) {
					int count = 0;
					double repUnitCost = 0.;
					for(j=0; j<sf_reprocessed_by_tier.length; j++) sf_reprocessed_by_tier[j]=0.;
					if(i>0) sf_reprocessed_by_tier[0]-=(legacySFStockpile[i]-legacySFStockpile[i-1]);
					for(j=0; j<REACTORNAMES.length; j++) {                                     
						for(k=0; k<i+1; k++) {
							if(SFGenerated[j][k] > EPS) {
								l=AmountOfSFReprocessed[j][k].size()/2;
								for(m=0; m<l; m++) {
									year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(2*m)).intValue();
									amount_reprocessed_this_year=((Double)AmountOfSFReprocessed[j][k].elementAt(2*m+1)).doubleValue();
									if(year_of_reprocessing==i+START_YEAR) {
										sf_reprocessed_by_tier[BELONGS_TO_TIER[j]]+=amount_reprocessed_this_year;
									}
								}
							}
						}
					}
					for (j = 0; j < NumberOfTiers; j++) SFReprocessedByTier[j][i] = sf_reprocessed_by_tier[j];

					// For now! Only calculates the unit cost for tier 0 reactors
					for (j = 0; j < REACTORNAMES.length; j++) {
						if (BELONGS_TO_TIER[j] == 0) {
							reprocessingUnitCost[j][i] = getRepUnitCost(j, i, sf_reprocessed_by_tier);	
							repUnitCost += reprocessingUnitCost[j][i];
							if (reprocessingUnitCost[j][i] > EPS) count += 1;
						}
					}
					if (repUnitCost!=0.) repUnitCost/=(double)count;

					output_writer_6.print(repUnitCost+"    ");
				}
				output_writer_6.println();
				output_writer_6.close();

			}
			catch(IOException ioe) {
				System.out.println("Error writing output file.");
				System.exit(-1);
			}
		} else if (!dynamic_unit_cost) {

			for(i=0; i<END_YEAR-START_YEAR+1; i++) {
				int count = 0;
				double repUnitCost = 0.;
				for(j=0; j<sf_reprocessed_by_tier.length; j++) sf_reprocessed_by_tier[j]=0.;
				if(i>0) sf_reprocessed_by_tier[0]-=(legacySFStockpile[i]-legacySFStockpile[i-1]);
				for(j=0; j<REACTORNAMES.length; j++) {                                     
					for(k=0; k<i+1; k++) {
						if(SFGenerated[j][k] > EPS) {
							l=AmountOfSFReprocessed[j][k].size()/2;
							for(m=0; m<l; m++) {
								year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(2*m)).intValue();
								amount_reprocessed_this_year=((Double)AmountOfSFReprocessed[j][k].elementAt(2*m+1)).doubleValue();
								if(year_of_reprocessing==i+START_YEAR) {
									sf_reprocessed_by_tier[BELONGS_TO_TIER[j]]+=amount_reprocessed_this_year;
								}
							}
						}
					}
				}
				for (j=0; j<NumberOfTiers; j++) SFReprocessedByTier[j][i] = sf_reprocessed_by_tier[j];
			}
			
		}

	}

	
	public void printOutput() { /* Estonians */

		int i,j;

		for(i=0; i<END_YEAR-START_YEAR+1; i++) {
			if (verbose) System.out.println("YEAR "+(i+START_YEAR)+" legacy SF: "+legacySFStockpile[i]+" PuStor: "+puStockpile[i]+" MAStor: "+maStockpile[i]);
			for(j=0; j<REACTORNAMES.length; j++) {
				if (verbose) System.out.println(genCap[j][i]+" "+SFGenerated[j][i]+" "+SFReprocessed[j][i]+" "+yearSFReprocessed[j][i]);
			}
		}
	}

	public void instantiateReprocessedSFVectors() {

		int i,j;
		for(i=0; i<REACTORNAMES.length; i++) {
			for (j=0; j<FRONTENDTECH.length+BACKENDTECH.length+2;j++) {
				integratedCosts[i][j]=0.;
			}
		}
		for(j=0; j<END_YEAR-START_YEAR+1; j++) {
			puStockpile[j]=0.;
			maStockpile[j]=0.;
			totalPuCharged[j]=0.;
			totalMACharged[j]=0.;
			puUnavailable[j] = 0.;
			maUnavailable[j] = 0.;
			puAvailable[j] = 0.;
			maAvailable[j] = 0.;
			for(i=0; i<REACTORNAMES.length; i++) {
				AmountOfSFReprocessed[i][j]=new Vector();
				AmountOfSFReprocessed[i][j].clear();
				SFReprocessed[i][j]=0.;
				SFReprocessedByReactor[i][j]=0.;
				puDemand[i][j]=0.;
				maDemand[i][j]=0.;
				yearSFReprocessed[i][j]=END_YEAR+1;
			}
		}
		for(i=0; i<ActinideWasteStream.length; i++) {
			for(j=0; j<ActinideWasteStream[0].length; j++) ActinideWasteStream[i][j]=0.;
		}
	}
	

	public void runTheSim(int first_reactor_build_decision, int second_reactor_build_decision, int final_reactor_build_decision) {

		boolean success=false; 

		/* For initializing the DecisionMaker functionality */

		reInitializeEverything();
		getPossibleCycles();
		massEquilibriumClculation(); 
		setBuildOrders();
		getMassConv();
		assignGenerationCapacity(); 
		/* original VEGAS implementation */
		//getReactorCharges();
		while(!success) {
			instantiateReprocessedSFVectors();
			if(bug) 
				if (verbose) System.out.println("VEGAS Executing...");
			setLegacySFStockpile();
			getPuDemand();
			getMADemand();
			success=reprocessSF();
		}

		int n_rx;
		int chosen_reprocessing_cost, waste_disposal_cost, sfr_capital_cost, htgr_capital_cost, chosen_capital_subsidy;
		double[] metrics = new double[4];
		
		if (only_one) {
			
			first_reactor_build_decision = robustInts[0];
			second_reactor_build_decision = robustInts[1];
			final_reactor_build_decision = robustInts[2];
			chosen_reprocessing_cost = robustInts[3];
			chosen_capital_subsidy = robustInts[4];
			waste_disposal_cost = robustInts[5];
			sfr_capital_cost = robustInts[6];
			htgr_capital_cost = robustInts[7];
			
			double[][] ma_waste_stream = new double[ActinideWasteStream.length][];
			for (int i = 0; i < ma_waste_stream.length; i++) {
				ma_waste_stream[i] = new double[ActinideWasteStream[i].length];
				System.arraycopy(ActinideWasteStream[i], 0, ma_waste_stream[i], 0, ActinideWasteStream[i].length);
			}

			double[] pu_stockpile = puStockpile;

			DEFAULTBECOST[2] = ChosenReprocessingCost[chosen_reprocessing_cost];
			DEFAULTBECOST[3] = DisposalCost[waste_disposal_cost][0];
			DEFAULTBECOST[6] = DisposalCost[waste_disposal_cost][1];

			//TODO only change for default costs?
			for (n_rx=0; n_rx<REACTORNAMES.length; n_rx++) {
				unitCostsBE[n_rx][2] = ChosenReprocessingCost[chosen_reprocessing_cost];
				unitCostsBE[n_rx][3] = DisposalCost[waste_disposal_cost][0];
				unitCostsBE[n_rx][6] = DisposalCost[waste_disposal_cost][1];
			}

			CAPITALCOST[0] = LWRCapitalCost;
			NOAKCapitalCost[0] = LWRCapitalCost;
			NOAKCapitalCost[1] = HTGRCapitalCost[htgr_capital_cost];
			NOAKCapitalCost[2] = SFRCapitalCost[sfr_capital_cost];

			yearlyReactorCharge = getUnitReactorCharges(ChosenCapitalSubsidy[chosen_capital_subsidy]);

			for (int i = 0; i < ActinideWasteStream.length; i++) System.arraycopy(ma_waste_stream[i], 0, ActinideWasteStream[i], 0, ma_waste_stream[i].length);

			System.out.print("... chosen reprocessing cost " + chosen_reprocessing_cost + ", chosen capital subsidy " + chosen_capital_subsidy + "\n");
			System.out.print("... " + waste_disposal_cost + " waste disposal cost, " + htgr_capital_cost + " htgr capital cost, and " + sfr_capital_cost + "sfr capital cost" + "\n");
			yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][final_reactor_build_decision] = yearlyAnnualReports();
			for (int m=0; m<4; m++) {
				for (int year=0; year<END_YEAR-START_YEAR+1-NewReactorLifetime; year++) {
					metrics[m] += yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][final_reactor_build_decision][year][m];
				}
			}
			System.out.print("The metrics are " + metrics[0] + " for G's LCOE, " + metrics[1] + " for the decay heat, " + metrics[2] + " for the proliferation resistance, and " + metrics[3] + " for U's LCOE." + "\n");

			for (int i = 0; i < ActinideWasteStream.length; i++) System.arraycopy(ma_waste_stream[i], 0, ActinideWasteStream[i], 0, ma_waste_stream[i].length);
			puStockpile = pu_stockpile;
			
		} else if (!only_one) {
			double[][] ma_waste_stream = new double[ActinideWasteStream.length][];
			for (int i = 0; i < ma_waste_stream.length; i++) {
				ma_waste_stream[i] = new double[ActinideWasteStream[i].length];
				System.arraycopy(ActinideWasteStream[i], 0, ma_waste_stream[i], 0, ActinideWasteStream[i].length);
			}

			for (chosen_reprocessing_cost=0; chosen_reprocessing_cost<ChosenReprocessingCost.length; chosen_reprocessing_cost++) {
				for (waste_disposal_cost=0; waste_disposal_cost<DisposalCost.length; waste_disposal_cost++) {
					for (sfr_capital_cost=0; sfr_capital_cost<SFRCapitalCost.length; sfr_capital_cost++) {
						for (htgr_capital_cost=0; htgr_capital_cost<HTGRCapitalCost.length; htgr_capital_cost++) {
							for (chosen_capital_subsidy=0; chosen_capital_subsidy<ChosenCapitalSubsidy.length; chosen_capital_subsidy++) {

								DEFAULTBECOST[2] = ChosenReprocessingCost[chosen_reprocessing_cost];
								DEFAULTBECOST[3] = DisposalCost[waste_disposal_cost][0];
								DEFAULTBECOST[6] = DisposalCost[waste_disposal_cost][1];

								//TODO only change for default costs?
								for (n_rx=0; n_rx<REACTORNAMES.length; n_rx++) {
									unitCostsBE[n_rx][2] = ChosenReprocessingCost[chosen_reprocessing_cost];
									unitCostsBE[n_rx][3] = DisposalCost[waste_disposal_cost][0];
									unitCostsBE[n_rx][6] = DisposalCost[waste_disposal_cost][1];
								}

								CAPITALCOST[0] = LWRCapitalCost;
								NOAKCapitalCost[0] = LWRCapitalCost;
								NOAKCapitalCost[1] = HTGRCapitalCost[htgr_capital_cost];
								NOAKCapitalCost[2] = SFRCapitalCost[sfr_capital_cost];

								yearlyReactorCharge = getUnitReactorCharges(ChosenCapitalSubsidy[chosen_capital_subsidy]);

								for (int i = 0; i < ActinideWasteStream.length; i++) System.arraycopy(ma_waste_stream[i], 0, ActinideWasteStream[i], 0, ma_waste_stream[i].length);

								System.out.print("... chosen reprocessing cost " + chosen_reprocessing_cost + ", chosen capital subsidy " + chosen_capital_subsidy + "\n");
								System.out.print("... " + waste_disposal_cost + " waste disposal cost, " + htgr_capital_cost + " htgr capital cost, and " + sfr_capital_cost + "sfr capital cost" + "\n");
								yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][final_reactor_build_decision] = yearlyAnnualReports();
								for (int m=0; m<4; m++) {
									for (int year=0; year<END_YEAR-START_YEAR+1-NewReactorLifetime; year++) {
										metrics[m] += yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][final_reactor_build_decision][year][m];
									}
								}
								System.out.print("The metrics are " + metrics[0] + " for G's LCOE, " + metrics[1] + " for the decay heat, " + metrics[2] + " for the proliferation resistance, and " + metrics[3] + " for U's LCOE." + "\n");

								for (int i = 0; i < ActinideWasteStream.length; i++) System.arraycopy(ma_waste_stream[i], 0, ActinideWasteStream[i], 0, ma_waste_stream[i].length);

							}
						}
					}
				}
			}
		}

		boolean dynamic_unit_cost = false;
		getReprocessingInfo(dynamic_unit_cost);

		/* functions added by Estonians */
		/* assessBackEndCosts();
		assessTRUStorageCosts();
		setBuildOrders();
		        for(int i=20;i<END_YEAR-START_YEAR+1;i=i+10){
		            getAllCyclesPrices(i);
		        }

		printOutput();
		averageCOE=getAverageCOE(); */

		/* added for birdy's thesis; penalizes underutilization of reprocessing capacity */
		// getReprocessingInfo();

		/* original VEGAS implementation */
		//printAnnualReports();

	}

	/**
	 * @param args the command line arguments
	 * @return 
	 */
	public static void main (String args[]) {

		int i,j,k,l,m,n;
		int first_reactor_build_decision, second_reactor_build_decision, final_reactor_build_decision;
		int waste_disposal_cost, sfr_capital_cost, htgr_capital_cost;
		int chosen_reprocessing_cost, chosen_capital_subsidy;
		int year;
		double[] metrics = new double[4];
		

		try {

			String user_dir = System.getProperty("user.dir");

			if (only_one) {


				/* robustInts{0,1,2,3,4,5,6,7}
				 * 0 = U's first reactor build decision
				 * 1 = U's second reactor build decision
				 * 2 = U's third reactor build decision
				 * 3 = G's chosen reprocessing cost
				 * 4 = G's chosen capital subsidy
				 * 5 = waste disposal cost outcome
				 * 6 = htgr capital cost outcome
				 * 7 = sfr capital cost outcome
				 */

				first_reactor_build_decision = robustInts[0];
				second_reactor_build_decision = robustInts[1];
				final_reactor_build_decision = robustInts[2];
				chosen_reprocessing_cost = robustInts[3];
				chosen_capital_subsidy = robustInts[4];
				waste_disposal_cost = robustInts[5];
				sfr_capital_cost = robustInts[6];
				htgr_capital_cost = robustInts[7];

				printNFCParamComboFile(FirstReactorBuildDecision[first_reactor_build_decision],SecondReactorBuildDecision[second_reactor_build_decision],FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]);
				printReactorParamFile(FirstReactorBuildDecision[first_reactor_build_decision],SecondReactorBuildDecision[second_reactor_build_decision],FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]);
				System.out.print("Running the sim with first reactor build decision " + FirstReactorBuildDecision[first_reactor_build_decision] + ", second reactor build decision " + SecondReactorBuildDecision[second_reactor_build_decision] + ", final reactor build decision " + final_reactor_build_decision + "\n");
				VEGAS mySim = new VEGAS();
				mySim.runTheSim(first_reactor_build_decision,second_reactor_build_decision,FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]);

				// THIS SHOULD ALL GO IN RUNTHESIM
				for (int metric_no=0; metric_no<metrics.length; metric_no++) {
					for (year=0; year<END_YEAR-START_YEAR+1-NewReactorLifetime; year++) {
						metrics[metric_no] += yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][final_reactor_build_decision][year][metric_no];
					}
				}

				/* Main decision making results */
				File output_target = new File(user_dir+File.separatorChar+"DecisionMakingResults.txt");
				if(output_target.exists()) output_target.delete();
				FileWriter output_filewriter = new FileWriter(output_target);
				PrintWriter output_writer = new PrintWriter(output_filewriter);
				output_writer.print("first_reactor_decision second_reactor_decision final_reactor_decision chosen_reprocessing_cost chosen_capital_subsidy disposal_cost htgr_capital_cost sfr_capital_cost g_lcoe decay_heat proliferation_resistance u_lcoe");
				output_writer.print("\n");
				output_writer.print(first_reactor_build_decision + " " + second_reactor_build_decision + " " + final_reactor_build_decision + " " + chosen_reprocessing_cost + " " + chosen_capital_subsidy + " "+ waste_disposal_cost + " " + htgr_capital_cost + " " + sfr_capital_cost + " " + metrics[0] + " " + metrics[1] + " " + metrics[2] + " " + metrics[3] + "\n");
				output_writer.close();


				File output_target_glcoe = new File(user_dir+File.separatorChar+"YearlyGLCOE.txt");
				if(output_target_glcoe.exists()) output_target_glcoe.delete();
				FileWriter output_filewriter_glcoe = new FileWriter(output_target_glcoe);
				PrintWriter output_writer_glcoe = new PrintWriter(output_filewriter_glcoe);
				output_writer_glcoe.print("first_reactor_decision second_reactor_decision final_reactor_decision chosen_reprocessing_cost chosen_capital_subsidy disposal_cost htgr_capital_cost sfr_capital_cost ");
				output_writer_glcoe.print(first_reactor_build_decision + " " + second_reactor_build_decision + " " + final_reactor_build_decision + " " + chosen_reprocessing_cost + " " + chosen_capital_subsidy + " "+ waste_disposal_cost + " " + htgr_capital_cost + " " + sfr_capital_cost + "\n");
				output_writer_glcoe.print("year glcoe" + "\n");
				for (year=0; year<END_YEAR-START_YEAR+1-NewReactorLifetime; year++) {
					output_writer_glcoe.print((year+START_YEAR) + " " + yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]][year][0]);
					output_writer_glcoe.print("\n");
				}
				output_writer_glcoe.close();

				/* Decay Heat */
				File output_target_decayheat = new File(user_dir+File.separatorChar+"YearlyDecayHeat.txt");
				if(output_target_decayheat.exists()) output_target_decayheat.delete();
				FileWriter output_filewriter_decayheat = new FileWriter(output_target_decayheat);
				PrintWriter output_writer_decayheat = new PrintWriter(output_filewriter_decayheat);
				output_writer_decayheat.print("first_reactor_decision second_reactor_decision final_reactor_decision chosen_reprocessing_cost chosen_capital_subsidy disposal_cost htgr_capital_cost sfr_capital_cost ");
				output_writer_decayheat.print(first_reactor_build_decision + " " + second_reactor_build_decision + " " + final_reactor_build_decision + " " + chosen_reprocessing_cost + " " + chosen_capital_subsidy + " "+ waste_disposal_cost + " " + htgr_capital_cost + " " + sfr_capital_cost + "\n");
				output_writer_decayheat.print("year decayheat" + "\n");
				for (year=0; year<END_YEAR-START_YEAR+1-NewReactorLifetime; year++) {
					output_writer_decayheat.print((year+START_YEAR) + " " + yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]][year][1]);
					output_writer_decayheat.print("\n");
				}
				output_writer_decayheat.close();

				/* Proliferation Resistance */
				File output_target_proliferationresistance = new File(user_dir+File.separatorChar+"YearlyProliferationResistance.txt");
				if(output_target_proliferationresistance.exists()) output_target_proliferationresistance.delete();
				FileWriter output_filewriter_proliferationresistance = new FileWriter(output_target_proliferationresistance);
				PrintWriter output_writer_proliferationresistance = new PrintWriter(output_filewriter_proliferationresistance);
				output_writer_proliferationresistance.print("first_reactor_decision second_reactor_decision final_reactor_decision chosen_reprocessing_cost chosen_capital_subsidy disposal_cost htgr_capital_cost sfr_capital_cost ");
				output_writer_proliferationresistance.print(first_reactor_build_decision + " " + second_reactor_build_decision + " " + final_reactor_build_decision + " " + chosen_reprocessing_cost + " " + chosen_capital_subsidy + " "+ waste_disposal_cost + " " + htgr_capital_cost + " " + sfr_capital_cost + "\n");
				output_writer_proliferationresistance.print("year proliferationresistance" + "\n");
				for (year=0; year<END_YEAR-START_YEAR+1-NewReactorLifetime; year++) {
					output_writer_proliferationresistance.print((year+START_YEAR) + " " + yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]][year][2]);
					output_writer_proliferationresistance.print("\n");
				}
				output_writer_proliferationresistance.close();


				/* Player U LCOE */
				File output_target_ulcoe = new File(user_dir+File.separatorChar+"YearlyULCOE.txt");
				if(output_target_ulcoe.exists()) output_target_ulcoe.delete();
				FileWriter output_filewriter_ulcoe = new FileWriter(output_target_ulcoe);
				PrintWriter output_writer_ulcoe = new PrintWriter(output_filewriter_ulcoe);
				output_writer_ulcoe.print("first_reactor_decision second_reactor_decision final_reactor_decision chosen_reprocessing_cost chosen_capital_subsidy disposal_cost htgr_capital_cost sfr_capital_cost ");
				output_writer_ulcoe.print(first_reactor_build_decision + " " + second_reactor_build_decision + " " + final_reactor_build_decision + " " + chosen_reprocessing_cost + " " + chosen_capital_subsidy + " "+ waste_disposal_cost + " " + htgr_capital_cost + " " + sfr_capital_cost + "\n");
				output_writer_ulcoe.print("year ulcoe" + "\n");
				for (year=0; year<END_YEAR-START_YEAR+1-NewReactorLifetime; year++) {
					output_writer_ulcoe.print(year + " " + yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]][year][3]);
					output_writer_ulcoe.print("\n");
				}
				output_writer_ulcoe.close();

				File output_target_wastequantities = new File(user_dir+File.separatorChar+"WasteQuantities.txt");
				if (output_target_wastequantities.exists()) output_target_wastequantities.delete();
				FileWriter output_filewriter_wastequantities = new FileWriter(output_target_wastequantities);
				PrintWriter output_writer_wastequantities = new PrintWriter(output_filewriter_wastequantities);
				output_writer_wastequantities.print("first_reactor_decision second_reactor_decision final_reactor_decision chosen_reprocessing_cost chosen_capital_subsidy disposal_cost htgr_capital_cost sfr_capital_cost ");
				output_writer_wastequantities.print(first_reactor_build_decision + " " + second_reactor_build_decision + " " + final_reactor_build_decision + " " + chosen_reprocessing_cost + " " + chosen_capital_subsidy + " "+ waste_disposal_cost + " " + htgr_capital_cost + " " + sfr_capital_cost + "\n");
				output_writer_wastequantities.print("year lwr_wastegenerated lwr_wastereprocessed_byyear lwr_wastereprocessed htgr_wastegenerated htgr_wastereprocessed_byyear htgr_wastereprocessed sfr_wastegenerated sfr_wastereprocessed_byyear sfr_wastereprocessed" + "\n");
				for (year=0; year<END_YEAR-START_YEAR+1-NewReactorLifetime; year++) {
					output_writer_wastequantities.print((year+START_YEAR) + " ");
					for (int n_rx=0; n_rx<REACTORNAMES.length; n_rx++) {
						output_writer_wastequantities.print((SFGenerated[n_rx][year]/1000) + " " + (SFReprocessedByReactor[n_rx][year]/1000) + " " + (SFReprocessed[n_rx][year]/1000) + " ");
					}
					output_writer_wastequantities.print((SFReprocessedByTier[0][year]/1000));
					output_writer_wastequantities.print("\n");
				}
				output_writer_wastequantities.close();



			} else if (!only_one) {

				for (first_reactor_build_decision=0; first_reactor_build_decision<FirstReactorBuildDecision.length; first_reactor_build_decision++) {
					for (second_reactor_build_decision=0; second_reactor_build_decision<SecondReactorBuildDecision.length; second_reactor_build_decision++) {
						for (final_reactor_build_decision=0; final_reactor_build_decision<FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision].length; final_reactor_build_decision++) {
							printNFCParamComboFile(FirstReactorBuildDecision[first_reactor_build_decision],SecondReactorBuildDecision[second_reactor_build_decision],FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]);
							printReactorParamFile(FirstReactorBuildDecision[first_reactor_build_decision],SecondReactorBuildDecision[second_reactor_build_decision],FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]);
							System.out.print("Running the sim with first reactor build decision " + FirstReactorBuildDecision[first_reactor_build_decision] + ", second reactor build decision " + SecondReactorBuildDecision[second_reactor_build_decision] + " and final reactor build decision " + FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision] + "\n");
							VEGAS mySim = new VEGAS();
							mySim.runTheSim(first_reactor_build_decision,second_reactor_build_decision,FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]);
						}
					}
				}

				/* metric number:
				 * 0 = G's LCOE
				 * 1 = Decay Heat
				 * 2 = Proliferation Resistance
				 * 3 = U's LCOE
				 */

				// no.. this isn't the correct metric calculation
				for (chosen_reprocessing_cost=0; chosen_reprocessing_cost<ChosenReprocessingCost.length; chosen_reprocessing_cost++) {
					for (waste_disposal_cost=0; waste_disposal_cost<DisposalCost.length; waste_disposal_cost++) {
						for (first_reactor_build_decision=0; first_reactor_build_decision<FirstReactorBuildDecision.length; first_reactor_build_decision++) {
							for (chosen_capital_subsidy=0; chosen_capital_subsidy<ChosenCapitalSubsidy.length; chosen_capital_subsidy++) {
								for (second_reactor_build_decision=0; second_reactor_build_decision<SecondReactorBuildDecision.length; second_reactor_build_decision++) {
									for (htgr_capital_cost=0; htgr_capital_cost<HTGRCapitalCost.length; htgr_capital_cost++) {
										for (sfr_capital_cost=0; sfr_capital_cost<SFRCapitalCost.length; sfr_capital_cost++) {
											for (final_reactor_build_decision=0; final_reactor_build_decision<FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision].length; final_reactor_build_decision++) {
												for (int metric_no=0; metric_no<metrics.length; metric_no++) {
													for (year=0; year<END_YEAR-START_YEAR+1-NewReactorLifetime; year++) {
														metrics[metric_no] += yearlySimulationValues[chosen_reprocessing_cost][waste_disposal_cost][first_reactor_build_decision][chosen_capital_subsidy][second_reactor_build_decision][htgr_capital_cost][sfr_capital_cost][FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision]][year][metric_no];
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
				metrics[0] /= (END_YEAR-START_YEAR+1-NewReactorLifetime); /* get the yearly average of the LCOE */
				metrics[3] /= (END_YEAR-START_YEAR+1-NewReactorLifetime); /* get the yearly average of the LCOE */

				File output_target = new File(user_dir+File.separatorChar+"DecisionMakingResults.txt");

				if(output_target.exists()) output_target.delete();
				FileWriter output_filewriter = new FileWriter(output_target);
				PrintWriter output_writer = new PrintWriter(output_filewriter);

				output_writer.print("decision_one decision_two decision_three reprocessing_cost capital_subsidy disposal_cost htgr_cost sfr_cost g_lcoe heat_load nonprolif u_lcoe");
				output_writer.print("\n");

				for (chosen_reprocessing_cost=0; chosen_reprocessing_cost<ChosenReprocessingCost.length; chosen_reprocessing_cost++) {
					for (waste_disposal_cost=0; waste_disposal_cost<DisposalCost.length; waste_disposal_cost++) {
						for (first_reactor_build_decision=0; first_reactor_build_decision<FirstReactorBuildDecision.length; first_reactor_build_decision++) {
							for (chosen_capital_subsidy=0; chosen_capital_subsidy<ChosenCapitalSubsidy.length; chosen_capital_subsidy++) {
								for (second_reactor_build_decision=0; second_reactor_build_decision<SecondReactorBuildDecision.length; second_reactor_build_decision++) {
									for (htgr_capital_cost=0; htgr_capital_cost<HTGRCapitalCost.length; htgr_capital_cost++) {
										for (sfr_capital_cost=0; sfr_capital_cost<SFRCapitalCost.length; sfr_capital_cost++) {
											for (final_reactor_build_decision=0; final_reactor_build_decision<FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision].length; final_reactor_build_decision++) {
												output_writer.print(first_reactor_build_decision + " " + second_reactor_build_decision + " " + FinalReactorBuildDecision[first_reactor_build_decision][second_reactor_build_decision][final_reactor_build_decision] + " " + chosen_reprocessing_cost + " " + chosen_capital_subsidy + " "+ waste_disposal_cost + " " + htgr_capital_cost + " " + sfr_capital_cost + " " + metrics[0] + " " + metrics[1] + " " + metrics[2] + " " + metrics[3] + "\n");
											}
										}
									}
								}
							}
						}
					}
				}

				output_writer.close();

			}

		} catch (IOException e) {
			System.out.print("Error writing Decision Making results");
		}

		// TODO Here's the matlab plot script
		/* Uncomment the line below to display matlab plots */
		//Process p = Runtime.getRuntime().exec("cmd /c matlab -r makePlots");

	}
	
	public static void printNFCParamComboFile(int first_reactor_build_decision, int second_reactor_build_decision, int final_reactor_build_decision) {
		
		int i,j;
		boolean Match=false;

		try {

			String user_dir = System.getProperty("user.dir");
			TextReader file = new TextReader(user_dir+File.separatorChar+"NFC_parameters_Template.txt");
			String[] data = file.OpenFile();
			FileWriter write = new FileWriter(user_dir+File.separatorChar+"NFC_parameters.txt");

			PrintWriter print = new PrintWriter(write);

			String[] keys3 = {"3@LWR", "3@HTGR", "3@SFR"};
			String[] keys6 = {"6@LWR", "6@HTGR", "6@SFR"};
			String[] keys8 = {"8@LWR", "8@HTGR", "8@SFR"};
			String[] RepCapKey = {"@2025RepCap", "@2035RepCap"};

			for (i=0; i<data.length; i++) {
				
				Match=false;
				for (j=0; j<keys3.length; j++) {
					if (data[i].contains(keys3[j])) {
						writeTryToBuild(print,first_reactor_build_decision,j); Match=true; break;
					} else if (data[i].contains(keys6[j])) {
						writeTryToBuild(print,second_reactor_build_decision,j); Match=true; break;
					} else if (data[i].contains(keys8[j])) {
						writeTryToBuild(print,final_reactor_build_decision,j); Match=true; break;
					}
				}
				for (j=0; j<RepCapKey.length; j++) {
					if (data[i].contains(RepCapKey[j])) {
						writeReprocessingCapacity(print, RepCapKey[j], first_reactor_build_decision, second_reactor_build_decision, final_reactor_build_decision); Match=true; break;
					}
				}
				if (!Match) print.print(data[i]+"\n");

			}
			print.close();

		} catch(IOException e) {
			System.out.print("Error: Failed to write the NFC Parameter file from Decision Making input text");
		}	
		
	}
	
	public static void writeTryToBuild(PrintWriter print, int strategy, int n_rx) {
		
		if (strategy==0) {
			if (n_rx==0) print.print("		Percentage=100" + "\n");
			if (n_rx==1) print.print("		Percentage=0" + "\n");
			if (n_rx==2) print.print("		Percentage=0" + "\n");
		} else if (strategy==1) {
    		if (n_rx==0) print.print("		Percentage=0" + "\n");
    		if (n_rx==1) print.print("		Percentage=100" + "\n");
    		if (n_rx==2) print.print("		Percentage=0" + "\n");
    	} else if (strategy==2) {
    		if (n_rx==0) print.print("		Percentage=0" + "\n");
    		if (n_rx==1) print.print("		Percentage=0" + "\n");
    		if (n_rx==2) print.print("		Percentage=100" + "\n");
		} else if (strategy==3) {
    		if (n_rx==0) print.print("		Percentage=0" + "\n");
    		if (n_rx==1) print.print("		Percentage=0" + "\n");
    		if (n_rx==2) print.print("		Percentage=100" + "\n");
		}
	
	}
	
	public double[][] yearlyAnnualReports() {

		int i,j,k,l;
		double yearly_reactor, yearly_om, yearly_fe, yearly_be,yearly_sf_inv,yearly_pu,yearly_ma;
		double total_capacity, total_energy,sf_dd,yearly_coe,yearly_fc,tru_stored, yearly_sf_long_term,yearly_pu_long_term,yearly_ma_long_term;
		double yearly_sf_out_of_pile;
		double[] sf_reprocessed_by_tier=new double[3];
		double[] gen_cap_by_tier = new double[3];
		double[] max_gen_fraction_by_tier = new double[3];
		double[] sf_by_rx = new double[REACTORNAMES.length];
		int year_of_reprocessing;
		double amount_reprocessed_this_year;
		int start_integrating=2015;
		int stop_integrating=2050;
		double max_thruput=0.;
		double[] becomponent = new double[2]; // could be used later for proliferation metric
		
		/* for DecisionMaking */
		/* Proliferation metric for G */
		double[] proliferation_resistance=new double[END_YEAR-START_YEAR+1];
		/* COE metric for G and U */
		double[] backend_costs=new double[END_YEAR-START_YEAR+1];
		double[] frontend_and_reactor_costs=new double[END_YEAR-START_YEAR+1];
		/* Waste disposal metric for G */
		double[] decay_heat=new double[END_YEAR-START_YEAR+1];
		/* To pass objective function values to main */
		double[][] yearlyLeafValues = new double[(int) (END_YEAR-START_YEAR+1-NewReactorLifetime)][4];

		
		for(i=0; i<integratedCosts.length; i++) {
			for(j=0; j<integratedCosts[0].length; j++) integratedCosts[i][j]=0.;
		}
		for(i=0; i<max_gen_fraction_by_tier.length; i++) max_gen_fraction_by_tier[i]=0.;

		for(i=0; i<END_YEAR-START_YEAR+1; i++) {
			if(i+START_YEAR==EQUILIBRIUM_YEAR) print_cost_coefficients=true;
			else print_cost_coefficients=false;
			yearly_reactor=0.;
			yearly_om=0.;
			yearly_fe=0.;
			yearly_be=0.;
			total_capacity=0.;
			total_energy=0.;
			for(j=0; j<REACTORNAMES.length; j++) sf_by_rx[j]=0.;
			for(j=0; j<sf_reprocessed_by_tier.length; j++) sf_reprocessed_by_tier[j]=0.;
			for(j=0; j<gen_cap_by_tier.length; j++) gen_cap_by_tier[j]=0.;
			yearly_sf_long_term=legacySFStockpile[i];
			yearly_sf_inv=legacySFStockpile[i];
			yearly_sf_out_of_pile=legacySFStockpile[i];
			sf_by_rx[0]=legacySFStockpile[i];
			if(i>0) sf_reprocessed_by_tier[0]-=(legacySFStockpile[i]-legacySFStockpile[i-1]);
			yearly_pu=puStockpile[i]+legacySFStockpile[i]*MASS_OUT[LegacySFType][1];
			yearly_ma=maStockpile[i]+legacySFStockpile[i]*MASS_OUT[LegacySFType][2];
			yearly_pu_long_term=puStockpile[i]+legacySFStockpile[i]*MASS_OUT[LegacySFType][1];
			yearly_ma_long_term=maStockpile[i]+legacySFStockpile[i]*MASS_OUT[LegacySFType][2];

			for(j=0; j<REACTORNAMES.length; j++) {
				gen_cap_by_tier[BELONGS_TO_TIER[j]]+=genCap[j][i];                                       
				for(k=0; k<i+1; k++) {
					if(SFGenerated[j][k] > EPS) {
						l=AmountOfSFReprocessed[j][k].size()/2;
						for(int m=0; m<l; m++) {
							year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(2*m)).intValue();
							amount_reprocessed_this_year=((Double)AmountOfSFReprocessed[j][k].elementAt(2*m+1)).doubleValue();
							if(year_of_reprocessing==i+START_YEAR) {
								sf_reprocessed_by_tier[BELONGS_TO_TIER[j]]+=amount_reprocessed_this_year;
							}
						}
						if (AmountOfSFReprocessed[j][k].size() > 0) {
							year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(0)).intValue();
						}
						else year_of_reprocessing=END_YEAR+2;
						if(yearSFReprocessed[j][k]<=i+START_YEAR && SFReprocessed[j][k]>EPS) {   
							yearly_sf_inv+=(SFGenerated[j][k]-SFReprocessed[j][k]);
							yearly_pu+=(SFGenerated[j][k]-SFReprocessed[j][k])*MASS_OUT[j][1];
							yearly_ma+=(SFGenerated[j][k]-SFReprocessed[j][k])*MASS_OUT[j][2];
						}
						else if(SFReprocessed[j][k]>EPS && yearSFReprocessed[j][k]>i+START_YEAR && year_of_reprocessing<=i+START_YEAR) {
							l=0;
							yearly_sf_inv+=SFGenerated[j][k];
							yearly_pu+=SFGenerated[j][k]*MASS_OUT[j][1];
							yearly_ma+=SFGenerated[j][k]*MASS_OUT[j][2];
							year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(l)).intValue();
							while (year_of_reprocessing <= i+START_YEAR) {
								amount_reprocessed_this_year=((Double)AmountOfSFReprocessed[j][k].elementAt(l+1)).doubleValue();
								yearly_sf_inv-=amount_reprocessed_this_year;
								yearly_pu-=amount_reprocessed_this_year*MASS_OUT[j][1];
								yearly_ma-=amount_reprocessed_this_year*MASS_OUT[j][2];
								l=l+2;
								if(AmountOfSFReprocessed[j][k].size() > l) {
									year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(l)).intValue();
								}
								else break;
							}
						}
						else {
							yearly_sf_inv+=SFGenerated[j][k];
							yearly_pu+=SFGenerated[j][k]*MASS_OUT[j][1];
							yearly_ma+=SFGenerated[j][k]*MASS_OUT[j][2];
						}
					}
				}
				for(k=0; k<i+1-RES_TIMES[j]-COOLING_TIMES[j]; k++) {
					if(SFGenerated[j][k] > EPS) {
						if (AmountOfSFReprocessed[j][k].size() > 0) {
							year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(0)).intValue();
						}
						else year_of_reprocessing=END_YEAR+2;
						if(yearSFReprocessed[j][k]<=i+START_YEAR+1 && SFReprocessed[j][k]>EPS) {
							yearly_sf_long_term+=(SFGenerated[j][k]-SFReprocessed[j][k]);
							yearly_pu_long_term+=(SFGenerated[j][k]-SFReprocessed[j][k])*MASS_OUT[j][1];
							yearly_ma_long_term+=(SFGenerated[j][k]-SFReprocessed[j][k])*MASS_OUT[j][2];
						}
						else if(SFReprocessed[j][k]>EPS && yearSFReprocessed[j][k]>i+START_YEAR+1 && year_of_reprocessing<=i+START_YEAR+1) {
							l=0;
							yearly_sf_long_term+=SFGenerated[j][k];
							yearly_pu_long_term+=SFGenerated[j][k]*MASS_OUT[j][1];
							yearly_ma_long_term+=SFGenerated[j][k]*MASS_OUT[j][2];
							year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(l)).intValue();
							while (year_of_reprocessing <= i+START_YEAR+1) {
								amount_reprocessed_this_year=((Double)AmountOfSFReprocessed[j][k].elementAt(l+1)).doubleValue();
								yearly_sf_long_term-=amount_reprocessed_this_year;
								yearly_pu_long_term-=amount_reprocessed_this_year*MASS_OUT[j][1];
								yearly_ma_long_term-=amount_reprocessed_this_year*MASS_OUT[j][2];
								l=l+2;
								if(AmountOfSFReprocessed[j][k].size() > l) {
									year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(l)).intValue();
								}
								else break;
							}
						}
						else {
							yearly_sf_long_term+=SFGenerated[j][k];
							yearly_pu_long_term+=SFGenerated[j][k]*MASS_OUT[j][1];
							yearly_ma_long_term+=SFGenerated[j][k]*MASS_OUT[j][2];
						}
					}
				}
				for(k=0; k<i+1-RES_TIMES[j]; k++) {
					if(SFGenerated[j][k] > EPS) {
						if (AmountOfSFReprocessed[j][k].size() > 0) {
							year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(0)).intValue();
						}
						else year_of_reprocessing=END_YEAR+2;
						if(yearSFReprocessed[j][k]<=i+START_YEAR && SFReprocessed[j][k]>EPS) {
							yearly_sf_out_of_pile+=(SFGenerated[j][k]-SFReprocessed[j][k]);
							sf_by_rx[j]+=(SFGenerated[j][k]-SFReprocessed[j][k]);
						}
						else if(SFReprocessed[j][k]>EPS && yearSFReprocessed[j][k]>i+START_YEAR && year_of_reprocessing<=i+START_YEAR) {
							l=0;
							yearly_sf_out_of_pile+=SFGenerated[j][k];
							sf_by_rx[j]+=(SFGenerated[j][k]);
							year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(l)).intValue();
							while (year_of_reprocessing <= i+START_YEAR) {
								amount_reprocessed_this_year=((Double)AmountOfSFReprocessed[j][k].elementAt(l+1)).doubleValue();
								yearly_sf_out_of_pile-=amount_reprocessed_this_year;
								sf_by_rx[j]-=amount_reprocessed_this_year;
								l=l+2;
								if(AmountOfSFReprocessed[j][k].size() > l) {
									year_of_reprocessing=((Integer)AmountOfSFReprocessed[j][k].elementAt(l)).intValue();
								}
								else break;
							}
						}
						else {
							yearly_sf_out_of_pile+=SFGenerated[j][k];
							sf_by_rx[j]+=SFGenerated[j][k];
						}
					}
				}
				/* SF arrays generated */
				if(genCap[j][i] > 0.) {
					total_capacity+=genCap[j][i];
					total_energy+=genCap[j][i]*AVAILABILITY[j]*365.*24.;  // kWh
					
					yearly_om+=genCap[j][i]*yearlyOM[j];

					if(i+START_YEAR >= start_integrating && i+START_YEAR<= stop_integrating) {
						socialPVF = Math.pow((1+socialDR), (i+START_YEAR-start_integrating));
						integratedCosts[j][0]+=(genCap[j][i]*yearlyCapitalCharge[j]+genCap[j][i]*yearlyOM[j])*socialPVF;
						add_to_integrals=true;
					}

					else add_to_integrals=false;

					yearly_fe+=augmentFrontEndCharges(genCap[j][i],j,i);          // frontend costs

					proliferation_resistance[i]+=getFrontEndProliferation(genCap[j][i],j);

					sf_dd=SFGenerated[j][i]-SFReprocessed[j][i];
					
					if(sf_dd > EPS) yearly_be+=augmentBackEndDDCharges(j,sf_dd/SFGenerated[j][i]*genCap[j][i],i);
					if(sf_dd > EPS) proliferation_resistance[i]+=getBackEndDDProliferation(j,sf_dd/SFGenerated[j][i]*genCap[j][i],i);

					if (i<=END_YEAR-START_YEAR+1-NewReactorLifetime) decay_heat[i]+=BACKENDMASS_DD[3]*(sf_dd/SFGenerated[j][i])*genCap[j][i]*capacityToMass(j);
					if (i<=END_YEAR-START_YEAR+1-NewReactorLifetime) decay_heat[i]+=BACKENDMASS[j][6]*(SFReprocessed[j][i]/SFGenerated[j][i])*genCap[j][i]*capacityToMass(j);

					if(SFReprocessed[j][i] > EPS) yearly_be+=augmentBackEndRepCharges(j,SFReprocessed[j][i]/SFGenerated[j][i]*genCap[j][i],i+START_YEAR,yearSFReprocessed[j][i]);
					if(SFReprocessed[j][i] > EPS) proliferation_resistance[yearSFReprocessed[j][i]-START_YEAR]+=getBackEndRepProliferation(j,SFReprocessed[j][i]/SFGenerated[j][i]*genCap[j][i]);
					
					if(i+START_YEAR >= start_integrating && i+START_YEAR<= stop_integrating) integratedCosts[j][integratedCosts[0].length-1]+=genCap[j][i]*AVAILABILITY[j]*365.*24.*socialPVF;
					
				}

			}
			
			yearly_reactor = yearlyReactorCharge[i];
			
			tru_stored=puStockpile[i]+maStockpile[i];
			yearly_be+=tru_stored*TRUSTORCOST;
			if(ActinideWasteStream[1][i] > 0.) {
				yearly_be+=vitrifyAndDisposeOfTRU((ActinideWasteStream[1][i]+ActinideWasteStream[2][i]));
				becomponent[0]+=ActinideWasteStream[1][i];
				becomponent[1]+=ActinideWasteStream[2][i];
			}
			if(add_to_integrals) {
				max_thruput = Math.max(max_thruput,(sf_reprocessed_by_tier[0]+sf_reprocessed_by_tier[1]));
				for(j=0; j<gen_cap_by_tier.length; j++) {
					max_gen_fraction_by_tier[j]=Math.max(gen_cap_by_tier[j]/totalGenCap[i],max_gen_fraction_by_tier[j]);
				}
			}
			if(add_to_integrals) integratedCosts[0][integratedCosts[0].length-2]=(tru_stored*TRUSTORCOST+vitrifyAndDisposeOfTRU((ActinideWasteStream[1][i]+ActinideWasteStream[2][i])))*socialPVF;

			if (total_energy > EPS) {
				yearly_coe=(yearly_fe+yearly_be+yearly_reactor+yearly_om)/total_energy*100.;      // cost of electricity calculation
				yearly_fc=(yearly_be+yearly_fe)/total_energy*100.;
			}
			else {
				yearly_coe=0.;
				yearly_fc=0.;
			}
			if(i>0) {
				ActinideWasteStream[1][i]+=ActinideWasteStream[1][i-1];
				ActinideWasteStream[2][i]+=ActinideWasteStream[2][i-1];
			}

			if (i<=END_YEAR-START_YEAR+1-NewReactorLifetime) {
				if (total_energy > EPS) {
					frontend_and_reactor_costs[i] = (yearly_fe+yearly_reactor+yearly_om)/total_energy*100;
					backend_costs[i] = yearly_be/total_energy*100;
				} else {
					frontend_and_reactor_costs[i] = 0;
					backend_costs[i] = 0;
				}
			}

		}

		/* 
		 * Sum over the decision making years 
		 * This implies that the average COE is the minimizing objective
		 */
		for (i=0; i<END_YEAR-START_YEAR+1-NewReactorLifetime; i++) {
			yearlyLeafValues[i][0] = backend_costs[i];
			yearlyLeafValues[i][1] = decay_heat[i];
			yearlyLeafValues[i][2] = proliferation_resistance[i];
			yearlyLeafValues[i][3] = frontend_and_reactor_costs[i];
		}
		
		return(yearlyLeafValues);

	}
	
	public static void printReactorParamFile(int strategy_three, int strategy_six, int strategy_eight) {
		
		int i;

		try {

			String user_dir = System.getProperty("user.dir");
			TextReader file = new TextReader(user_dir+File.separatorChar+"Reactor_parameters_Template.txt");
			String[] data = file.OpenFile();
			FileWriter write = new FileWriter(user_dir+File.separatorChar+"Reactor_parameters.txt");

			PrintWriter print = new PrintWriter(write);

			String key = "@SFRreplace";

			for (i=0; i<data.length; i++) {
				if (data[i].contains(key)) {
					writeSFRReplace(print, data[i], key, strategy_three, strategy_six, strategy_eight);
				} else {
					print.print(data[i]+"\n");
				}
			}
			
			print.close();

		} catch(IOException e) {
			System.out.print("Error: Failed to write the NFC Parameter file from Decision Making input text");
		}	
		
	}
	
	public static void writeSFRReplace(PrintWriter print, String data, String key, int strategy_three, int strategy_six, int strategy_eight) {
		
		StringBuilder value = new StringBuilder("IfCausesTiltReplaceWithType		0			1		 	");

		if (strategy_three==0) value.append("0,");
		if (strategy_three==1) value.append("1,");
		if (strategy_three==2) value.append("0,");
		if (strategy_three==3) value.append("1,");

		if (strategy_six==0) value.append("0,");
		if (strategy_six==1) value.append("1,");
		if (strategy_six==2) value.append("0,");
		if (strategy_six==3) value.append("1,");
		
		if (strategy_eight==0) value.append("0").append("\n");
		if (strategy_eight==1) value.append("1").append("\n");
		if (strategy_eight==2) value.append("0").append("\n");
		if (strategy_eight==3) value.append("1").append("\n");

		print.print(value.toString());

	}
	
	public static void writeReprocessingCapacity(PrintWriter print, String key, int first_reactor_build_decision, int second_reactor_build_decision, int final_reactor_build_decision) {

		int htgr_and_sfr_decisions=0;
		int lwr_and_sfr_decisions=0;
		
		
		
		if (first_reactor_build_decision==2) {
			lwr_and_sfr_decisions++;
		} else if (first_reactor_build_decision==3) {
			htgr_and_sfr_decisions++;
		}
		
		if (second_reactor_build_decision==2) {
			lwr_and_sfr_decisions++;
		} else if (second_reactor_build_decision==3) {
			htgr_and_sfr_decisions++;
		}
		
		
		
//		if (strategy_six)
//		
//		
//		StringBuilder value = new StringBuilder("IfCausesTiltReplaceWithType		0			1		 	");
//
//		if (strategy_three==0) value.append("0,");
//		if (strategy_three==1) value.append("1,");
//		if (strategy_three==2) value.append("0,");
//		if (strategy_three==3) value.append("1,");
//
//		if (strategy_six==0) value.append("0,");
//		if (strategy_six==1) value.append("1,");
//		if (strategy_six==2) value.append("0,");
//		if (strategy_six==3) value.append("1,");
//
//		if (strategy_eight==0) value.append("0").append("\n");
//		if (strategy_eight==1) value.append("1").append("\n");
//		if (strategy_eight==2) value.append("0").append("\n");
//		if (strategy_eight==3) value.append("1").append("\n");
//
//		print.print(value.toString());

	}
	
	
}
