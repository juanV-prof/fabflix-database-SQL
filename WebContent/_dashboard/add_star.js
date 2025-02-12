
$(document).ready(function() {
    $('#starForm').on('submit', function(event) {
        event.preventDefault();
        handleStarFormSubmit();
    });
});

function handleStarFormSubmit() {
    var starName = $('#starName').val();
    var birthYear = $('#birthYear').val();

    $.ajax({
        url: '../api/add_star',
        type: 'POST',
        data: {
            starName: starName,
            birthYear: birthYear
        },
        success: function(response) {
            console.log(response);
            alert('Star added successfully!');
        },
        error: function(xhr, status, error) {
            console.error(error);
            alert('Error adding star!');
        }
    });
}