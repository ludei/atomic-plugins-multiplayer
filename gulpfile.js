var gulp = require('gulp'); 
var jshint = require('gulp-jshint');
var jsdoc = require('gulp-jsdoc');
var uglify = require('gulp-uglify');
var concat = require('gulp-concat');

gulp.task('build', function () {

    gulp.src(['src/atomic/ios/common/*.h','src/atomic/ios/common/*.m'])
        .pipe(gulp.dest('src/cordova/ios/common/src/deps'));

    gulp.src(['src/atomic/ios/gamecenter/*.h','src/atomic/ios/gamecenter/*.m'])
        .pipe(gulp.dest('src/cordova/ios/gamecenter/src/deps'));

    gulp.src(['src/js/cocoon_multiplayer.js', 'src/js/cocoon_multiplayer_loopback.js'])
    		.pipe(jshint())
    		.pipe(jshint.reporter())
            .pipe(concat('cocoon_multiplayer.js')) 
            .pipe(uglify())
            .pipe(gulp.dest('src/cordova/common/www'));
    gulp.src('src/js/cocoon_multiplayer_gamecenter.js')
            .pipe(jshint())
            .pipe(jshint.reporter())
            .pipe(uglify())
            .pipe(gulp.dest('src/cordova/ios/gamecenter/www'));
    return gulp.src('src/js/cocoon_multiplayer_googleplaygames.js')
            .pipe(jshint())
            .pipe(jshint.reporter())
            .pipe(uglify())
            .pipe(gulp.dest('src/cordova/android/googleplaygames/www'));

});
gulp.task('doc', ["build"], function() {

    var config = require('./doc_template/js/jsdoc.conf.json');

    var infos = {
        plugins: config.plugins
    }

    var templates = config.templates;
    templates.path = 'doc_template/js';

    return gulp.src("src/js/*.js")
      .pipe(jsdoc.parser(infos))
      .pipe(jsdoc.generator('dist/doc/js', templates));

});

gulp.task('default', ['build', 'doc']);
