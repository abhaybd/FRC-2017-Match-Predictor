package com.coolioasjulio.frc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.coolioasjulio.frc.APIUtils.*;
import com.coolioasjulio.neuralnetwork.NeuralNetwork;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MatchPredictor {
	public static void main(String[] args){
		MatchPredictor mp = new MatchPredictor();
		try {
			mp.train();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private NeuralNetwork network;
	
	public MatchPredictor(){
		network = new NeuralNetwork(new int[]{42,20,3}, new int[]{1,1,0});
	}
	
	public void train() throws IOException{
		DataPoint[] dps = loadData();
		saveData(dps);
		double[][] inputs = new double[dps.length][];
		double[][] outputs = new double[dps.length][];
		for(int i = 0; i < dps.length; i++){
			inputs[i] = dps[i].input;
			outputs[i] = dps[i].output;
		}
		System.out.println(Arrays.toString(dps) + dps.length);
		network.train(inputs, outputs, 0.1, 0.9, 3000);
		
		
		System.out.println(dps.length);
		Scanner in = new Scanner(System.in);
		System.out.println("Type in an index number");
		int index = in.nextInt();
		in.nextLine();
		System.out.println(Arrays.toString(dps[index].input));
		System.out.println(Arrays.toString(dps[index].output));
		System.out.println(Arrays.toString(network.guess(dps[index].input,true)));
		in.close();
	}
	
	public void saveData(Object obj){
		try(PrintWriter out = new PrintWriter(new FileOutputStream("data.dat"))){
			out.print(new Gson().toJson(obj));
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public DataPoint[] loadData() throws IOException{
		Reader reader = new InputStreamReader(new FileInputStream("data.dat"));
		List<DataPoint> dps = new Gson().fromJson(reader, new TypeToken<List<DataPoint>>(){}.getType());
		return dps.toArray(new DataPoint[0]);
	}
	
	public DataPoint[] getData() throws IOException{
		Event[] events = APIUtils.getEvents("pnw", 2017);
		List<DataPoint> dataPoints = new ArrayList<DataPoint>();
		for(Event event:events){
			Match[] matches = APIUtils.getEventMatches(event);
			for(Match match:matches){
				DataPoint dp = new DataPoint();
				dp.input = new double[42];
				dp.output = new double[3];
				
				if(match.getWinner() == Color.BLUE) dp.output[0] = 1;
				else if(match.getWinner() == Color.RED) dp.output[1] = 1;
				else if(match.getWinner() == null) dp.output[2] = 1;
				int index = 0;
				for(String id:match.getAlliances().getBlue().getTeams()){
					Score score = APIUtils.getAvgSeasonScore(id);
					dp.input[index++] = score.totalPoints/500;
					dp.input[index++] = score.teleopPoints/500;
					dp.input[index++] = score.autoPoints/75;
					dp.input[index++] = score.autoRotorPoints/60;
					dp.input[index++] = score.autoMobilityPoints/50;
					dp.input[index++] = score.autoFuelHigh/10;
					dp.input[index++] = score.autoFuelLow/3;
				}
				for(String id:match.getAlliances().getRed().getTeams()){
					Score score = APIUtils.getAvgSeasonScore(id);
					dp.input[index++] = score.totalPoints/500;
					dp.input[index++] = score.teleopPoints/500;
					dp.input[index++] = score.autoPoints/75;
					dp.input[index++] = score.autoRotorPoints/60;
					dp.input[index++] = score.autoMobilityPoints/50;
					dp.input[index++] = score.autoFuelHigh/10;
					dp.input[index++] = score.autoFuelLow/3;
				}
				System.out.println("Input: " + Arrays.toString(dp.input));
				System.out.println("Output: " + Arrays.toString(dp.output));
				dataPoints.add(dp);
			}
		}
		return dataPoints.toArray(new DataPoint[0]);
	}
	
	public class DataPoint implements java.io.Serializable{
		private static final long serialVersionUID = 1L;
		public double[] input;
		public double[] output;
		@Override
		public String toString(){
			return "{ input : " + Arrays.toString(input) + ", output : " + Arrays.toString(output) + " }";
		}
	}
}
