(ns leiningen.war
  "Leiningen war plugin"
  (:use [clojure.contrib.duck-streams :only [to-byte-array copy]]
        [clojure.contrib.str-utils :only [str-join re-sub re-gsub]]
        [clojure.contrib.java-utils :only [file]])
  (:use leiningen.web-xml)
  (:require [leiningen.compile :as compile])
  (:import [java.util.jar Manifest JarEntry JarOutputStream]
           [java.io BufferedOutputStream 
                    FileOutputStream 
                    ByteArrayInputStream]))


(defn no-trailing-slash [path] (re-sub #"/$" "" path))
(defn no-leading-slash [path] (re-sub #"^/" "" path))
(defn no-double-slash [path] (re-sub #"//" "/" path))
(defn unix-path [path] (re-gsub #"\\" "/" path))
(defn has-trailing-slash [path] (re-find #"/$" path))

(defn find-files 
  "Returns all files in and below the given directory. If 
passed the path of a file returns a sequence containing a single 
file object."
  [path] 
  (file-seq (file path)))

(def dirs-in-jar)

(defn add-dir-to-jar 
  "Adds the given directory to the given jar output stream"
  [jar-os dir]
  (if-not (contains? dirs-in-jar dir)
    (do
      (set! dirs-in-jar (assoc dirs-in-jar dir true))
      (.putNextEntry jar-os (JarEntry. dir)))))
  
(defn add-file-to-jar
  "Adds a file/directory to the given jar output stream"
  [jar-os f dest-path]
  (when-not (empty? dest-path)
    (cond (has-trailing-slash dest-path)
	  (add-dir-to-jar jar-os dest-path)
	  ;;--------
	  (.isDirectory f)
	  (let [dest-dir (no-double-slash (str dest-path "/"))]
	    (add-dir-to-jar jar-os dest-dir))
	  ;;--------
	  (.exists f)
	  (do
	    (.putNextEntry jar-os (JarEntry. dest-path))
	    (copy f jar-os)))))

(defn jar-destination
  "Returns the destination path in a jar file when given the
 top level jar path, the directory being added to the jar and
 the path of the file being added."
  [jar-path directory file-path]
  (let [dir-re (re-pattern (str "^" (no-trailing-slash (unix-path directory))))
        dest-path (->> file-path
                       (unix-path)
                       (re-sub dir-re jar-path)
                       (no-leading-slash)
                       (no-double-slash))]
      dest-path))
 
(defn add-tree-to-jar 
  "Adds the contents of a directory (or a single file) to the given 
 jar output stream"
  [jar-os jar-path directory]
  (doseq [f (find-files directory)]
    (add-file-to-jar jar-os f 
                     (jar-destination jar-path directory (str f)))))

(defn add-files-to-jar
  "Adds the given files to the given jar output stream"
  [jar-os jar-path directory files]
  (doseq [path files]
    (add-file-to-jar jar-os (file path)
                     (jar-destination jar-path directory path))))

(defn add-entry-to-jar
  "Adds an entry into the given jar output stream"
  ([jar-os directory]
     (add-entry-to-jar jar-os "" directory))
  ([jar-os entry-path directory]
     (add-tree-to-jar jar-os entry-path directory))
  ([jar-os entry-path directory files]
     (add-files-to-jar jar-os entry-path directory files)))

(defn make-manifest []
  (Manifest.
   (ByteArrayInputStream.
    (to-byte-array 
     (str 
      "Manifest-Version: 1.0" \newline
      "Created-By: Leiningen War Plugin" \newline
      "Built-By: " (System/getProperty "user.name") \newline
      "Build-Jdk: " (System/getProperty "java.version") \newline
      \newline)))))

(defn create-jar [path]
  (JarOutputStream. (BufferedOutputStream. (FileOutputStream. path))
                    (make-manifest)))

(defn jar
  "Create a jar file with entries described by vectors in one of 
   the forms:
   [dest-path? directory]
   [dest-path? directory [file1 file2 ...]]
   [dest-path? file]" 
  [dest & entries] 
  (with-open [jar-os (create-jar dest)]
    (binding [dirs-in-jar {}]
      (doseq [entry entries]
        (apply add-entry-to-jar jar-os entry)))))

(defn war-name
  "Returns the name of the war file to create"
  [project]
  (str (:name project) "-" (:version project) ".war"))


(defn web-content 
  "Returns the path of the directories containing web 
 content that will be put into the war file"
  [project]
  (or (:web-content project) "src/html"))

(defn check-exists
  "Check that the given file exists - warn if not"
  [f]
  (if (not (.exists (file f))) 
    (println "[WARNING]" (str f) "does not exist.")))

(defn war
  "Create a $PROJECT-$VERSION.war file containing the following directory structure:
   destination                 default source         project.clj 
   ---------------------------------------------------------------------        
   WEB-INF/web.xml             src/web.xml            :webxml
   WEB-INF/classes             classes                :compile-path 
   /                           src/html               :web-content
   WEB-INF/classes             resources              :resources-path
   WEB-INF/classes             src                    :source-path"
  [project & args]
  (autocreate-webxml project)
  (compile/compile project)
  (jar (war-name project)
       ["WEB-INF/web.xml" (webxml-path project)]
       ["WEB-INF/classes/" (:compile-path project)]
       ["WEB-INF/classes/" (:resources-path project)]
       ["WEB-INF/classes/" (:source-path project)]
       [(web-content project)]))
