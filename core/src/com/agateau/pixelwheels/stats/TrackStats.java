/*
 * Copyright 2018 Aurélien Gâteau <mail@agateau.com>
 *
 * This file is part of Pixel Wheels.
 *
 * Pixel Wheels is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.agateau.pixelwheels.stats;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;

public class TrackStats implements Json.Serializable {
    public static final int RECORD_COUNT = 3;
    public static final String DEFAULT_RECORD_VEHICLE = "CPU";

    transient GameStats mGameStats;
    ArrayList<TrackResult> mLapRecords;
    ArrayList<TrackResult> mTotalRecords;

    public enum ResultType {
        LAP,
        TOTAL
    }

    TrackStats(GameStats gameStats) {
        mGameStats = gameStats;
        mLapRecords = new ArrayList<>();
        mTotalRecords = new ArrayList<>();
    }

    public TrackStats () {
        this(null);
    }

    @Override
    public void write (Json json) {
        ArrayList<TrackResult> lapRecords = new ArrayList<>();
        for (TrackResult result: mLapRecords)
            if (!result.vehicle.equals(DEFAULT_RECORD_VEHICLE))
                lapRecords.add(result);
        json.writeValue("mLapRecords", lapRecords, ArrayList.class);
        ArrayList<TrackResult> totalRecords = new ArrayList<>();
        for (TrackResult result: mTotalRecords)
            if (!result.vehicle.equals(DEFAULT_RECORD_VEHICLE))
                totalRecords.add(result);
        json.writeValue("mTotalRecords", totalRecords, ArrayList.class);
    }

    @Override
    public void read (Json json, JsonValue jsonData) {
        mLapRecords = json.readValue("mLapRecords", ArrayList.class, jsonData);
        mTotalRecords = json.readValue("mTotalRecords", ArrayList.class, jsonData);
    }

    public ArrayList<TrackResult> get(ResultType resultType) {
        return resultType == ResultType.LAP ? mLapRecords : mTotalRecords;
    }

    public int addResult(ResultType resultType, String vehicleName, float time) {
        TrackResult result = new TrackResult(vehicleName, time);
        int rank = addResult(get(resultType), result);
        if (rank != -1) {
            mGameStats.save();
        }
        return rank;
    }

    private static int addResult(ArrayList<TrackResult> results, TrackResult result) {
        // Insert result if it is better than an existing one
        for (int idx = 0; idx < results.size(); ++idx) {
            if (result.value < results.get(idx).value) {
                results.add(idx, result);
                if (results.size() > RECORD_COUNT) {
                    results.remove(RECORD_COUNT);
                }
                return idx;
            }
        }
        // If result is not better than existing ones but there is room at the end, append it
        if (results.size() < RECORD_COUNT) {
            results.add(result);
            return results.size() - 1;
        }
        return -1;
    }
}
