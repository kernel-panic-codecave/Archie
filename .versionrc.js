const versionRegex = /version=([\d.]+)/;
const gradleFile = 'gradle.properties';

const gradleFileUpdater = {
    filename: gradleFile,
    type: "custom",
    updater: {
        readVersion: function (contents) {
            const match = contents.match(versionRegex)

            if (match && match[1]) {
                return match[1];
            }

            throw Error(`Could not read version from ${gradleFile}`);
        },
        writeVersion: function (contents, version) {
            return contents.replace(versionRegex, `version=${version}`);
        }
    }
}

module.exports = {
    header: "# Changelog",
    packageFiles: [gradleFileUpdater],
    bumpFiles: [gradleFileUpdater]
}