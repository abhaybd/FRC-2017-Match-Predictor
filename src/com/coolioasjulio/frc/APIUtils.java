package com.coolioasjulio.frc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class APIUtils {
	public static enum Color{
		BLUE, RED
	}
	
	public static final String domain = "https://www.thebluealliance.com/api/v2";
	
	/**
	 * Queries url with stub and returns json
	 * @param url url WITHOUT domain stub to query
	 * @return JSON
	 * @throws IOException if connection fails
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getJSON(String url, Type type) throws IOException{
		URLConnection connection = new URL(domain + url).openConnection();
		connection.addRequestProperty("X-TBA-App-Id", "frc492:match-prediction:1");
		connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
		Reader reader = new InputStreamReader(connection.getInputStream());
		
		Gson gson = new Gson();
		return (T)gson.fromJson(reader, type);
	}
	
	/**
	 * Get all Matches in an event
	 * @param event Event to get matches from
	 * @return Match[] representing all matches played in that event
	 * @throws IOException if connection fails
	 */
	public static Match[] getEventMatches(Event event) throws IOException{
		List<Match> matches = getJSON(String.format("/event/%s/matches", event.getKey()), new TypeToken<List<Match>>(){}.getType());
		return matches.toArray(new Match[0]);
	}
	
	/**
	 * Get all events in a region this year that haven't happened yet.
	 * @param region FRC specified region code
	 * @param year 4 digit year. ex: 2017
	 * @return Event[] of all events in that region that haven't happened yet this year.
	 * @throws IOException if connection failed
	 */
	public static Event[] getEvents(String region, int year) throws IOException{
		List<Event> events = getJSON(String.format("/district/%s/%s/events", region, year), new TypeToken<List<Event>>(){}.getType());
		events.removeIf(e -> !e.hasPassed() || !e.thisYear());
		return events.toArray(new Event[0]);
	}
	
	/**
	 * Get al teams in a region this year.
	 * @param region FRC specified region code
	 * @param year 4 digit year. ex: 2017
	 * @return Team[] of all teams competing that year in that region
	 * @throws IOException if connection failed
	 */
	public static Team[] getTeams(String region, int year) throws IOException{
		List<Team> teams = getJSON(String.format("/district/%s/%s/rankings", region, year), new TypeToken<List<Team>>(){}.getType());
		return teams.toArray(new Team[0]);
	}
	
	/**
	 * Get all events a team has competed in this year
	 * @param id FRC specified team id
	 * @return Events that team competes in
	 * @throws IOException if connection fails
	 */
	public static Event[] getTeamEvents(String id) throws IOException{
		List<Event> event = getJSON("/team/" + id + "/history/events", new TypeToken<List<Event>>(){}.getType());
		event.removeIf(e -> !e.hasPassed() || !e.thisYear());
		
		return event.toArray(new Event[0]);
	}
	
	/**
	 * Get average score in the current season for a team
	 * @param id FRC specified team id
	 * @return Average score this year
	 * @throws IOException if connection fails
	 */
	public static Score getAvgSeasonScore(String id) throws IOException{
		Event[] events = getTeamEvents(id);
		Score score = new Score();
		for(Event event:events){
			Score s = getAvgMatchScore(id, event.getKey());
			score.autoPoints += s.autoPoints / (double)events.length;
			score.totalPoints += s.totalPoints / (double)events.length;
			score.teleopPoints += s.teleopPoints / (double)events.length;
			score.autoRotorPoints += s.autoRotorPoints / (double)events.length;
			score.autoMobilityPoints += s.autoMobilityPoints / (double)events.length;
			score.autoFuelHigh += s.autoFuelHigh / (double)events.length;
			score.autoFuelLow += s.autoFuelLow / (double)events.length;
		}
		return score;
	}
	
	/**
	 * Get average score of a team in an event
	 * @param id FRC-specified team id
	 * @param event FRC-specified event id
	 * @return Score object representing average score in event
	 * @throws IOException if connection fails
	 */
	public static Score getAvgMatchScore(String id, String event) throws IOException {		
		Match[] matches = getTeamMatches(id, event);
		Score score = new Score();
		for(Match m:matches){
			Color team = m.alliances.getColor(id);
			Score s = m.score_breakdown.getScore(team);
			score.autoPoints += s.autoPoints / (double)matches.length;
			score.totalPoints += s.totalPoints / (double)matches.length;
			score.teleopPoints += s.teleopPoints / (double)matches.length;
			score.autoRotorPoints += s.autoRotorPoints / (double)matches.length;
			score.autoMobilityPoints += s.autoMobilityPoints / (double)matches.length;
			score.autoFuelHigh += s.autoFuelHigh / (double)matches.length;
			score.autoFuelLow += s.autoFuelLow / (double)matches.length;
		}
		return score;
	}
	
	/**
	 * Get all matches that a team played in an event
	 * @param id FRC-specified team id
	 * @param event FRC-specified event id
	 * @return Array of Match objects representing the matches
	 * @throws IOException if connection fails
	 */
	public static Match[] getTeamMatches(String id, String event) throws IOException{
		List<Match> matches = getJSON(String.format("/team/%s/event/%s/matches", id, event), new TypeToken<List<Match>>(){}.getType());
		return matches.toArray(new Match[0]);
	}
	
	public static class Event{
		private String key, end_date, event_code;
		
		public String getKey() { return key; }
		public String getDate() { return end_date; }
		public String getCode() { return event_code; }
		
		public boolean hasPassed(){
			try {
				Date end = new SimpleDateFormat("yyyy-MM-dd").parse(end_date);
				Date current = new Date();
				int diff = end.compareTo(current);
				return diff < 0;
			} catch (ParseException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		public boolean thisYear(){
			try {
				Calendar end = Calendar.getInstance();
				end.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(end_date));
				Calendar current = Calendar.getInstance();
				return current.get(Calendar.YEAR) == end.get(Calendar.YEAR);
			} catch (ParseException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		@Override
		public String toString(){
			return "{ key : " + key + ", date : " + end_date + " }";
		}
	}
	
	public static class Team{
		public int rank;
		private String team_key;
		public int point_total;
		
		public String getKey(){ return team_key; }
		
		@Override
		public String toString(){
			return String.format("{ rank : %s, key : %s, points : %s }", rank, team_key, point_total);
		}
	}
	
	public static class Match{
		private String key;
		private int match_number;
		private ScoreBreakdown score_breakdown;
		private Alliances alliances;
		
		public String getKey(){ return key; }
		public int getMatchNumber(){ return match_number; }
		public ScoreBreakdown getScores(){ return score_breakdown; }
		public Alliances getAlliances(){ return alliances; }
		
		public Color getWinner(){
			double red = score_breakdown.red.totalPoints;
			double blue = score_breakdown.blue.totalPoints;
			if(red > blue) return Color.RED;
			else if(blue > red) return Color.BLUE;
			else return null;
		}
		
		@Override
		public String toString(){
			return "key: " + key + " - match: " + match_number + " - alliances: " + alliances.toString() + " - score: " + score_breakdown.toString();
		}
	}
	
	public static class ScoreBreakdown{
		private Score blue, red;
		
		public Score getBlueScore(){ return blue; }
		public Score getRedScore(){ return red; }
		
		public Score getScore(Color team){
			if(team == Color.BLUE){
				return blue;
			}
			else if(team == Color.RED){
				return red;
			}
			return null;
		}
		
		@Override
		public String toString(){
			return blue.toString() + " | " + red.toString();
		}
	}
	
	public static class Score{
		public double totalPoints, teleopPoints, autoPoints, autoRotorPoints, autoMobilityPoints, autoFuelHigh, autoFuelLow;
		
		public double[] getFields(){
			return new double[]{totalPoints, teleopPoints, autoPoints, autoRotorPoints, autoMobilityPoints, autoFuelHigh, autoFuelLow};
		}
		
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder("{ ");
			Field[] fields = this.getClass().getDeclaredFields();
			for(int i = 0; i < fields.length; i++){
				try {
					sb.append(fields[i].getName() + " : " + fields[i].getDouble(this));
					if(i != fields.length - 1) sb.append(", ");
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			return sb.toString() + " }";
		}
	}
	
	public static class Alliances{
		private Alliance blue, red;
		
		public Alliance getBlue(){ return blue; }
		public Alliance getRed() { return red; }
		
		@Override
		public String toString(){
			return blue.toString() + " - " + red.toString();
		}
		
		public Color getColor(String id){
			if(Arrays.asList(blue.teams).contains(id)){
				return Color.BLUE;
			}
			else if(Arrays.asList(red.teams).contains(id)){
				return Color.RED;
			}
			return null;
		}
	}
	
	public static class Alliance{
		private int score;
		private String[] teams;
		
		public String[] getTeams(){ return teams; }
		public int getScore(){ return score; }
		
		@Override
		public String toString(){
			return Arrays.toString(teams);
		}
	}
}
