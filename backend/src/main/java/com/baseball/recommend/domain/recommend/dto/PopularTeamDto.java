package com.baseball.recommend.domain.recommend.dto;

public record PopularTeamDto(String teamName, String primaryColor, long count, int percentage) {
    public static PopularTeamDto of(String teamName, String primaryColor, long count, long total) {
        int pct = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
        return new PopularTeamDto(teamName, primaryColor, count, pct);
    }
}
