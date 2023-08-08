### User manual

How to use the system:

Make sure you have MySql installed on your device, this project works with version 8.0.33 as of August 8th, 2023. To connect to the database, you need to set up the correct connection variables in the SqlDAO.java file. 




Next, open MyBnb.java and run this class to start the project in the terminal.

After running the system in your terminal, a menu of options will be displayed:

```
Functions supported:
user/create              user/delete              user/show                user/review
user/seereview           listing/search           listing/book             bookings/show
bookings/completestay    bookings/cancel          listing/create           listing/remove
listing/update           listing/review           listing/seereview        reports
exit
```


Start off by selecting “user/create” to create your profile on the system. You can select a function listed by typing them into the terminal	

```
Select a function: user/create

Enter your SIN:
```

Each function will display a series of prompts, enter the value when requested. If a specific format is required, it will be shown in brackets, for example: (YYYY-MM-DD) for a date. 

The list of functions can be grouped into 3 main categories meant for the user and the function reports which is meant for system admins



* user/ functions
    * user/create: creates a new user identified by their SIN
    * user/delete: deletes the user that has a certain SIN. Note that you cannot delete a user if: This user has listings and/or bookings that are not removed/cancelled.
    * user/show: shows all users
    * user/review: to leave a review on a user. Note that you must have completed a stay on a listing hosted by this user to be able to review them
    * user/seereview: to see a review on a user
* listing/ functions
    * listing/search: to search for all listings matching the requirements given by the user. After selecting the function, users will be able to enable a list of filters to narrow their search and sort the result.
        * Filters: distance, postal code, type, city, country, window of availability, price ranges, exact address.
        * You can then sort the output of your search by: ascending distance, descending distance, ascending cost, descending cost
    * listing/book: to book a listing
    * listing/create: to create a new listing
    * listing/remove: to remove a listing
    * listing/update: to update the availability or price of an existing listing
    * listing/review: to leave a review under a listing. Note that the user must have completed a stay in the listing before being able to leave a review. There is also a limit on the number of characters(255) you can write
    * listing/seereview: to see the reviews left on a listing
* bookings/ functions
    * bookings/show: shows all bookings relevant to a given user. When you input the SIN of the user, you will see all bookings this user made, and all bookings made on this user’s listings.
    * bookings/completestay: allows the user who booked a listing to mark their stay as completed, which would then allow them to leave a review on the host and listing.
    * bookings/cancel: allows the user who booked a listing to cancel their booking. The host of the booking can also cancel bookings from their renters.
* reports function can give reports on
    * Total number of bookings in a specific date range sorted by city or by city and zip
    * The total number of listings per country, per country and city as well as per country, city and postal code
    * The ranks of hosts by the total number of listings they have overall per country or per country and city
    * The hosts that have number of listings more than 10% of total listings in that city and country
    * The ranks of renters by total number of bookings they have that start and end within a specific date range
    * The ranks of renters by total number of bookings they have that start and end within a specific date range per city. Note that only users that have made at least two bookings in the year of the starting date range are considered.
    * The hosts and renters with the largest number of cancellations within a given year
    * The words in a listing review sorted by frequency of each listing

Host toolkit: The Host toolkit will use the surrounding listings to suggest amenities and prices that exist in the area where the new listing is being created. In the case where there are no listings within the radius of the area, no price or amenities will be suggested

The HostToolkit has the following functionalities to help hosts:



* Suggest amenities: When a new listing is created, the HostToolkit queries the database of existing listings of the same type and suggests the most popular amenities within a 300 km radius of the listing that is being created. Currently, the top 50% amenities are suggested, along with their expected increase in listing price, should the host choose to add them. We calculate the expected increase in cost by calculating the average increase in listing cost when the amenity is present, versus when the amenity is not present. 
    * Count number of listings with and without amenity, calculate the sum of listing prices with and without the amenity, and then return averageCostWithAmenity - averageCostWithoutAmenity
* Suggest listing prices in an area: When a new listing is created, we suggest a price for this listing type in a 300 km radius of the listing by returning the average listing price in this radius. Since listings can have different prices for different availability ranges, we estimate a listing price by taking the sum of all these prices divided by the number of days in total that the listing is available.
