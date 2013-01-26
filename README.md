Leiningen war plugin
====================

This is a dead and unsupported project. Developers who are new to clojure web development are recommended to try the excellent [lein-ring](https://github.com/weavejester/lein-ring) plugin. If you are mixing Clojure with servlets then the [lein-servlet](https://github.com/kumarshantanu/lein-servlet) plugin should fit your needs.
 

Old Readme
==========

This plugin creates standard war files for use with java web application servers 
and is not useful when developing for the Google App Engine. 

This plugin is available at [http://clojars.org/](http://clojars.org/uk.org.alienscience/leiningen-war)

    :dev-dependencies [[uk.org.alienscience/leiningen-war "0.0.13"]]

An example application using the plugin is at: [http://github.com/alienscience/compojure-war-example](http://github.com/alienscience/compojure-war-example).
 
The best place for discussion and support is the Clojure web development google group: [http://groups.google.com/group/clojure-web-dev](http://groups.google.com/group/clojure-web-dev)

This plugin adds three commands to leiningen:

lein web-xml
------------

Create a web.xml file if one does not already exist. For most non-trivial
applications this file will need to be edited manually.

By default the file is created in src/web.xml but this can be overidden
by setting `:webxml` in your `:war` configuration in project.clj.

The servlet class is assumed to be the first entry in the
`:aot` setting given in project.clj.

lein uberwar
------------

Create a war file containing the following directory structure:

    destination                   default source              project.clj 
    ----------------------------------------------------------------------------        
    WEB-INF/web.xml               src/web.xml                 :war {:webxml}
    WEB-INF/classes               classes                     :compile-path 
    WEB-INF/lib                   lib                         :library-path
    /                             src/html                    :war {:web-content}
    WEB-INF/classes               resources                   :resources-path
    WEB-INF/classes               src                         :source-path

Artifacts listed in `:dev-dependencies` will not copied into the war file. The name
of the war file defaults to $PROJECT-$VERSION.war, however, it can be overridden
by setting `:war :name` in project.clj.

lein war
--------

This command does not include dependencies in the war file and is intended for cases
where the servlet container classpath is setup manually.

Create a war file containing the following directory structure:

    destination                 default source         project.clj 
    ---------------------------------------------------------------------        
    WEB-INF/web.xml             src/web.xml            :war {:webxml}
    WEB-INF/classes             classes                :compile-path 
    /                           src/html               :war {:web-content}
    WEB-INF/classes             resources              :resources-path
    WEB-INF/classes             src                    :source-path

Simple Example
==============

    (defproject example "0.0.1"
      :dependencies [[org.clojure/clojure "1.2.0"]
                     [org.clojure/clojure-contrib "1.2.0"]]
      :dev-dependencies [[uk.org.alienscience/leiningen-war "0.0.12"]])

`lein war` will create a war file with the following structure:

    WEB-INF/
    WEB-INF/web.xml   <--- taken from src/web.xml
    WEB-INF/classes   <---- taken from classes
    index.html        <---- taken from src/html/index.html

`lein uberwar` will create a similar directory structure with the addition:

    WEB-INF/lib       <----  taken from lib

Overiding Defaults
==================
    
    (defproject example "0.0.1"
      :dependencies [[org.clojure/clojure "1.2.0"]
                     [org.clojure/clojure-contrib "1.2.0"]]
      :dev-dependencies [[uk.org.alienscience/leiningen-war "0.0.12"]]
      :war {:webxml "war/example.xml"
            :web-content "html"}
      :compile-path  "build"
      :library-path  "libs"
      :resources-path "war/resources")

`lein war` will create a war file with the following structure:

    WEB-INF/
    WEB-INF/web.xml           <--- taken from war/example.xml
    WEB-INF/classes           <---- taken from build
    index.html                <---- taken from html/index.html
    WEB-INF/classes/templates <---- taken from war/resources/templates

`lein uberwar` will create a similar directory structure with the addition:

    WEB-INF/lib               <----  taken from libs

War name
--------

The default filename used for the .war file is $PROJECT-$VERSION.war.  You can change this
by specifying the `:name` key in your `:war` configuration in project.clj.  Don't forget to
include ".war" on the end of the name.
