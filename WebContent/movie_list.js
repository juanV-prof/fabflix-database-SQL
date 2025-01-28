/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs two steps:
 *      1. Use jQuery to talk to backend API to get the json data.
 *      2. Populate the data to correct html elements.
 */


/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleStarResult(resultData) {
    console.log("handleStarResult: populating star table from resultData");

    // Populate the star table
    // Find the empty table body by id "top20_table_body"
    let movieTableBodyElement = jQuery("#top20_table_body");

    // Iterate through resultData, no more than 10 entries
    for (let i = 0; i < 20; i++) {
        let rowHTML = "<tr>";

        // Add link to each movie title
        rowHTML += "<th><a href='movie.html?id=" + encodeURIComponent(resultData[i]["movie_id"]) + "'>"
            + resultData[i]["title"] + "</a></th>";

        rowHTML += "<th>" + resultData[i]["year"] + "</th>";
        rowHTML += "<th>" + resultData[i]["director"] + "</th>";
        rowHTML += "<th>" + resultData[i]["genres"] + "</th>";

        let stars = resultData[i]["stars"].split(', ');
        let starLinks = stars.map(star => {
            let [name, id] = star.split(':');
            return "<a href='star.html?id=" + encodeURIComponent(id) + "'>" + name + "</a>";
        });
        rowHTML += "<th>" + starLinks.join(', ') + "</th>";

        rowHTML += "<th>" + resultData[i]["rating"] + "</th>";

        rowHTML += "</tr>";


        movieTableBodyElement.append(rowHTML);
    }
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Makes the HTTP GET request and registers on success callback function handleStarResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/top20", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleStarResult(resultData) // Setting callback function to handle data returned successfully by the StarsServlet
});