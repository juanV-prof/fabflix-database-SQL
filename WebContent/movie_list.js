/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs two steps:
 *      1. Use jQuery to talk to backend API to get the json data.
 *      2. Populate the data to correct html elements.
 */


let currentPage = 1;


document.getElementById("nextButton").addEventListener("click", function () {
    currentPage++; // Increment the currentPage
    getMovies();
});

document.getElementById("prevButton").addEventListener("click", function () {
    currentPage--; // Increment the currentPage
    getMovies();
});


/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleMovieListResults(resultData) {
    console.log("handleStarResult: populating star table from resultData");

    // Find the empty table body by id "results_table_body"
    let movieTableBodyElement = jQuery("#results_table_body");
    let moviesPerPageSelection = parseInt($("#moviesPerPage").val());

    movieTableBodyElement.empty();


    // Iterate through resultData, no more than n entries
    for (let i = 0; i < resultData.length; i++) {
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

        rowHTML += `<th>
        <button class="add-button" data-movie-id="${resultData[i]["movie_id"]}">add</button>
        </th>`;

        rowHTML += "</tr>";


        movieTableBodyElement.append(rowHTML);
    }

    if (currentPage === 1) {
        $("#prevButton").prop("disabled", true);
    } else {
        $("#prevButton").prop("disabled", false);
    }

    if (resultData.length < moviesPerPageSelection) {
        $("#nextButton").prop("disabled", true);
    } else {
        $("#nextButton").prop("disabled", false);
    }
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

function getMovies() {
    let params = new URLSearchParams();
    let moviesPerPage = $("#moviesPerPage").val();

    params.append("page", currentPage);
    params.append("moviesPerPage", moviesPerPage);

    let urlParams = new URLSearchParams(window.location.search);
    let title = urlParams.get("title");
    let director = urlParams.get("director");
    let year = urlParams.get("year");
    let star = urlParams.get("star");
    let genre = urlParams.get("genre");
    let prefix = urlParams.get("prefix");

    if (title) params.append("title", title);
    if (director) params.append("director", director);
    if (year) params.append("year", year);
    if (star) params.append("star", star);
    if (genre) params.append("genre", genre);
    if (prefix) params.append("prefix", prefix);

    // Makes the HTTP GET request and registers on success callback function handleStarResult
    jQuery.ajax({
        dataType: "json", // Setting return data type
        method: "GET", // Setting request method
        url: `api/movie_list?${params.toString()}`, // Setting request url, which is mapped by movie_list in MovieListServlet.java
        success: (resultData) => handleMovieListResults(resultData) // Setting callback function to handle data returned successfully by the MovieListServlet
    });
}

getMovies();