

case class LiveReport(
    date: String,
    county: String,
    state: String,
    fips: String,
    cases: String,
    deaths: String,
    confirmed_cases: Int,
    confirmed_deaths: Int,
    probable_cases: Int,
    probable_deaths: Int)