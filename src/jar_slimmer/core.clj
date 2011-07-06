(ns jar-slimmer.core)

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
  ([all seg f] (cond (empty? seg)      (sorted-set)
                     (nil? (next seg)) (if (true-without? all seg f)
                                         seg
                                         (sorted-set))
                     :otherwise        (if (true-without? all seg f)
                                         seg
                                         (concat (find-unused all (first-half seg) f)
                                                 (find-unused all (second-half seg) f))))))

(defn smallest
  "Return the smallest sub seq of the given seq for which the given function return true"
  [s f] (compl s (find-unused s f)))
