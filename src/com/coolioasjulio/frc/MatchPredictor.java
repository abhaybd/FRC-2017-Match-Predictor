package com.coolioasjulio.frc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
		try (Scanner in = new Scanner(System.in)){
			System.out.println("Train again with updated data? This will take a while. y/n");
			char response = in.nextLine().charAt(0);
			if(response != 'n'){
				mp.train();				
			}
			mp.guessRepeat();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private NeuralNetwork network;
	
	public MatchPredictor(){
		network = new NeuralNetwork(new int[]{42,20,3}, new int[]{1,1,0});
	}
	
	public void train() throws IOException{
		DataPoint[] dps = getData();
		saveData(dps,"data.dat");
		double[][] inputs = new double[dps.length][];
		double[][] outputs = new double[dps.length][];
		for(int i = 0; i < dps.length; i++){
			inputs[i] = dps[i].input;
			outputs[i] = dps[i].output;
		}
		System.out.println(Arrays.toString(dps) + dps.length);
		network.train(inputs, outputs, 0.1, 0.9, 3000);
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("match.net"));
		oos.writeObject(network);
		oos.flush();
		oos.close();
	}
	
	private double[] sortScores(Score...scores){
		List<Score> sorted = new ArrayList<Score>();
		for(Score score:scores){
			if(sorted.size() == 0){
				sorted.add(score);
				continue;
			}
			for(int i = 0; i < sorted.size(); i++){
				if(score.totalPoints >= sorted.get(i).totalPoints){
					sorted.add(i, score);
					break;
				}
				else if(i == sorted.size() - 1){
					sorted.add(score);
				}
			}
		}
		double[] toReturn = new double[sorted.size()*7];
		int index = 0;
		for(Score score:sorted){
			toReturn[index++] = score.totalPoints;
			toReturn[index++] = score.teleopPoints;
			toReturn[index++] = score.autoPoints;
			toReturn[index++] = score.autoRotorPoints;
			toReturn[index++] = score.autoMobilityPoints;
			toReturn[index++] = score.autoFuelHigh;
			toReturn[index++] = score.autoFuelLow;
		}
		return toReturn;
	}
	
	public void guessRepeat(){
		try(Scanner in = new Scanner(System.in)){
			network = loadNetwork();
			double[] input = new double[42];
			while(true){
				String[] teams = getTeams(in);
				
				String[] blue = Arrays.copyOfRange(teams, 0, 3);
				String[] red = Arrays.copyOfRange(teams, 3, 6);
				
				List<Score> blueScoreList = new ArrayList<Score>();
				for(String id:blue){
					blueScoreList.add(normalizeScore(APIUtils.getAvgSeasonScore(id)));
				}
				
				List<Score> redScoreList = new ArrayList<Score>();
				for(String id:red){
					redScoreList.add(normalizeScore(APIUtils.getAvgSeasonScore(id)));
				}
				
				double[] blueScore = sortScores(blueScoreList.toArray(new Score[0]));
				double[] redScore = sortScores(redScoreList.toArray(new Score[0]));
				
				for(int i = 0; i < input.length; i++){
					if(i < 21){
						input[i] = blueScore[i];
					}
					else{
						input[i] = redScore[i-21];
					}
				}
				
				System.out.println(Arrays.toString(input));
				double[] result = network.guess(input, true);
				System.out.println("Blue probability: " + (float)result[0]*100f + "%");
				System.out.println("Red probability: " + (float)result[1]*100f + "%");
				System.out.println("Tie probability: " + (float)result[2]*100f + "%");			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String[] getTeams(Scanner in){
		List<String> teams = new ArrayList<String>();
		System.out.println("Teams on blue alliance? Input in a list, separated only by commas. Ex: frc492,frc420,frc6969");
		teams.addAll(Arrays.asList(in.nextLine().replace(" ", "").split(",")));
		System.out.println("Teams on red alliance? Input in a list, separated only by commas. Ex: frc492,frc420,frc6969");
		teams.addAll(Arrays.asList(in.nextLine().replace(" ", ",").split(",")));
		return teams.toArray(new String[0]);
	}
	
	public void saveData(Object obj, String path){
		try(PrintWriter out = new PrintWriter(new FileOutputStream(path))){
			out.print(new Gson().toJson(obj));
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private NeuralNetwork loadNetwork() throws Exception{
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("match.net"))){
			return (NeuralNetwork)ois.readObject();			
		}
	}
	
	public DataPoint[] loadData() throws IOException{
		Reader reader = new InputStreamReader(new FileInputStream("data.dat"));
		List<DataPoint> dps = new Gson().fromJson(reader, new TypeToken<List<DataPoint>>(){}.getType());
		return dps.toArray(new DataPoint[0]);
	}
	
	private Score normalizeScore(Score score){
		Score normalized = new Score();
		normalized.totalPoints = score.totalPoints/500;
		normalized.teleopPoints = score.teleopPoints/500;
		normalized.autoPoints = score.autoPoints/75;
		normalized.autoRotorPoints = score.autoRotorPoints/60;
		normalized.autoMobilityPoints = score.autoMobilityPoints/50;
		normalized.autoFuelHigh = score.autoFuelHigh/10;
		normalized.autoFuelLow = score.autoFuelLow/3;
		return normalized;
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
				
				List<Score> blueScoreList = new ArrayList<Score>();
				for(String id:match.getAlliances().getBlue().getTeams()){
					blueScoreList.add(normalizeScore(APIUtils.getAvgSeasonScore(id)));
				}
				
				List<Score> redScoreList = new ArrayList<Score>();
				for(String id:match.getAlliances().getRed().getTeams()){
					redScoreList.add(normalizeScore(APIUtils.getAvgSeasonScore(id)));
				}
				
				double[] blueScore = sortScores(blueScoreList.toArray(new Score[0]));
				double[] redScore = sortScores(redScoreList.toArray(new Score[0]));
				
				for(int i = 0; i < dp.input.length; i++){
					if(i < 21){
						dp.input[i] = blueScore[i];
					}
					else{
						dp.input[i] = redScore[i-21];
					}
				}
				dataPoints.add(dp);
				System.out.println("Input: " + Arrays.toString(dp.input));
				System.out.println("Output: " + Arrays.toString(dp.output));
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
