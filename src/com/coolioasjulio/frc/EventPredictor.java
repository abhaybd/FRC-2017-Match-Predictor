package com.coolioasjulio.frc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.coolioasjulio.frc.APIUtils.Match;

public class EventPredictor {
	public static void main(String[] args){
		EventPredictor ep = new EventPredictor("2017pncmp");
	}
	
	public EventPredictor(String event){
		MatchPredictor mp = new MatchPredictor();
		try {
			Match[] matches = APIUtils.getEventMatches(event);
			//System.out.println(Arrays.asList(matches).toString());
			Map<String,Integer> teams = new HashMap<>();
			for(Match match:matches){
				String[] blue = match.getAlliances().getBlue().getTeams();
				String[] red = match.getAlliances().getRed().getTeams();
				String[] combined = new String[blue.length + red.length];
				
				for(int i = 0; i < combined.length; i++){
					if(i < blue.length){
						combined[i] = blue[i];
					}else combined[i] = red[i%blue.length];
				}
				
				double[] guess = mp.guess(combined);
				int winner = indexOfMax(guess);
				System.out.println(Arrays.toString(guess));
				System.out.println(Arrays.toString(combined) + " : " + winner);
				for(int i = 0; i < combined.length; i++){
					if(winner == 0 && i < combined.length/2){
						incrementValue(teams, combined[i]);
					}
					else if(winner == 1 && i >= combined.length/2){
						incrementValue(teams, combined[i]);
					}
				}
			}
			System.out.println("Done");
			printMap(teams);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private <T,V> void printMap(Map<T,V> map){
		for(T key:map.keySet()){
			System.out.println(key.toString() + " - " + map.get(key).toString());
		}
	}
	
	private <T> void incrementValue(Map<T,Integer> map, T key){
		if(map.containsKey(key)){
			map.put(key, map.get(key)+1);
		}
		else{
			map.put(key, 1);
		}
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
}
