package com.project.game1;

import java.util.ArrayList;

public class GameState {

    public static ArrayList<String> players = new ArrayList<>();

    // Fixed role pool for the whole match
    public static ArrayList<String> roles = new ArrayList<>();
    public static ArrayList<Integer> rolePoints = new ArrayList<>();

    // Cumulative points
    public static ArrayList<Integer> cumulativeScores =
            new ArrayList<>();

    public static void initialize(
            ArrayList<String> playerList,
            ArrayList<String> roleList,
            ArrayList<Integer> pointList
    ) {

        players = new ArrayList<>(playerList);

        roles = new ArrayList<>(roleList);

        rolePoints = new ArrayList<>(pointList);

        cumulativeScores.clear();

        for (int i = 0; i < players.size(); i++) {
            cumulativeScores.add(0);
        }
    }

    public static void addRoundPoints(
            ArrayList<Integer> roundPoints
    ) {

        for (int i = 0; i < cumulativeScores.size(); i++) {

            cumulativeScores.set(
                    i,
                    cumulativeScores.get(i)
                            + roundPoints.get(i)
            );
        }
    }
}