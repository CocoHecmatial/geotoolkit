This directory is not part of Maven build. Its content is of interest
only to Unix servers publishing reports of Geotoolkit project on the web.
The server should host a clone of the Mercurial repository to be pulled
and updated on a regular basis, typically in a cron table. The server's
web space can contains symbolic links to some files in this directory:

* The directory hosting the download files can contains a symbolic
  link to "download/HEADER.html".

* The directory hosting the Maven repository can contains a symbolic
  link to "repository/HEADER.html".

* The directory hosting javadoc can contains symbolic links to the
  files in the "javadoc" directory.
