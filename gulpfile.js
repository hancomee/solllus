var gulp = require('gulp'),
    sass = require('gulp-sass');

var ps = (function () {
    var root = 'src/main/resources/static/';
    return {
        src: root + 'src/',
        dist: root + 'dist/',
        root: root
    }
})();


gulp.task('compileSass', function () {
    return gulp.src('web-resources/style/**/*.scss')
        .pipe(sass().on('error', sass.logError))
        .pipe(gulp.dest('src/main/resources/static/dist/'));
});

