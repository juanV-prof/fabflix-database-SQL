/**
 * Handles the data returned by the API, reads the jsonObject, and populates data into HTML elements.
 * @param resultData jsonObject
 */
function handleMovieResult(resultData) {
    console.log("handleMovieResult: ", resultData); // Log data to verify

    // Assuming resultData is an array, retrieve the first object
    const movieData = resultData[0];

    // Set <title> and <h1> to  movie title with year in parentheses
    document.title = `${movieData["title"]}`; // Update  <title>
    jQuery("h1").text(`${movieData["title"]} (${movieData["year"]})`); // Update  <h1>

    // Populate the movie table
    let movieTableBodyElement = jQuery("#movie_table_body");

    let rowHTML = "<tr>";
    rowHTML += "<th>" + movieData["director"] + "</th>";
    rowHTML += "<th>" + movieData["genres"] + "</th>";

    let stars = movieData["stars"].split(', ');
    let starLinks = stars.map(star => {
        let [name, id] = star.split(':');
        return "<a href='star.html?id=" + encodeURIComponent(id) + "'>" + name + "</a>";
    });
    rowHTML += "<th>" + starLinks.join(', ') + "</th>";

    rowHTML += "<th>" + movieData["rating"] + "</th>";

    rowHTML += `<th>
        <button class="add-button" data-movie-id="${movieData["movie_id"]}">add</button>
    </th>`;

    rowHTML += "</tr>";

    // Append the generated HTML to the table body
    movieTableBodyElement.append(rowHTML);
}

$(document).ready(function () {
    // When add on a movie is clicked
    $(document).on("click", ".add-button", function () {
        let movieId = $(this).data("movie-id");

        $.ajax({
            type: "POST",
            url: "api/cart",
            data: {movieId: movieId, action: "add"},
            success: function (response) {
                alert("Movie added to cart!");
            },
            error: function () {
                alert("Failed to add movie to cart.");
            }
        });
    });
});


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
