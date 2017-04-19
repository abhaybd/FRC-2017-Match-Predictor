package com.coolioasjulio.frc;

import java.io.BufferedReader;
import java.io.File;
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
			System.out.println("Type a command: build/train/run");
			String response = in.nextLine();
			if(response.equalsIgnoreCase("build")){
				mp.buildIndexes();
			}
			else if(response.equalsIgnoreCase("train")){
				mp.train();
			}
			mp.guessRepeat();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private NeuralNetwork network;
	
	public MatchPredictor(){
		network = new NeuralNetwork(new int[]{60,20,3}, new int[]{1,1,0});
	}
	
	/**
	 * Trains neural network and saves it to match.net
	 * @throws IOException if connection fails
	 */
	public void train() throws IOException{
		//buildIndexes();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		DataPoint[] dps = loadData();
		saveData(dps,"data.dat");
		double[][] inputs = new double[dps.length-100][];
		double[][] outputs = new double[dps.length-100][];
		for(int i = 0; i < inputs.length; i++){
			inputs[i] = dps[i].input;
			outputs[i] = dps[i].output;
		}
		System.out.println(Arrays.toString(dps) + (dps.length-100));
		network.train(inputs, outputs, 0.1, 0.9, 5000);
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("match.net"));
		oos.writeObject(network);
		oos.flush();
		oos.close();
		
		test(network, Arrays.copyOfRange(dps, dps.length-100, dps.length));
	}
	
	/**
	 * Parse the teams and return the estimated winner
	 * @param teams String[] of teams
	 * @return double[] representing the output of the neural network
	 * @throws IOException if connection fails
	 */
	public double[] guess(String[] teams) throws IOException{
		double[] input = new double[60];
		String[] blue = Arrays.copyOfRange(teams, 0, 3);
		String[] red = Arrays.copyOfRange(teams, 3, 6);
		
		List<Score> blueScoreList = new ArrayList<Score>();
		for(String id:blue){
			blueScoreList.add(normalizeScore(getScore(id)));
		}
		
		List<Score> redScoreList = new ArrayList<Score>();
		for(String id:red){
			redScoreList.add(normalizeScore(getScore(id)));
		}
		
		double[] blueScore = sortScores(blueScoreList.toArray(new Score[0]));
		double[] redScore = sortScores(redScoreList.toArray(new Score[0]));
		
		for(int i = 0; i < input.length; i++){
			if(i < 30){
				input[i] = blueScore[i];
			}
			else{
				input[i] = redScore[i-30];
			}
		}
		return network.guess(input,true);
	}
	
	/**
	 * Prompt the user for repeated guesses
	 */
	public void guessRepeat(){
		try(Scanner in = new Scanner(System.in)){
			network = loadNetwork();
			while(true){
				String[] teams = getTeams(in);
				
				double[] result = guess(teams);				
				
				System.out.println("Blue probability: " + (float)result[0]*100f + "%");
				System.out.println("Red probability: " + (float)result[1]*100f + "%");
				System.out.println("Tie probability: " + (float)result[2]*100f + "%");			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void test(NeuralNetwork network, DataPoint[] dps){
		int numCorrect = 0;
		int total = 0;
		for(DataPoint dp:dps){
			double[] guess = network.guess(dp.input);
			int guessWinner = indexOfMax(guess);
			int actualWinner = indexOfMax(dp.output);
			if(guessWinner == actualWinner){
				numCorrect++;
			}
			total++;
			System.out.println(Arrays.toString(dp.output));
			System.out.println(Arrays.toString(guess));
			System.out.println((guessWinner == actualWinner) + "\n");
		}
		System.out.printf("Guessed %s out of %s correctly!\n", numCorrect, total);
	}
	
	private int indexOfMax(double[] arr){
		int index = 0;
		double max = arr[0];
		for(int i = 0; i < arr.length; i++){
			if(arr[i] > max){
				max = arr[i];
				index = i;
			}
		}
		return index;
	}
	
	/**
	 * Build team indexes
	 * @throws IOException if TBA API fails
	 */
	public void buildIndexes() throws IOException{
		List<Team> teams = new ArrayList<Team>();
		//teams.addAll(Arrays.asList(APIUtils.getTeams("pnw", 2017)));
		String[] regions = new String[]{"chs","fim","in","isr","mar","nc","ne","ont","pch","pnw"};
		for(String region:regions){
			teams.addAll(Arrays.asList(APIUtils.getTeams(region, 2017)));
		}
		
		File folder = new File("teams");
		deleteFolder(folder);
		folder.mkdir();
		
		Gson gson = new Gson();
		for(Team team:teams){
			try(PrintWriter out = new PrintWriter("teams/" + team.getKey() + ".team")){
				String json = gson.toJson(APIUtils.getAvgSeasonScore(team.getKey()));
				System.out.println(json);
				out.println(json);
				out.flush();
			}
			catch(IOException | NullPointerException e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * Recursively deletes folder
	 * @param folder Folder to recursively delete. This folder will be deleted too.
	 */
	private void deleteFolder(File folder){
		if(folder.isFile() || !folder.exists()) return;
		for(File file:folder.listFiles()){
			if(file.isDirectory()) deleteFolder(file);
			file.delete();
		}
	}
	
	/**
	 * Looks up score for team in indexes. If it fails, return using the TBA API
	 * @param id FRC specified team id
	 * @return non normalized Score for this id
	 * @throws IOException if API connection fails
	 */
	private Score getScore(String id) throws IOException{
		try(BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("teams/" + id + ".team")))){
			Gson gson = new Gson();
			return gson.fromJson(in, new TypeToken<Score>(){}.getType());
		} catch (IOException e) {
			return APIUtils.getAvgSeasonScore(id);
		}
		
	}
	
	/**
	 * Sort scores from high -> low based on total points
	 * @param scores Scores to sort
	 * @return double[] with length 10 times that of @scores, because 10 datapoints per score.
	 */
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
		double[] toReturn = new double[sorted.size()*10];
		int index = 0;
		for(Score score:sorted){
			toReturn[index++] = score.totalPoints;
			toReturn[index++] = score.teleopPoints;
			toReturn[index++] = score.autoPoints;
			toReturn[index++] = score.autoRotorPoints;
			toReturn[index++] = score.autoMobilityPoints;
			toReturn[index++] = score.autoFuelHigh;
			toReturn[index++] = score.autoFuelLow;
			toReturn[index++] = score.teleopRotorPoints;
			toReturn[index++] = score.teleopFuelHigh;
			toReturn[index++] = score.teleopFuelLow;
		}
		return toReturn;
	}
	
	/**
	 * Promp the user for 3 blue and red teams
	 * @param in Scanner to get input from
	 * @return String[] of length 6 of teams. 0-2 are blue, 3-5 are red.
	 */
	private String[] getTeams(Scanner in){
		List<String> teams = new ArrayList<String>();
		boolean running = true;
		while(running){
			running = false;
			teams.clear();
			System.out.println("Teams on blue alliance? Input in a list, separated only by commas. Ex: frc492,frc420,frc6969");
			teams.addAll(Arrays.asList(in.nextLine().replace(" ", "").split(",")));
			System.out.println("Teams on red alliance? Input in a list, separated only by commas. Ex: frc492,frc420,frc6969");
			teams.addAll(Arrays.asList(in.nextLine().replace(" ", ",").split(",")));
			if(teams.size() != 6){
				System.out.println("Incorrect number of teams entered!");
				running = true;
			}
		}
		return teams.toArray(new String[0]);
	}
	
	/**
	 * Save data as JSON to path @path
	 * @param obj Object to save
	 * @param path Path to store JSON file.
	 */
	private void saveData(Object obj, String path){
		try(PrintWriter out = new PrintWriter(new FileOutputStream(path))){
			out.print(new Gson().toJson(obj));
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Deserialize and return neuralnetwork from file match.net
	 * @return Deserialized neural network
	 * @throws Exception if file not found or if class not found.
	 */
	private NeuralNetwork loadNetwork() throws Exception{
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("match.net"))){
			return (NeuralNetwork)ois.readObject();			
		}
	}
	
	@SuppressWarnings("unused")
	/**
	 * Loads JSON data from disk.
	 * @return DataPoint[] from JSON
	 * @throws IOException If file not found.
	 */
	private DataPoint[] loadData() throws IOException{
		Reader reader = new InputStreamReader(new FileInputStream("data.dat"));
		List<DataPoint> dps = new Gson().fromJson(reader, new TypeToken<List<DataPoint>>(){}.getType());
		return dps.toArray(new DataPoint[0]);
	}
	
	/**
	 * Normalizes score to make all values between 0 and 1
	 * @param score Score to normalize
	 * @return New instance of normalized score
	 */
	private Score normalizeScore(Score score){
		Score normalized = new Score();
		normalized.totalPoints = score.totalPoints/500;
		normalized.teleopPoints = score.teleopPoints/500;
		normalized.autoPoints = score.autoPoints/75;
		normalized.autoRotorPoints = score.autoRotorPoints/60;
		normalized.autoMobilityPoints = score.autoMobilityPoints/50;
		normalized.autoFuelHigh = score.autoFuelHigh/10;
		normalized.autoFuelLow = score.autoFuelLow/3;
		normalized.teleopRotorPoints = score.teleopRotorPoints / 300;
		normalized.teleopFuelHigh = score.teleopFuelHigh/300;
		normalized.teleopFuelLow = score.teleopFuelLow/300;
		return normalized;
	}
	
	/**
	 * Gets new data from TBA API
	 * @return DataPoint[] from the web
	 * @throws IOException if connection fails.
	 */
	private DataPoint[] getData() throws IOException{
		Event[] events = APIUtils.getEvents("pnw", 2017);
		List<DataPoint> dataPoints = new ArrayList<DataPoint>();
		for(Event event:events){
			Match[] matches = APIUtils.getEventMatches(event);
			for(Match match:matches){
				DataPoint dp = new DataPoint();
				dp.input = new double[60];
				dp.output = new double[3];
				
				if(match.getWinner() == Color.BLUE) dp.output[0] = 1;
				else if(match.getWinner() == Color.RED) dp.output[1] = 1;
				else if(match.getWinner() == null) dp.output[2] = 1;
				
				List<Score> blueScoreList = new ArrayList<Score>();
				for(String id:match.getAlliances().getBlue().getTeams()){
					blueScoreList.add(normalizeScore(getScore(id)));
				}
				
				List<Score> redScoreList = new ArrayList<Score>();
				for(String id:match.getAlliances().getRed().getTeams()){
					redScoreList.add(normalizeScore(getScore(id)));
				}
				
				double[] blueScore = sortScores(blueScoreList.toArray(new Score[0]));
				double[] redScore = sortScores(redScoreList.toArray(new Score[0]));
				
				for(int i = 0; i < dp.input.length; i++){
					if(i < 30){
						dp.input[i] = blueScore[i];
					}
					else{
						dp.input[i] = redScore[i-30];
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
