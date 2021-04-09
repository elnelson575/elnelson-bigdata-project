'use strict';
const http = require('http');
var assert = require('assert');
const express= require('express');
const fetch = require('node-fetch');
const app = express();
const mustache = require('mustache');
const Papa = require('papaparse');
const filesystem = require('fs');
const url = require('url');
const port = Number(process.argv[2]);

/* Send simulated weather to kafka */
var kafka = require('kafka-node');
var Producer = kafka.Producer;
var KeyedMessage = kafka.KeyedMessage;
var kafkaClient = new kafka.KafkaClient({kafkaHost: process.argv[5]});
var kafkaProducer = new Producer(kafkaClient);

let nodeGeocoder = require('node-geocoder');
let options = {
	provider: 'openstreetmap'
};


let geoCoder = nodeGeocoder(options);
const hbase = require('hbase')
var hclient = hbase({ host: process.argv[3], port: Number(process.argv[4])})

function rowToMap(row) {
	var stats = {}
	row.forEach(function (item) {
		stats[item['column']] = Number(item['$'])
	});
	return stats;
}

app.use(express.static('public'));
app.get('/homepage/addressdata.html',function (req, res) {
	const street = req.query['street'];
	const city = req.query['city'];
	const state = req.query['state'];
	const zip = req.query['zip'];
	const street_edit = street.replace(/\s/g, '+');
	const city_edit = city.replace(/\s/g, '+');

	async function fetchJSON() {
		const url_formatted = 'https://geocoding.geo.census.gov/geocoder/geographies/address?street=' + street_edit +
			'&city=' + city_edit + '&state=' + state + '&benchmark=Public_AR_Census2010&vintage=Census2010_Census2010&layers=14&format=json';
		console.log(url_formatted);
		const response = await fetch(url_formatted);
		const formatted = await response.text();
		console.log(formatted);
		const re = /(?<=GEOID\":\")\d{11}/
		const format2 = await formatted.match(re);
		if (format2 == null) {
			return 0
		}
		const census_tract = await format2[0];
		return census_tract;
	}

	fetchJSON().then(census_tract => {
		if (census_tract == 0) {
			var template = filesystem.readFileSync("result.mustache").toString();
			var html = mustache.render(template, {
				casedt: "-",
				avg_mon: "-",
				avg_tues: "-",
				avg_wed: "-",
				avg_thurs: "-",
				avg_fri: "-",
				avg_sat: "-",
				avg_sun: "-",
				frequently: "-",
				sometimes: "-",
				always: "-",
				rarely: "-",
				never: "-"
				//Add the others here (mask data)
			});
			res.send(html);
		} else {
			var today = new Date();
			var dd = today.getDate();

			var mm = today.getMonth() + 1;
			var yyyy = today.getFullYear();
			/*		if (dd < 10) {
                        dd = '0' + dd;
                    }*/

			if (mm < 10) {
				mm = '0' + mm;
			}
			today = yyyy + '-' + mm + '-01'

			function daily_update() {
				const data_update = fetch('https://raw.githubusercontent.com/nytimes/covid-19-data/master/live/us-counties.csv')
					.then(response => response.text()).then(data =>
						Papa.parse(data, {
							header: true,
							step: function (row) {
								kafkaProducer.send([{topic: 'elnelson-covid2', messages: JSON.stringify(row.data)}],
									function (err, results) {
										console.log(row.data);
										/*									console.log("Kafka Error: " + err);
																			console.log(results);*/
									}
								);
							},
							complete: function () {
								console.log("done");
							}
						})
					);

			}

			daily_update();

			function get_info(col, map) {
				let val = map["census_tract:" + col];
				console.log('info', val);
				if (isNaN(val))
					return " - ";
				return (val).toFixed(1);
			}

			hclient.table('elnelson_sg_mask')
				.row("Tract_" + census_tract)
				.get(function (error, value) {
					const sgMaskInfo = rowToMap(value);
					let fips = census_tract.substring(0, 5);
					console.log(fips)
					hclient.table('elnelson_covid_data').row(fips + '-' + today).get(function (err, cells) {
						const sgCaseInfo = rowToMap(cells);
						console.log('here')
						let total_cases = sgCaseInfo['fipsdate:cases'];

						var net_cases = (total_cases).toFixed(1);
						console.log(total_cases, 'total');

						var template = filesystem.readFileSync("result.mustache").toString();
						var html = mustache.render(template, {
							casedt: net_cases,
							avg_mon: get_info("avg_Monday", sgMaskInfo),
							avg_tues: get_info("avg_Tuesday", sgMaskInfo),
							avg_wed: get_info("avg_Wednesday", sgMaskInfo),
							avg_thurs: get_info("avg_Thursday", sgMaskInfo),
							avg_fri: get_info("avg_Friday", sgMaskInfo),
							avg_sat: get_info("avg_Saturday", sgMaskInfo),
							avg_sun: get_info("avg_Sunday", sgMaskInfo),
							frequently: get_info("frequently", sgMaskInfo),
							sometimes: get_info("sometimes", sgMaskInfo),
							always: get_info("always", sgMaskInfo),
							rarely: get_info("rarely", sgMaskInfo),
							never: get_info("never", sgMaskInfo)
							//Add the others here (mask data)
						});
						res.send(html);
					});

				});
		}
	});
});
	
app.listen(port);
