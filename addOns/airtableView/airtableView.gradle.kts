version = "2"
description = "Adds a view that shows stringified object params on airtable api nicely"

zapAddOn {
    addOnName.set("Airtable View")
    zapVersion.set("2.9.0")

    manifest {
        author.set("Matt Blessed")
        url.set("")

        helpSet {
            baseName.set("help%LC%.helpset")
            localeToken.set("%LC%")
        }
    }
}
