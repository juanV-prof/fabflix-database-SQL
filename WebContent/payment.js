/**
 * Handles the data returned by the API, reads the jsonObject, and populates data into HTML elements.
 */
document.addEventListener("DOMContentLoaded", function () {
    const totalAmount = sessionStorage.getItem("totalAmount");

    if (totalAmount) {
        $("#total").text(`Final Payment: $${totalAmount}`);
    } else {
        $("#total").text("Final Payment: $0");
    }

    $(".place-order").click(function () {
        const cardNumber = $("#cardNumber").val();
        const firstName = $("#firstName").val();
        const lastName = $("#lastName").val();
        const expiryDate = $("#expiryDate").val();

        $.ajax("api/process-payment", {
            method: "POST",
            data: {
                cardNumber: cardNumber,
                firstName: firstName,
                lastName: lastName,
                expiryDate: expiryDate,
            },
            success: function (resultData) {
                if (resultData.success == "true" || resultData.success === true) {
                    window.location.href = "confirmation.html";
                } else {
                    alert("Credit card information not found");
                }
            },
        });
    });
});
