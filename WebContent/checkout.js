/**
 * Handles the data returned by the API, reads the jsonObject, and populates data into HTML elements.
 * @param resultData jsonObject
 */
function handleCartList(resultData) {
    console.log("handleCartList: ", resultData); // Log data to verify

    let cartListTableBodyElement = jQuery("#cart_table_body");

    let total = 0;

    for (let i = 0; i < resultData.length; i++) {
        let movie = resultData[i];
        let totalPrice = movie["quantity"] * movie["price"];
        total += totalPrice;

        let rowHTML = "<tr>";

        rowHTML += `<th><a href='movie.html?id=${encodeURIComponent(movie["movie_id"])}'>${movie["title"]}</a></th>`;

        rowHTML += `<th>
                        <button class="decrease-button" data-movie-id="${movie["movie_id"]}">-</button>
                        <span class="quantity" id="quantity-${movie["movie_id"]}">${movie["quantity"]}</span>
                        <button class="increase-button" data-movie-id="${movie["movie_id"]}">+</button>
                    </th>`;

        rowHTML += `<th><button class="delete-button" data-movie-id="${movie["movie_id"]}">Delete</button></th>`;

        rowHTML += `<th>$${movie["price"]}</th>`;

        rowHTML += `<th id="total-${movie["movie_id"]}">$${totalPrice}</th>`;

        rowHTML += "</tr>";

        cartListTableBodyElement.append(rowHTML);
    }

    $("#total").text(`Total: $${total}`);
}

$(document).ready(function () {
    $(document).on("click", ".increase-button", function () {
        let movieId = $(this).data("movie-id");
        updateQuantity(movieId, 1);
    });

    $(document).on("click", ".decrease-button", function () {
        let movieId = $(this).data("movie-id");
        updateQuantity(movieId, -1);
    });

    $(document).on("click", ".delete-button", function () {
        let movieId = $(this).data("movie-id");

        $.ajax({
            type: "POST",
            url: "api/cart",
            data: {movieId: movieId, action: "remove"},
            success: function () {
                location.reload();
            },
            error: function () {
                alert("Failed to remove movie from cart.");
            }
        });
    });
});

function updateQuantity(movieId, change) {
    let quantityElement = $(`#quantity-${movieId}`);
    let totalElement = $(`#total-${movieId}`);

    let totalPrice = parseFloat(totalElement.text().replace("$", ""));
    let quantity = parseInt(quantityElement.text());

    let price = totalPrice / quantity;

    let newQuantity = quantity + change;
    if (newQuantity < 1) newQuantity = 1;

    quantityElement.text(newQuantity);

    let newTotal = (newQuantity * price);
    totalElement.text(`$${newTotal}`);

    updateTotal();

    $.ajax({
        type: "POST",
        url: "api/cart",
        data: {movieId: movieId, action: "update", quantity: newQuantity},
        error: function () {
            alert("Failed to update quantity.");
        }
    });
}

function updateTotal() {
    let newTotal = 0;

    $(".quantity").each(function () {
        let movieId = $(this).attr("id").replace("quantity-", "");
        let quantity = parseInt($(this).text());

        let totalPrice = parseFloat($(`#total-${movieId}`).text().replace("$", ""));
        let pricePerItem = totalPrice / quantity;

        newTotal += quantity * pricePerItem;
    });

    $("#total").text(`Total: $${newTotal}`);
}

document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("payButton").addEventListener("click", function () {
        const totalAmount = document.getElementById("total").innerText.replace("Total: $", "");
        sessionStorage.setItem("totalAmount", totalAmount);
        window.location.href = "payment.html";
    });
});

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: `api/checkout`,
    success: (resultData) => handleCartList(resultData)
});
