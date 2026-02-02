package apis;

import lombok.Getter;

import java.util.Map;

@Getter
public class FlaresolverModel {
    Solution solution;

    @Getter
    public static class Solution {
        String response;
        Map<String, Object>[] cookies;
        String userAgent;
    }
}
