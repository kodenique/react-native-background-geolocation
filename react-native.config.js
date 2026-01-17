module.exports = {
    dependency: {
        platforms: {
            android: {
                sourceDir: "./android/lib"
            }
        }
        // Removed deprecated hooks (postlink/postunlink) - not needed with autolinking
    }
};