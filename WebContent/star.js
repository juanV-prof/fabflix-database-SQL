/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs three steps:
 *      1. Get parameter from request URL so it know which id to look for
 *      2. Use jQuery to talk to backend API to get the json data.
 *      3. Populate the data to correct html elements.
 */

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */

function handleResult(resultData) {
    console.log("handleResult: populating star info from resultData");

    // Assuming resultData is an array, retrieve the first object
    const starData = resultData[0];

    // Set <title> and <h1> to  movie title with year in parentheses
    document.title = `${starData["name"]}`; // Update  <title>
    jQuery("h1").text(`${starData["name"]}`); // Update  <h1>


    // Populate the star table
    let movieTableBodyElement = jQuery("#star_table_body");

    let rowHTML = "<tr>";
    rowHTML += "<th>" + starData["dob"] + "</th>";

    let stars = starData["movies"].split(', ');
    let movieLinks = stars.map(star => {
        let [movieName, id] = star.split('::');
        return "<a href='movie.html?id=" + encodeURIComponent(id) + "'>" + movieName + "</a>";
    });
    rowHTML += "<th>" + movieLinks.join(', ') + "</th>";
    rowHTML += "</tr>";

    // Append the generated HTML to the table body
    movieTableBodyElement.append(rowHTML);
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
const starId = new URLSearchParams(window.location.search).get("id");

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: `api/star?id=${starId}`, // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
});