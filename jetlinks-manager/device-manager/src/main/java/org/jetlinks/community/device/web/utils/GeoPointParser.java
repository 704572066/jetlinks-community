package org.jetlinks.community.device.web.utils;

import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.action.search.SearchResponse;
import org.jetlinks.community.device.web.response.DeviceGeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeoPointParser {

    public static List<DeviceGeoPoint> parseGeoPoints(SearchResponse response, Map<String, String> deviceNameMap) {
        List<DeviceGeoPoint> result = new ArrayList<>();

        Terms terms = response.getAggregations().get("each_device");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            TopHits topHits = bucket.getAggregations().get("latest_point");
            SearchHit[] hits = topHits.getHits().getHits();
            if (hits.length > 0) {
                Map<String, Object> source = hits[0].getSourceAsMap();
                Map<String, Object> geoValue = (Map<String, Object>) source.get("geoValue");

                String deviceId = (String) source.get("deviceId");
                String name = deviceNameMap.getOrDefault(deviceId, "");
                double lon = ((Number) geoValue.get("lon")).doubleValue();
                double lat = ((Number) geoValue.get("lat")).doubleValue();
                long timestamp = ((Number) source.get("timestamp")).longValue();

                String geoPoint = lon + "," + lat;
                result.add(new DeviceGeoPoint(deviceId, name, geoPoint, timestamp));
            }
        }

        return result;
    }
}