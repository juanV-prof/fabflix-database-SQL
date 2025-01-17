/**
 * Handles the data returned by the API, reads the jsonObject, and populates data into HTML elements.
 * @param resultData jsonObject
 */
function handleMovieResult(resultData) {
    console.log("handleMovieResult: ", resultData); // Log data to verify

    // Assuming resultData is an array, retrieve the first object
    const movieData = resultData[0];

    // Set <title> and <h1> to  movie title with year in parentheses
    const titleWithYear = `${movieData["title"]} (${movieData["year"]})`;
    document.title = titleWithYear; // Update  <title>
    jQuery("h1").text(titleWithYear); // Update  <h1>

    // Populate the movie table
    let movieTableBodyElement = jQuery("#movie_table_body");

    let rowHTML = "<tr>";
    rowHTML += "<th>" + movieData["director"] + "</th>";
    rowHTML += "<th>" + movieData["genres"] + "</th>";
    rowHTML += "<th>" + movieData["stars"] + "</th>";
    rowHTML += "<th>" + movieData["rating"] + "</th>";
    rowHTML += "</tr>";

    // Append the generated HTML to the table body
    movieTableBodyElement.append(rowHTML);
}


/**
 * Once this .js is loaded, the following scripts will be executed by the browser.
 */
// Extract movieId from the query string
const movieId = new URLSearchParams(window.location.search).get("id");

// Makes the HTTP GET request and registers the success callback function handleMovieResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: `api/movie?id=${movieId}`, // Setting request URL for the specific movie
    success: (resultData) => handleMovieResult(resultData) // Setting callback function to handle data returned successfully
});
