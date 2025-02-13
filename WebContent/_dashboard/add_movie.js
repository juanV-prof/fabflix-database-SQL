
$(document).ready(function() {
    $('#movieForm').on('submit', function(event) {
        event.preventDefault();
        handleMovieFormSubmit();
    });
});

function handleMovieFormSubmit() {
    var movieName = $('#movieName').val();
    var movieYear = $('#movieYear').val();
    var directorName = $('#directorName').val();
    var starName = $('#starName').val();
    var birthYear = $('#birthYear').val();
    var genreName = $('#genreName').val();

    $.ajax({
        url: '../api/add_movie',
        type: 'POST',
        data: {
            title: movieName,
            year: movieYear,
            director: directorName,
            starName: starName,
            birthYear: birthYear,
            genreName: genreName
        },
        success: function(response) {
            console.log(response);
            if (response.movieId && response.starId && response.genreId) {
                alert(`Movie added successfully! \nMovie ID: ${response.movieId} \nStar ID: ${response.starId} \nGenre ID: ${response.genreId}`);
            } else {
                alert('Movie added successfully.');
            }
        },
        error: function(xhr, status, error) {
            console.error(error);
            alert('Error adding movie!');
        }
    });
}