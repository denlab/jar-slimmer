(ns jar-slimmer.core
  (:use [clargon.core])
  (:use [clojure.contrib.jar])
  (:use [clojure.java.io])
  (:import (java.io FileOutputStream FileInputStream BufferedOutputStream BufferedInputStream))
  (:import (java.util.zip ZipOutputStream ZipInputStream ZipEntry))
  (:gen-class))

;; ------------------- <pure-functions> --------------------------------

(defn sorted-set? "true if input is a sorted set, false otherwise"
  [s] (and (set? s) (sorted? s)))

(defn compl
  "A set which is the complement of minus to all"
  [all minus]
  {:pre [(sorted-set? all) (sorted-set? minus)]}
  (apply sorted-set (remove #(minus %) all)))

(defn true-without?
  [all minus f] "Return true if f applied to the complement of minus to all is true, false otherwise"
  (f (compl all minus)))

(defn half
  "Return the first or second half of the given seq"
  [s f] (apply sorted-set
               (f (split-at (bit-shift-right (count s) 1)
                            s))))

(defn first-half
  "Return the 1st half of the given seq"
  [s] (half s first))

(defn second-half
  "Return the 1st half of the given seq"
  [s] (half s second))

;; WARN: non-TCO optimized recursion, will blow the stack for deep trees !
(defn find-unused
  "Find all elements of the set s for which (f (- s unused)) is true."
  ([s f] (apply sorted-set (find-unused s s f)))
  ([all seg f] (cond (empty? seg)              (sorted-set)
                     (true-without? all seg f) seg
                     (nil? (next seg))         (sorted-set)
                     :otherwise                (concat (find-unused all (first-half seg) f)
                                                       (find-unused all (second-half seg) f)))))

(defn smallest
  "Return the smallest sub seq of the given seq for which the given function return true"
  [s f] (compl s (find-unused s f)))

;; ------------------- </pure-functions> -------------------------------

;; --------------------- <side-effects> --------------------------------

(defn -main [& args]
  (let [opts
        (clargon
         args
         (required ["-j" "--jar" "jar to test"])
         (required ["-c" "--cmd" "Cmd to test the jar"]))])
  )

;; TODO : use clojure.java.shell.sh
(defn run-cmd
  "Run the given cmd, and return the exit code."
  [s] (.waitFor (.exec (Runtime/getRuntime) s)))

(defn jar-list
  "Return the list of the files contained in the given jar"
  [j] (filenames-in-jar (java.util.jar.JarFile. j)))

(defn smallest-jar-list
  "Return the smallest possible resource list corresponding to the given cmd and jar"
  [c j] (smallest (jar-list j) #(run-cmd c)))

(defn build-jar
  "Given a jar and a list of resource, build the jarout jar with the listed resource"
  [jarin jarout l]
  (with-open
      [fis (FileInputStream. jarin)   bis (BufferedInputStream. fis)  zis (ZipInputStream. bis)
       fos (FileOutputStream. jarout) bos (BufferedOutputStream. fos) zos (ZipOutputStream. bos)]
    (loop [ze (.getNextEntry zis)]
      (when ze
        (when-let [n (l (.getName ze))]
          (.putNextEntry zos (ZipEntry. n))
          (copy zis zos)
          (.closeEntry zos))
        (recur (.getNextEntry zis))))))

(defn jar-slimmer
  "build the smallest possible jar given the jar and cmd"
  [j c] (build-jar j (str j ".slim") (smallest-jar-list j c)))


;; --------------------- </side-effects> -------------------------------

;; ----------------------- <in-progress> -------------------------------

(def jar-path "/home/denis/.m2/repository/org/clojure/clojure-contrib/1.2.0/clojure-contrib-1.2.0.jar")

(def zip-is (ZipInputStream. (FileInputStream. jar-path)))


'(let [out (ZipOutputStream. (FileOutputStream. (str jar-path ".slim")))]
   (loop [ze (.getNextEntry zip-is)]
     (when ze
       (.putNextEntry out (ZipEntry. (.getName ze)))
       (copy zip-is out)
       (.closeEntry out)
       (recur (.getNextEntry zip-is))))
   (.close out))

'(with-open [fos (FileOutputStream. (str jar-path ".slim"))
             out (ZipOutputStream. fos)]
   (loop [ze (.getNextEntry zip-is)]
     (when ze
       (.putNextEntry out (ZipEntry. (.getName ze)))
       (copy zip-is out)
       (.closeEntry out)
       (recur (.getNextEntry zip-is)))))

'(with-open [fos (FileOutputStream. (str jar-path ".slim"))
             bos (BufferedOutputStream. fos)
             out (ZipOutputStream. bos)]
   (loop [ze (.getNextEntry zip-is)]
     (when ze
       (.putNextEntry out (ZipEntry. (.getName ze)))
       (copy zip-is out)
       (.closeEntry out)
       (recur (.getNextEntry zip-is)))))

(def zip-is (ZipInputStream. (FileInputStream. jar-path)))

'(with-open [fis (FileInputStream. jar-path)
             bis (BufferedInputStream. fis)
             zis (ZipInputStream. bis)
             fos (FileOutputStream. (str jar-path ".slim"))
             bos (BufferedOutputStream. fos)
             zos (ZipOutputStream. bos)]
   (loop [ze (.getNextEntry zis)]
     (when ze
       (.putNextEntry zos (ZipEntry. (.getName ze)))
       (copy zis zos)
       (.closeEntry zos)
       (recur (.getNextEntry zis)))))

'(with-open
    [fis (FileInputStream. jar-path)                bis (BufferedInputStream. fis)  zis (ZipInputStream. bis)
     fos (FileOutputStream. (str jar-path ".slim")) bos (BufferedOutputStream. fos) zos (ZipOutputStream. bos)]
  (loop [ze (.getNextEntry zis)]
    (when ze
      (.putNextEntry zos (ZipEntry. (.getName ze)))
      (copy zis zos)
      (.closeEntry zos)
      (recur (.getNextEntry zis)))))


(loop [ze (.getNextEntry zip-is)]
  (when ze
    (recur (.getNextEntry zip-is))))

;; ----------------------- </in-progress> ------------------------------
