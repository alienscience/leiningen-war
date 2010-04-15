(ns leiningen.uberwar
  "Leiningen uberwar plugin"
  (:use leiningen.war))

(defn re-filter 
  "Uses the given regular expression to filter a sequence"
  [re s]
  (filter #(re-find re %) s))

(defn find-jars
  "Returns the path of all the jars in the given path"
  [dir]
  (re-filter #".jar$" (map str (find-files dir))))

(defn dev-artifacts
  "Returns the artifact names of the dev-dependencies of 
 the given project"
  [project]
  (map #(re-find #"[^/]+$" %)
       (map #(str (first %)) (:dev-dependencies project))))

(defn matches-a-pattern
  "Indicates if a string contains any of the patterns 
 in the given sequence"
  [s patterns]
  (some #(re-find  % s) patterns))

(defn dependency-jars
  "Returns the jar files that are dependencies of a project.
 Removes the top level dev dependencies in case 'lein clean'
 has not been run."
  [project]
  (let [dev-patterns (map re-pattern (dev-artifacts project))]
    (filter #(not (matches-a-pattern % dev-patterns))
            (find-jars (:library-path project)))))

(defn uberwar
  "Create a $PROJECT-$VERSION.war file containing the following directory structure:
   destination                   default source              project.clj 
   ----------------------------------------------------------------------------        
   WEB-INF/web.xml               src/web.xml                 :webxml
   WEB-INF/appengine-web.xml     src/appengine-web.xml       :appengine-webxml
   WEB-INF/classes               classes                     :compile-path 
   WEB-INF/lib                   lib                         :library-path
   /                             src/html                    :web-content
   WEB-INF/classes               resources                   :resources-path
  Artifacts listed in :dev-dependencies will not copied into the war file"
  [project & args]
  (check-exists (webxml project))
  (check-exists (:library-path project))
  (jar (war-name project)
       ["WEB-INF/web.xml" (webxml project)]
       ["WEB-INF/appengine-web.xml" (appengine-webxml project)]
       [(web-content project)]
       ["WEB-INF/lib/" (:library-path project) (dependency-jars project)]
       ["WEB-INF/classes/" (:compile-path project)]
       ["WEB-INF/classes/" (:resources-path project)]))
