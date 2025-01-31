/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs two steps:
 *      1. Use jQuery to talk to backend API to get the json data.
 *      2. Populate the data to correct html elements.
 */


let urlParams = new URLSearchParams(window.location.search);
let currentPage;

// If pageNumber is in URL, use it; otherwise, check sessionStorage
if (urlParams.has("pageNumber")) {
    currentPage = parseInt(urlParams.get("pageNumber")) || 1;
    sessionStorage.setItem("pageNumber", currentPage);
} else {
    let storedPage = sessionStorage.getItem("pageNumber");
    currentPage = storedPage ? parseInt(storedPage) : 1;
    sessionStorage.setItem("pageNumber", currentPage);
}

// Update URL to ensure pageNumber is always present
urlParams.set("pageNumber", currentPage);
window.history.replaceState(null, "", "results.html?" + urlParams.toString());


document.getElementById("nextButton").addEventListener("click", function () {
    let urlParams = new URLSearchParams(window.location.search);

    currentPage++;
    urlParams.set("pageNumber", currentPage);

    sessionStorage.setItem("pageNumber", currentPage); // Store it

    window.history.replaceState(null, "", "results.html?" + urlParams.toString());

    getMovies();
});

document.getElementById("prevButton").addEventListener("click", function () {
    let urlParams = new URLSearchParams(window.location.search);

    if (currentPage > 1) {
        currentPage--;
        urlParams.set("pageNumber", currentPage);

        sessionStorage.setItem("pageNumber", currentPage); // Store it

        window.history.replaceState(null, "", "results.html?" + urlParams.toString());

        getMovies();
    }
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

    let urlParams = new URLSearchParams(window.location.search);
    let title = urlParams.get("title");
    let director = urlParams.get("director");
    let year = urlParams.get("year");
    let star = urlParams.get("star");
    let genre = urlParams.get("genre");
    let prefix = urlParams.get("prefix");
    let pageNumber = urlParams.get("pageNumber");
    let moviesPerPage = urlParams.get("moviesPerPage")
    let sortBy = urlParams.get("sortBy")

    if (title) params.append("title", title);
    if (director) params.append("director", director);
    if (year) params.append("year", year);
    if (star) params.append("star", star);
    if (genre) params.append("genre", genre);
    if (prefix) params.append("prefix", prefix);
    if (pageNumber) params.append("pageNumber", pageNumber);
    if (moviesPerPage) params.append("moviesPerPage", moviesPerPage);
    if (sortBy) params.append("sortBy", sortBy);

    // Makes the HTTP GET request and registers on success callback function handleStarResult
    jQuery.ajax({
        dataType: "json", // Setting return data type
        method: "GET", // Setting request method
        url: `api/results?${params.toString()}`, // Setting request url, which is mapped by results in ResultsServlet.java
        success: (resultData) => handleMovieListResults(resultData) // Setting callback function to handle data returned successfully by the MovieListServlet
    });
}

getMovies();