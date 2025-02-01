/**
 * Handles the data returned by the API, reads the jsonObject, and populates data into HTML elements.
 * @param resultData jsonObject
 */
function handleMovieList(resultData) {
    console.log("handleCartList: ", resultData); // Log data to verify

    let cartListTableBodyElement = jQuery("#conf_table_body");

    let total = 0;

    for (let i = 0; i < resultData.length; i++) {
        let movie = resultData[i];
        total += movie["total_price"]

        let rowHTML = "<tr>";

        rowHTML += `<th>${movie["sale_id"]}</th>`;

        rowHTML += `<th><a href='movie.html?id=${encodeURIComponent(movie["movie_id"])}'>${movie["title"]}</a></th>`;

        rowHTML += `<th>${movie["quantity"]}</th>`;

        rowHTML += `<th>$${movie["price"]}</th>`;

        rowHTML += `<th>$${movie["total_price"]}</th>`;

        rowHTML += "</tr>";

        cartListTableBodyElement.append(rowHTML);
    }

    $("#total").text(`Total: $${total}`);
}

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: `api/confirmation`,
    success: (resultData) => handleMovieList(resultData)
});
