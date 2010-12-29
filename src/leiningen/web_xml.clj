
(ns leiningen.web-xml
  "Leiningen war plugin - create web.xml file"
  (:use [clojure.contrib.strint])
  (:use [clojure.contrib.java-utils :only [file]]))

(defn webxml-path
  "Returns the path of the web.xml to use in the war file"
  [project]
  (or (-> project :war :webxml-path) "src/web.xml"))

(defn- generate-webxml
  "Returns the xml for a web.xml file."
  [servlet-name servlet-class]
  (<< "<web-app>
  <!-- Servlet class taken from first :aot namespace -->
  <servlet>
     <servlet-name>~{servlet-name}</servlet-name>
     <servlet-class>~{servlet-class}</servlet-class>
  </servlet>
  <!-- Servlet is mapped to / by default  -->
  <servlet-mapping>
     <servlet-name>~{servlet-name}</servlet-name>
     <url-pattern>/*</url-pattern>
  </servlet-mapping>
</web-app>\n"))

(defn- required-key
  "Extracts a compulsory key from a project definition."
  [project key default]
  (if (contains? project key)
    (project key)
    (do
      (println "[WARNING]" "project.clj does not contain" key)
      default)))

(defn- create-webxml
  "Create a web.xml file from a project definition."
  [project]
  (let [servlet-name (required-key project :name "unknown")
        servlet-class (first (required-key project :aot ["unknown"]))]
    (spit (webxml-path project)
          (generate-webxml servlet-name servlet-class))))

(defn autocreate-webxml
  "Automatically creates a web.xml if it doesn't exist"
  [project]
  (let [pth (webxml-path project)]
    (if-not (.exists (file pth))
      (do
        (println "[INFO]" pth "does not exist, will create a default.")
        (create-webxml project)))))

(defn web-xml
  "Create a web.xml file if one does not already exist.
   By default the file is created in src/web.xml but this can be overidden
   by setting :webxml in project.clj.
   The servlet class is assumed to be the first entry in the
   :aot setting given in project.clj."
  [project & args]
  (let [pth (webxml-path project)]
    (if (.exists (file pth))
      (println "[ERROR]" pth "exists, will not overwrite.")
      (create-webxml project))))
  
