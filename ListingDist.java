public class ListingDist {
    private double dist, lat, lon;
    private String type, pos, city, country;

    public ListingDist(String type, double lat, double lon, String pos, String city, String country, double dist) {
        this.dist = dist;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
        this.pos = pos;
        this.city = city;
        this.country = country;
    }

    @Override
    public String toString() {
        return Math.round(dist*10.0)/10.0+"km: [type=" + type + ", lat=" + lat + ", lon=" + lon + ", pos=" + pos + ", city=" + city + ", country="
            + country + "]";
    }

    public double getDist() {
        return dist;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getType() {
        return type;
    }

    public String getPos() {
        return pos;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public void setDist(double dist) {
        this.dist = dist;
    }
}
