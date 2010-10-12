(ns leiningen.uberwar
  "Leiningen uberwar plugin"
  (:require [leiningen.compile :as compile])
  (:use leiningen.war)
  (:use leiningen.web-xml))

(defn re-filter 
  "Uses the given regular expression to filter a sequence"
  [re s]
  (filter #(re-find re %) s))

(defn re-remove 
  "Uses the given regular expression to filter a sequence.
   Returns strings that do not match the given re."
  [re s]
  (filter #(not (re-find re %)) s))

(defn find-jars
  "Returns the path of all the jars in the given path"
  [dir]
  (re-filter #".jar$" (map str (find-files dir))))

(defn dependency-jars
  "Returns the jar files that are dependencies of a project.
 Removes the dev dependencies in case 'lein clean' has not been run."
  [project]
  (let [dev-pattern #"[\\/]dev[\\/][^\\/]+\.jar"]
    (re-remove dev-pattern (find-jars (:library-path project)))))

(defn uberwar
  "Create a $PROJECT-$VERSION.war file containing the following directory structure:
   destination                   default source              project.clj 
   ----------------------------------------------------------------------------        
   WEB-INF/web.xml               src/web.xml                 :webxml
   WEB-INF/classes               classes                     :compile-path 
   WEB-INF/lib                   lib                         :library-path
   /                             src/html                    :web-content
   WEB-INF/classes               resources                   :resources-path
   WEB-INF/classes               src                         :source-path
  Artifacts listed in :dev-dependencies will not copied into the war file"
  [project & args]
  (autocreate-webxml project)
  (compile/compile project)
  (check-exists (:library-path project))
  (jar (war-name project)
       ["WEB-INF/web.xml" (webxml-path project)]
       [(web-content project)]
       ["WEB-INF/lib/" (:library-path project) (dependency-jars project)]
       ["WEB-INF/classes/" (:compile-path project)]
       ["WEB-INF/classes/" (:resources-path project)]
       ["WEB-INF/classes/" (:source-path project)]))
