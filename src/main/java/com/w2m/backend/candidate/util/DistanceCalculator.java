package com.w2m.backend.candidate.util;

// 하버사인 공식: 지구를 구로 가정한 두 좌표 간 직선거리(미터) 계산
public class DistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000;

    public static double calculateDistance(double lat1, double lng1,
double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat =  Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
