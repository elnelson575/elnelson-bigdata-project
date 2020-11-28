namespace java edu.uchicago.elnelson.sgWeekly

struct weekPOI {
	1: optional string placekey;
	2: optional string safegraph_place_id;
	3: optional string location_name;
	4: optional string street_address;
	5: optional string city;
	6: optional string region;
	7: optional string postal_code;
	8: optional string iso_country_code;
	9: optional string safegraph_brand_ids;
	10: optional string brands;
	11: optional string date_range_start;
	12: optional string date_range_end;
	13: optional string raw_visit_counts;
	14: optional string raw_visitor_counts;
	15: optional string visits_by_day;
	16: optional string visits_by_each_hour;
	17: optional string poi_cbg;
	18: optional string visitor_home_cbgs;
	19: optional string visitor_daytime_cbgs;
	20: optional string visitor_country_of_origin;
	21: optional i64 distance_from_home;
	22: optional double median_dwell;
	23: optional string related_same_day_brand;
	24: optional string related_same_week_brand;
	25: optional string device_type;
}

