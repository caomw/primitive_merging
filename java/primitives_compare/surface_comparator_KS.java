/*
 * Copyright © Alexandre Boulch, Renaud Marlet, Ecole Nationale des Ponts et Chaussees
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:

 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.

 * The Software is provided "as is", without warranty of any kind, express or implied,
 * including but not limited to the warranties of merchantability, fitness for a
 * particular purpose and noninfringement. In no event shall the authors or copyright
 * holders be liable for any claim, damages or other liability, whether in an action of
 * contract, tort or otherwise, arising from, out of or in connection with the software
 * or the use or other dealings in the Software.
 */


package primitives_compare;

import primitives_compare.Surface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class surface_comparator_KS<Point,
Surface1 extends Surface<Point>,
Surface2 extends Surface<Point>,
Surface3 extends Surface<Point> > {

	
	//vector for distributions
	ArrayList<Double> X,Y;
	
	
	//Surfaces and pointSets
	Surface1 S1;
	Surface2 S2;
	Surface3 S3;
	ArrayList<Point> pP1;
	ArrayList<Point> pP2;
	
	boolean use_3_surfaces;
	
	public surface_comparator_KS(Surface1 s1,ArrayList<Point> pp1,
						Surface2 s2,ArrayList<Point> pp2,
						Surface3 s3){
		S1 = s1;
		S2 = s2;
		S3 = s3;
		pP1 = pp1;
		pP2 = pp2;
		
		use_3_surfaces = true;
		
		X = new ArrayList<Double>();
		Y = new ArrayList<Double>();
	}
	
	public surface_comparator_KS(Surface1 s1,ArrayList<Point> pp1,
			Surface2 s2,ArrayList<Point> pp2){
		S1=s1;
		S2=s2;
		pP1 = pp1;
		pP2 = pp2;
		
		use_3_surfaces = false;
		
		X = new ArrayList<Double>();
		Y = new ArrayList<Double>();
	}
	
	int sample_size(double alpha, double epsilon){
		//from Dvoretsy-Keifer-Wolfowitz
		return (int) (Math.ceil(Math.log(2./alpha))/(2*epsilon*epsilon));
	}
	
	int minUint(int i1, int i2){
		if(i1 < i2)	return i1;
		return i2;
	}
	
	<SurfaceP extends Surface<Point>, SurfaceOther extends Surface<Point> > 
	void generateXY_for_one_pointSet(int sample_size,
			ArrayList<Point> P,
		SurfaceP SP,
		SurfaceOther SOther){
		
		Random rand = new Random();
		
		//have to divide sample size by 2 for the two set of points
		int nbr_points_prim = (int) Math.ceil(sample_size/2.);
		
		if(P.size() <= nbr_points_prim){
			//less points than desired => use all points
			for(int i=0; i<P.size(); i++){
				X.add(SP.distance(P.get(i)));
				Y.add(SOther.distance(P.get(i)));
			}
		}else{
			//shuffle a copy of P1
			//slow and memory greedy, should be fixed
			ArrayList<Point> copyP = P;
			for(int i=0; i<nbr_points_prim; i++){
				int idSwap =i+Math.abs(rand.nextInt())%(P.size()-i);
				Point temp = copyP.get(idSwap);	
				copyP.set(idSwap, copyP.get(i));
				copyP.set(i, temp);
			}
			for(int i=0; i<nbr_points_prim; i++){
				X.add(SP.distance(copyP.get(i)));
				Y.add(SOther.distance(copyP.get(i)));
			}
		}
	}
	
	void generateXY(int sample_size){
		//clear vector
		if(X.size() > 0){
			X.clear();
		}
		if(Y.size() > 0){
		Y.clear();
		}
		
		if(! use_3_surfaces){//use surface 2
			generateXY_for_one_pointSet(sample_size,pP1,S1,S2);
			generateXY_for_one_pointSet(sample_size,pP2,S2,S1);
		}else{//use surface 3
			generateXY_for_one_pointSet(sample_size,pP1,S1,S3);
			generateXY_for_one_pointSet(sample_size,pP2,S2,S3);
		}
		
		Collections.sort(X);
		Collections.sort(Y);
	}
		
	
	
	double Kolmogorov_Smirnov(){
		
		double Dmax = 0;
		double current_D=0;
		int current_rankX = 0;
		int current_rankY = 0;
		
		Random rand = new Random();
		
		while(current_rankX < X.size() && current_rankY < Y.size()){
			if(X.get(current_rankX) < Y.get(current_rankY)){
				current_D += 1./X.size();
				current_rankX++;
			}else if(X.get(current_rankX) > Y.get(current_rankY)){
				current_D += -1./Y.size();
				current_rankY++;
			}else{
				//the two are equal
				if(rand.nextInt()%2 == 0){//select direct
					current_D += 1./X.size();
					current_rankX++;
				}else{//select cross
					current_D += -1./Y.size();
					current_rankY++;
				}
			}
			if(Math.abs(current_D)>Dmax){
				Dmax = Math.abs(current_D);
			}
		}
		while(current_rankX < X.size()){
			current_D += 1./X.size();
			current_rankX++;
		}
		if(Math.abs(current_D)>Dmax){
			Dmax = Math.abs(current_D);
		}
		while(current_rankY < Y.size()){
			current_D += -1./Y.size();
			current_rankY++;
		}
		if(Math.abs(current_D)>Dmax){
			Dmax = Math.abs(current_D);
		}
		return Dmax/Math.sqrt(2./X.size());
	}
	
	double KS_threshold(double alpha){
		double c_alpha=0;
		Map<Double,Double> alpha_to_c_alpha = new HashMap<Double,Double>();
		alpha_to_c_alpha.put(0.10,1.22);
		alpha_to_c_alpha.put(0.05,1.36);
		alpha_to_c_alpha.put(0.01,1.63);
		alpha_to_c_alpha.put(0.025,1.48);
		alpha_to_c_alpha.put(0.005, 1.73);
		alpha_to_c_alpha.put(0.001, 1.95);
		if(alpha_to_c_alpha.containsKey(alpha)){
			c_alpha = alpha_to_c_alpha.get(alpha);
		}
		return c_alpha;
	}

	//apply test
	public boolean apply_KS_test(
		double alpha, 
		double epsilon){
		
		//get the size of the samples
		int nbr_points = sample_size(alpha,epsilon);
		
		//create vector X and Y
		generateXY(nbr_points);
		
		double c_alpha=KS_threshold(alpha);
		return Kolmogorov_Smirnov() < c_alpha;
	}
	
	//get statistic
	public double get_KS(
		double alpha, 
		double epsilon){
		
		//get the size of the samples
		int nbr_points = sample_size(alpha,epsilon);
		
		//create vector X and Y
		generateXY(nbr_points);
		
		return Kolmogorov_Smirnov();
	}
	
}
