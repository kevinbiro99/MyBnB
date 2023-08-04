/*
 * The Host toolkit will use the surrounding listings to suggest amenities
 * and prices that exist in the area where the new listing is being created.
 * 
 * In the case where there are no listings within the radius of the area, 
 * no price or amenities will be suggested
 * 
 */

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class HostToolkit {
    private static double RADIUS = 300; // in km

    // Only the top 50% of most popular amenities are suggested
    private final static double TOP_SUGGESTED_AMENITIES = 0.5;

    /*
     * Suggest amenities that listings of the same type in this area also have.
     * 
     * Counts the occurances of all amenities in the range and suggests
     * the most popular ones.
     * 
     * Prints out the suggested amenities along with estimated cost
     */
    public static void suggestAmenities(double lat, double lon, String type) throws ClassNotFoundException, SQLException {
        // Get listings within acceptable range
        ArrayList<Integer> listingsInRange = new ArrayList<Integer>();
        ArrayList<String> recommendedAmenities = new ArrayList<String>();
        ResultSet rs = SqlDAO.getInstance().getListingsInRadius(RADIUS, lat, lon, type);

        // save listing ids in the range
        while (rs.next()) {
            listingsInRange.add(rs.getInt("listing_id"));
        }
        rs.close();

        // Get the recommended amenities sorted descending (most popular is at index 0)
        rs = SqlDAO.getInstance().getRecommendedAmenitiesInRangeSortedDesc(RADIUS, lat, lon, type);
        while (rs.next()) {
            recommendedAmenities.add(rs.getString("amenity"));
            // System.out.println(rs.getString("amenity") + " " + rs.getInt("amenity_count"));
        }
        rs.close();
        

        // Calculate the price change for each amenity
        int n = 0;
        System.out.println("\nRecommended amenities to add based on the location of your listing. \n" + 
            "Sorted from most popular to least popular, and estimated price increase to \nlisting per day for each amenity:");
        for(String s : recommendedAmenities) {
            n++;
            System.out.println(s + ", Price: " + estimateAmenityCost(listingsInRange, s));
            if (n / recommendedAmenities.size() > TOP_SUGGESTED_AMENITIES) return;
        }
        System.out.println();   
    }

    /*
     * Returns the expected revenue increase for the added amenity.
     * 
     * Calculates the average listing price increase when the amenity is present
     * in the area vs when it is not present.
     */
    public static double estimateAmenityCost(ArrayList<Integer> listings, String newAmenity) throws ClassNotFoundException, SQLException {
        // Step 1: Find listings with and without the new amenity
        double totalCostWithAmenity = 0;
        int listingsWithAmenity = 0;
        double totalCostWithoutAmenity = 0;
        int listingsWithoutAmenity = 0;

        for (int listing : listings) {
            if (SqlDAO.getInstance().listingOffersAmenity(listing, newAmenity)) {
                totalCostWithAmenity += SqlDAO.getInstance().averageListingCost(listing);
                listingsWithAmenity++;
            } else {
                totalCostWithoutAmenity += SqlDAO.getInstance().averageListingCost(listing);
                listingsWithoutAmenity++;
            }
        }

        // Step 2: Calculate the average increase in listing cost when the amenity is present
        double averageCostWithAmenity = (listingsWithAmenity > 0) ? totalCostWithAmenity / listingsWithAmenity : 0;
        double averageCostWithoutAmenity = (listingsWithoutAmenity > 0) ? totalCostWithoutAmenity / listingsWithoutAmenity : 0;

        double averageAmenityCost = averageCostWithAmenity - averageCostWithoutAmenity;

        return averageAmenityCost;
    }

    /*
     * Returns the average listing price per day in a given radius.
     */
    public static double estimatePriceInArea(double lat, double lon, String type) throws ClassNotFoundException, SQLException {
        double price = 0;
        ArrayList<Integer> listings = new ArrayList<Integer>();
        ResultSet rs = SqlDAO.getInstance().getListingsInRadius(RADIUS, lat, lon, type);
        while (rs.next()) {
            listings.add(rs.getInt("listing_id"));
        }
        rs.close();
        
        for (int i : listings) {
            price += SqlDAO.getInstance().averageListingCost(i);
        }

        return price / listings.size();
    }
}
