module.exports = {
	dependency: {
		platforms: {
			ios: {}, // No need to specify 'project' anymore as autolinking handles this
			android: {
				sourceDir: './platforms/android'
			}
		}
	}
};
