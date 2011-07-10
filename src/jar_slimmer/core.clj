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

(defn compl "A set which is the complement of minus to all"
  [all minus]
  {:pre [(sorted-set? all) (sorted-set? minus)]}
  (apply sorted-set (remove #(minus %) all)))

(defn true-without? "Return true if f applied to the complement of minus to all is true, false otherwise"
  [all minus f] (f (compl all minus)))

(defn half "Return the first or second half of the given seq"
  [s f] (apply sorted-set
               (f (split-at (bit-shift-right (count s) 1)
                            s))))

(defn first-half "Return the 1st half of the given seq"
  [s] (half s first))

(defn second-half "Return the 1st half of the given seq"
  [s] (half s second))

;; WARN: non-TCO optimized recursion, will blow the stack for deep trees !
(defn find-unused "Find all elements of the set s for which (f s unused) is true."
  ([s f] (apply sorted-set (find-unused (apply sorted-set s) (apply sorted-set s) f)))
  ([all seg f] (cond (empty? seg)              (sorted-set)
                     (true-without? all seg f) seg
                     (nil? (next seg))         (sorted-set)
                     :otherwise                (concat (find-unused all (first-half seg) f)
                                                       (find-unused all (second-half seg) f)))))

(defn smallest "Return the smallest sub seq of the given seq for which the given function return true"
  [s f] (compl s (find-unused s f)))

;; ------------------- </pure-functions> -------------------------------

;; --------------------- <side-effects> --------------------------------

;; Not using clojure.java.shell.sh, as we would need to parse then pass the params (?)
(defn run-cmd "Run the given cmd, and return the exit code."
  [s] (.waitFor (.exec (Runtime/getRuntime) s)))

(defn jar-list "Return the list of the files contained in the given jar"
  [j] (apply sorted-set (filenames-in-jar (java.util.jar.JarFile. j))))

(defn build-jar "Given a jar and a list of resource, build the jarout jar with the listed resource"
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

(defn jar-check "Return true if the test pass on the given jar with the given command, false otherwise"
  [j c] (zero? (run-cmd (str c " " j))))

(defn jar-list-check
  "Given an original jar, a list of resource to include and a command, return true if the jar is valid, false if not"
  [j l c] (when (seq l)
            (let [tmpj (str j ".tmp")]
              (build-jar j tmpj l)
              (jar-check tmpj c))))

(defn smallest-jar-list "Return the smallest possible resource list corresponding to the given cmd and jar"
  [j c] (smallest (jar-list j) #(jar-list-check j % c)))

(defn jar-slimmer "build the smallest possible jar given the jar and cmd, returns nil if ok, a str message otherwise"
  [j c] (let [final-j (str j ".slim")]
          (build-jar j final-j (smallest-jar-list j c))
          (if-not (jar-check final-j c)
            "*** FAILURE *** Something wrong happened, the final jar is not valid")))

(defn -main [& args]
  (let [opts
        (clargon
         args
         (required ["-j" "--jar" "jar to minify"])
         (required ["-c" "--cmd" "Cmd that take a uniq arg which is the name of the jar to test"]))]
    (time (println (if-let [r (jar-slimmer (opts :jar) (opts :cmd))]
                     r
                     "*** SUCCESS ***")))))

;; --------------------- </side-effects> -------------------------------
