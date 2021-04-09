package edu.uchicago.elnelson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import com.opencsv.CSVReader;
import edu.uchicago.elnelson.sgWeekly.weekPOI;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;


public abstract class SGProcessor {
// h/t https://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
	void processLine(String line, String archiveEntryName) throws IOException {
		ArrayList<String> csvLine = new ArrayList<String>();
		int start = 0;
		boolean inQuotes = false;
		for (int current = 0; current < line.length(); current++) {
			if (line.charAt(current) == '\"') inQuotes = !inQuotes; // toggle state
			else if (line.charAt(current) == ',' && !inQuotes) {
				csvLine.add(line.substring(start, current));
				start = current + 1;
			}
		}
		csvLine.add(line.substring(start));

		//if(result.length > 0 ) {
			processSG(weekFromLine(csvLine), archiveEntryName);
		//}
	}

	abstract void processSG(weekPOI wkpoi, String fileName) throws IOException;
	BufferedReader getFileReader(InputStream is) throws FileNotFoundException, IOException {
		return new BufferedReader(new InputStreamReader(new GZIPInputStream(is)));
	}
	
	void processSGFile(InputStream is, String archiveEntryName) throws IOException {
		BufferedReader br = getFileReader(is);
		br.readLine(); // Discard header
		String line;
		while((line = br.readLine()) != null) {
			processLine(line, archiveEntryName);
		}
	}

	void processSGTarFile(InputStream is) throws IOException {
		TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(is);
		TarArchiveEntry entry;
		while ((entry = (TarArchiveEntry)tarArchiveInputStream.getNextEntry()) != null) {
			if(!entry.getName().endsWith(".gz"))
				continue;
			processSGFile(tarArchiveInputStream, entry.getName());
		}
	}
	
	weekPOI weekFromLine(ArrayList<String> csvLine) {
		weekPOI wkpoi = new weekPOI();
		wkpoi.setPlacekey(csvLine.get(0));
		wkpoi.setSafegraph_place_id(csvLine.get(1));
		wkpoi.setLocation_name(csvLine.get(2));
		wkpoi.setStreet_address(csvLine.get(3));
		wkpoi.setCity(csvLine.get(4));
		wkpoi.setRegion(csvLine.get(5));
		wkpoi.setPostal_code(csvLine.get(6));
		wkpoi.setIso_country_code(csvLine.get(7));
		//wkpoi.setSafegraph_brand_ids(csvLine[8]);
		//wkpoi.setBrands(csvLine[9]);
		wkpoi.setDate_range_start(csvLine.get(10));
		wkpoi.setDate_range_end(csvLine.get(11));
		wkpoi.setRaw_visit_counts(csvLine.get(12));
		wkpoi.setRaw_visitor_counts(csvLine.get(13));
		wkpoi.setVisits_by_day(csvLine.get(14));
		wkpoi.setVisits_by_each_hour(csvLine.get(15));
		wkpoi.setPoi_cbg(csvLine.get(16));
		//wkpoi.setVisitor_home_cbgs(csvLine[17]);
		//wkpoi.setVisitor_daytime_cbgs(csvLine[18]);
		//wkpoi.setVisitor_country_of_origin(csvLine[19]);
		//wkpoi.setDistance_from_home(Integer.parseInt(csvLine[20]));
		//wkpoi.setMedian_dwell(Double.parseDouble(csvLine[21]));
		//wkpoi.setRelated_same_day_brand(csvLine[22]);
		//wkpoi.setRelated_same_week_brand(csvLine[23]);
		//wkpoi.setDevice_type(csvLine[24]);
		return wkpoi;
	}
}
